/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.bukkit.bag;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.event.FishingBagPreCollectEvent;
import net.momirealms.customfishing.api.event.FishingLootSpawnEvent;
import net.momirealms.customfishing.api.mechanic.MechanicType;
import net.momirealms.customfishing.api.mechanic.action.Action;
import net.momirealms.customfishing.api.mechanic.action.ActionManager;
import net.momirealms.customfishing.api.mechanic.bag.BagManager;
import net.momirealms.customfishing.api.mechanic.bag.FishingBagHolder;
import net.momirealms.customfishing.api.mechanic.context.Context;
import net.momirealms.customfishing.api.mechanic.requirement.Requirement;
import net.momirealms.customfishing.api.mechanic.requirement.RequirementManager;
import net.momirealms.customfishing.api.storage.data.InventoryData;
import net.momirealms.customfishing.api.storage.data.PlayerData;
import net.momirealms.customfishing.api.storage.user.UserData;
import net.momirealms.customfishing.api.util.EventUtils;
import net.momirealms.customfishing.api.util.PlayerUtils;
import net.momirealms.customfishing.bukkit.config.BukkitConfigManager;
import net.momirealms.customfishing.common.helper.AdventureHelper;
import net.momirealms.sparrow.heart.SparrowHeart;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitBagManager implements BagManager, Listener {

    private final BukkitCustomFishingPlugin plugin;
    private final HashMap<UUID, UserData> tempEditMap;
    private final ConcurrentHashMap<UUID, Inventory[]> playerPageInventories;
    private Action<Player>[] collectLootActions;
    private Action<Player>[] bagFullActions;
    private boolean bagStoreLoots;
    private boolean bagStoreRods;
    private boolean bagStoreBaits;
    private boolean bagStoreHooks;
    private boolean bagStoreUtils;
    private final HashSet<MechanicType> storedTypes = new HashSet<>();
    private boolean enable;
    private String bagTitle;
    private List<Material> bagWhiteListItems = new ArrayList<>();
    private Requirement<Player>[] collectRequirements;
    
    private static final int MAX_PAGES = 20;
    private static final int PREV_BUTTON_SLOT = 45;
    private static final int NEXT_BUTTON_SLOT = 53;
    private Sound pageSwitchSound;
    private float soundVolume;
    private float soundPitch;

    public BukkitBagManager(BukkitCustomFishingPlugin plugin) {
        this.plugin = plugin;
        this.tempEditMap = new HashMap<>();
        this.playerPageInventories = new ConcurrentHashMap<>();
        this.pageSwitchSound = Sound.UI_BUTTON_CLICK;
        this.soundVolume = 1.0f;
        this.soundPitch = 1.0f;
    }

    @Override
    public void load() {
        this.loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin.getBootstrap());
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this);
        storedTypes.clear();
        playerPageInventories.clear();
    }

    @Override
    public void disable() {
        unload();
        if (!tempEditMap.isEmpty()) {
            this.plugin.getStorageManager().getDataSource().updateManyPlayersData(tempEditMap.values(), true);
        }
    }

    @EventHandler
    public void onLootSpawn(FishingLootSpawnEvent event) {
        if (!enable || !bagStoreLoots) {
            return;
        }
        if (!event.summonEntity()) {
            return;
        }
        if (!(event.getEntity() instanceof Item itemEntity)) {
            return;
        }

        Player player = event.getPlayer();
        Context<Player> context = event.getContext();
        if (!RequirementManager.isSatisfied(context, collectRequirements)) {
            return;
        }

        Optional<UserData> onlineUser = plugin.getStorageManager().getOnlineUser(player.getUniqueId());
        if (onlineUser.isEmpty()) {
            return;
        }
        UserData userData = onlineUser.get();
        
        // Get first page inventory for collection
        Inventory inventory = getOrCreatePage(userData, 1);
        
        ItemStack item = itemEntity.getItemStack();
        FishingBagPreCollectEvent preCollectEvent = new FishingBagPreCollectEvent(player, item, inventory);
        if (EventUtils.fireAndCheckCancel(preCollectEvent)) {
            return;
        }

        int cannotPut = PlayerUtils.putItemsToInventory(inventory, item, item.getAmount());
        
        // Save the page after changes
        if (cannotPut != item.getAmount()) {
            savePage(userData, 1, inventory);
            ActionManager.trigger(context, collectLootActions);
        }
        
        if (cannotPut == 0) {
            event.summonEntity(false);
            return;
        }
        item.setAmount(cannotPut);
        itemEntity.setItemStack(item);
        ActionManager.trigger(context, bagFullActions);
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Section config = BukkitConfigManager.getMainConfig().getSection("mechanics.fishing-bag");

        enable = config.getBoolean("enable", true);
        bagTitle = config.getString("bag-title", "<gold>Fishing Bag <gray>| Page {page}/{maxpage}");
        bagStoreLoots = config.getBoolean("can-store-loot", false);
        bagStoreRods = config.getBoolean("can-store-rod", true);
        bagStoreBaits = config.getBoolean("can-store-bait", true);
        bagStoreHooks = config.getBoolean("can-store-hook", true);
        bagStoreUtils = config.getBoolean("can-store-util", true);
        bagWhiteListItems = config.getStringList("whitelist-items").stream()
                .map(it -> Material.valueOf(it.toUpperCase(Locale.ENGLISH))).toList();
        collectLootActions = plugin.getActionManager().parseActions(config.getSection("collect-actions"));
        bagFullActions = plugin.getActionManager().parseActions(config.getSection("full-actions"));
        collectRequirements = plugin.getRequirementManager().parseRequirements(config.getSection("collect-requirements"), false);
        
        // Load sound settings
        String soundName = config.getString("page-switch-sound", "UI_BUTTON_CLICK");
        try {
            pageSwitchSound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            pageSwitchSound = Sound.UI_BUTTON_CLICK;
        }
        soundVolume = config.getDouble("sound-volume", 1.0).floatValue();
        soundPitch = config.getDouble("sound-pitch", 1.0).floatValue();

        if (bagStoreLoots) storedTypes.add(MechanicType.LOOT);
        if (bagStoreRods) storedTypes.add(MechanicType.ROD);
        if (bagStoreBaits) storedTypes.add(MechanicType.BAIT);
        if (bagStoreHooks) storedTypes.add(MechanicType.HOOK);
        if (bagStoreUtils) storedTypes.add(MechanicType.UTIL);
    }

    @Override
    public CompletableFuture<Boolean> openBag(@NotNull Player viewer, @NotNull UUID owner) {
        return openBagAtPage(viewer, owner, 1);
    }

    @Override
    public CompletableFuture<Boolean> openBagAtPage(@NotNull Player viewer, @NotNull UUID owner, int page) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (!enable) {
            future.complete(false);
            return future;
        }
        
        if (page < 1 || page > MAX_PAGES) {
            future.complete(false);
            return future;
        }
        
        if (!hasPagePermission(viewer, page)) {
            viewer.sendMessage("§cYou don't have permission to access page " + page + "!");
            future.complete(false);
            return future;
        }
        
        Optional<UserData> onlineUser = plugin.getStorageManager().getOnlineUser(owner);
        onlineUser.ifPresentOrElse(data -> {
            Inventory targetPage = getOrCreatePage(data, page);
            addNavigationButtons(targetPage, page, getMaxAccessiblePage(viewer));
            viewer.openInventory(targetPage);
            updateInventoryTitle(viewer, owner, page, data.name());
            future.complete(true);
            
        }, () -> plugin.getStorageManager().getOfflineUserData(owner, true).thenAccept(result -> result.ifPresentOrElse(data -> {
            if (data.isLocked()) {
                future.completeExceptionally(new RuntimeException("Data is locked"));
                return;
            }
            
            this.tempEditMap.put(viewer.getUniqueId(), data);
            
            Inventory targetPage = getOrCreatePage(data, page);
            addNavigationButtons(targetPage, page, getMaxAccessiblePage(viewer));
            
            viewer.openInventory(targetPage);
            updateInventoryTitle(viewer, owner, page, data.name());
            future.complete(true);
        }, () -> future.complete(false))));
        
        return future;
    }

    @Override
    public boolean hasPagePermission(Player player, int page) {
        if (player.hasPermission("fishingbag.page.*")) {
            return true;
        }
        return player.hasPermission("fishingbag.page." + page);
    }

    @Override
    public int getMaxAccessiblePage(Player player) {
        if (player.hasPermission("fishingbag.page.*")) {
            return MAX_PAGES;
        }
        
        for (int i = MAX_PAGES; i >= 1; i--) {
            if (player.hasPermission("fishingbag.page." + i)) {
                return i;
            }
        }
        return 1;
    }

    @Override
    public int getTotalItemCount(Player player) {
        Optional<UserData> userData = plugin.getStorageManager().getOnlineUser(player.getUniqueId());
        if (userData.isEmpty()) return 0;
        
        PlayerData playerData = userData.get().toPlayerData();
        List<InventoryData> pages = playerData.getBagPages();
        
        int total = 0;
        // Note: Karena InventoryData pake serialized string, kita gak bisa hitung item secara akurat
        // Ini akan diimplementasikan nanti jika perlu
        return total;
    }

    @Override
    public void clearAllPages(Player player) {
        Optional<UserData> userData = plugin.getStorageManager().getOnlineUser(player.getUniqueId());
        if (userData.isEmpty()) return;

        UserData data = userData.get();
        PlayerData playerData = data.toPlayerData();
        
        List<InventoryData> pages = playerData.getBagPages();
        for (int i = 0; i < pages.size(); i++) {
            pages.set(i, InventoryData.empty());
        }
        
        playerPageInventories.remove(player.getUniqueId());
        
        data.data(playerData);
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof FishingBagHolder holder))
            return;
            
        final Player viewer = (Player) event.getPlayer();
        UserData userData = tempEditMap.remove(viewer.getUniqueId());
        
        if (userData != null) {
            PlayerData playerData = userData.toPlayerData();
            // Simpan sebagai empty dulu, implementasi serialization nanti
            playerData.setBagPage(holder.getPage() - 1, InventoryData.empty());
            userData.data(playerData);
            
            this.plugin.getStorageManager().saveUserData(userData, true);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FishingBagHolder holder))
            return;
            
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (slot == PREV_BUTTON_SLOT || slot == NEXT_BUTTON_SLOT) {
            event.setCancelled(true);
            
            int currentPage = holder.getPage();
            int maxPage = getMaxAccessiblePage(player);
            int newPage = slot == PREV_BUTTON_SLOT ? currentPage - 1 : currentPage + 1;
            
            if (newPage >= 1 && newPage <= maxPage && hasPagePermission(player, newPage)) {
                player.playSound(player.getLocation(), pageSwitchSound, soundVolume, soundPitch);
                openBagAtPage(player, holder.getOwner(), newPage);
            }
            return;
        }
        
        ItemStack movedItem = event.getCurrentItem();
        Inventory clicked = event.getClickedInventory();
        if (clicked != event.getWhoClicked().getInventory()) {
            if (event.getAction() != InventoryAction.HOTBAR_SWAP && event.getAction() != InventoryAction.HOTBAR_MOVE_AND_READD) {
                return;
            }
            movedItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
        }
        if (movedItem == null || movedItem.getType() == Material.AIR || bagWhiteListItems.contains(movedItem.getType()))
            return;
        String id = plugin.getItemManager().getItemID(movedItem);
        List<MechanicType> type = MechanicType.getTypeByID(id);
        if (type == null) {
            event.setCancelled(true);
            return;
        }
        for (MechanicType mechanicType : type) {
            if (storedTypes.contains(mechanicType)) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UserData userData = tempEditMap.remove(event.getPlayer().getUniqueId());
        if (userData == null)
            return;
        plugin.getStorageManager().saveUserData(userData, true);
    }

    private Inventory getOrCreatePage(UserData userData, int page) {
        PlayerData playerData = userData.toPlayerData();
        List<InventoryData> pages = playerData.getBagPages();
        
        while (pages.size() < page) {
            pages.add(InventoryData.empty());
        }
        
        Player owner = Bukkit.getPlayer(userData.uuid());
        int rows = owner != null ? BagManager.getBagInventoryRows(owner) : 6;
        
        FishingBagHolder holder = FishingBagHolder.create(
            userData.uuid(), 
            new ItemStack[rows * 9], // Empty inventory
            rows * 9,
            page
        );
        
        Inventory[] cachedPages = playerPageInventories.computeIfAbsent(userData.uuid(), k -> new Inventory[MAX_PAGES]);
        cachedPages[page - 1] = holder.getInventory();
        
        return holder.getInventory();
    }

    private void savePage(UserData userData, int page, Inventory inventory) {
        PlayerData playerData = userData.toPlayerData();
        // Implement serialization later
        playerData.setBagPage(page - 1, InventoryData.empty());
        userData.data(playerData);
        
        Inventory[] cachedPages = playerPageInventories.get(userData.uuid());
        if (cachedPages != null) {
            cachedPages[page - 1] = inventory;
        }
    }

    private void addNavigationButtons(Inventory inv, int currentPage, int maxPage) {
        ItemStack prevButton = createNavButton(
            currentPage > 1, 
            "§aPrevious Page", 
            currentPage > 1 ? "§7Click to go to page " + (currentPage - 1) : "§7You are on the first page"
        );
        
        ItemStack nextButton = createNavButton(
            currentPage < maxPage, 
            "§aNext Page", 
            currentPage < maxPage ? "§7Click to go to page " + (currentPage + 1) : "§7Last page reached"
        );
        
        inv.setItem(PREV_BUTTON_SLOT, prevButton);
        inv.setItem(NEXT_BUTTON_SLOT, nextButton);
    }

    private ItemStack createNavButton(boolean enabled, String name, String lore) {
        ItemStack button = new ItemStack(enabled ? Material.ARROW : Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        button.setItemMeta(meta);
        return button;
    }

    private void updateInventoryTitle(Player viewer, UUID owner, int page, String ownerName) {
        String title = bagTitle.replace("{player}", ownerName)
                               .replace("{page}", String.valueOf(page))
                               .replace("{maxpage}", String.valueOf(getMaxAccessiblePage(viewer)));
        SparrowHeart.getInstance().updateInventoryTitle(viewer, 
            AdventureHelper.componentToJson(AdventureHelper.miniMessage(
                plugin.getPlaceholderManager().parse(Bukkit.getOfflinePlayer(owner), title, Map.of("{uuid}", owner.toString()))
            ))
        );
    }
}
