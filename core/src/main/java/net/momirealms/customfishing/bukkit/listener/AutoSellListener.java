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

package net.momirealms.customfishing.bukkit.listener;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.context.Context;
import net.momirealms.customfishing.api.storage.data.InventoryData;
import net.momirealms.customfishing.api.storage.data.PlayerData;
import net.momirealms.customfishing.api.storage.user.UserData;
import net.momirealms.customfishing.bukkit.bag.BukkitBagManager;
import net.momirealms.customfishing.bukkit.market.BukkitMarketManager;
import net.momirealms.customfishing.common.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AutoSellListener implements Listener {

    private final BukkitCustomFishingPlugin plugin;
    private final BukkitBagManager bagManager;
    private final BukkitMarketManager marketManager;
    private static final int AUTO_SELL_THRESHOLD = 15000;

    public AutoSellListener(BukkitCustomFishingPlugin plugin) {
        this.plugin = plugin;
        this.bagManager = (BukkitBagManager) plugin.getBagManager();
        this.marketManager = (BukkitMarketManager) plugin.getMarketManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!player.hasPermission("fishingbag.autosell")) return;

        checkAndAutoSell(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("fishingbag.autosell")) return;

        checkAndAutoSell(player);
    }

    private void checkAndAutoSell(Player player) {
        int totalItems = bagManager.getTotalItemCount(player);

        if (totalItems >= AUTO_SELL_THRESHOLD) {
            sellAllItems(player);
        }
    }

    private void sellAllItems(Player player) {
        Pair<Integer, Double> sellResult = calculateSellValue(player);
        double totalValue = sellResult.right();

        if (totalValue > 0) {
            addMoney(player, totalValue);
            clearAllBagPages(player);

            player.sendMessage("§a§lAUTO SELL! §7Sold §e" + sellResult.left() + " §7items for §a$" + String.format("%,.2f", totalValue));
            player.sendMessage("§7Your fishing bag has been cleared.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private void clearAllBagPages(Player player) {
        Optional<UserData> userDataOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());
        if (userDataOpt.isEmpty()) return;
    
        UserData oldData = userDataOpt.get();
        PlayerData playerData = oldData.toPlayerData();

        List<InventoryData> pages = playerData.getBagPages();
        for (int i = 0; i < pages.size(); i++) {
            pages.set(i, InventoryData.empty());
        }

        // Buat UserData baru dengan builder
        UserData newData = UserData.builder()
                .data(playerData)
                .build();

        plugin.getStorageManager().saveUserData(newData, true);
    }

    private void addMoney(Player player, double amount) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            "eco give " + player.getName() + " " + (int) amount
        );
    }
}
