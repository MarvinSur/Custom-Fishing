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

package net.momirealms.customfishing.api.storage.data;

import com.google.gson.annotations.SerializedName;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * The PlayerData class holds data related to a player.
 */
public class PlayerData {

    public static final String DEFAULT_NAME = "";
    public static final StatisticData DEFAULT_STATISTICS = StatisticData.empty();
    public static final InventoryData DEFAULT_BAG = InventoryData.empty();
    public static final EarningData DEFAULT_EARNING = EarningData.empty();
    public static final int MAX_BAG_PAGES = 20;

    @SerializedName("name")
    protected String name;
    @SerializedName("stats")
    protected StatisticData statisticsData;
    @SerializedName("bag")
    protected InventoryData bagData;
    @SerializedName("trade")
    protected EarningData earningData;
    @SerializedName("bag_pages")
    protected List<InventoryData> bagPages;
    
    transient private UUID uuid;
    transient private boolean locked;
    transient private byte[] jsonBytes;

    public PlayerData(UUID uuid, String name, StatisticData statisticsData, InventoryData bagData, 
                      EarningData earningData, boolean isLocked, List<InventoryData> bagPages) {
        this.name = name;
        this.statisticsData = statisticsData;
        this.bagData = bagData;
        this.earningData = earningData;
        this.locked = isLocked;
        this.uuid = uuid;
        this.bagPages = bagPages != null ? bagPages : new ArrayList<>();
        
        // Initialize empty pages up to MAX_BAG_PAGES
        while (this.bagPages.size() < MAX_BAG_PAGES) {
            this.bagPages.add(InventoryData.empty());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PlayerData empty() {
        return new Builder()
                .bag(InventoryData.empty())
                .earnings(EarningData.empty())
                .statistics(StatisticData.empty())
                .uuid(new UUID(0, 0))
                .locked(false)
                .bagPages(new ArrayList<>())
                .build();
    }

    public static class Builder {

        private String name = DEFAULT_NAME;
        private StatisticData statisticsData = DEFAULT_STATISTICS;
        private InventoryData bagData = DEFAULT_BAG;
        private EarningData earningData = DEFAULT_EARNING;
        private boolean isLocked = false;
        private UUID uuid;
        private List<InventoryData> bagPages = new ArrayList<>();

        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        @NotNull
        public Builder uuid(@NotNull UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        @NotNull
        public Builder locked(boolean locked) {
            this.isLocked = locked;
            return this;
        }

        @NotNull
        public Builder statistics(@Nullable StatisticData statisticsData) {
            this.statisticsData = statisticsData;
            return this;
        }

        @NotNull
        public Builder bag(@Nullable InventoryData inventoryData) {
            this.bagData = inventoryData;
            return this;
        }

        @NotNull
        public Builder earnings(@Nullable EarningData earningData) {
            this.earningData = earningData;
            return this;
        }

        @NotNull
        public Builder bagPages(@Nullable List<InventoryData> bagPages) {
            this.bagPages = bagPages != null ? bagPages : new ArrayList<>();
            return this;
        }

        @NotNull
        public PlayerData build() {
            return new PlayerData(requireNonNull(uuid), name, statisticsData, bagData, earningData, isLocked, bagPages);
        }
    }

    public byte[] toBytes() {
        if (jsonBytes == null) {
            jsonBytes = BukkitCustomFishingPlugin.getInstance().getStorageManager().toBytes(this);
        }
        return jsonBytes;
    }

    public StatisticData statistics() {
        return statisticsData;
    }

    public InventoryData bagData() {
        return bagData;
    }

    public EarningData earningData() {
        return earningData;
    }

    public String name() {
        return name;
    }

    public boolean locked() {
        return locked;
    }

    public void locked(boolean locked) {
        this.locked = locked;
    }

    public UUID uuid() {
        return uuid;
    }

    public void uuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public List<InventoryData> getBagPages() {
        // Initialize if null
        if (bagPages == null) {
            bagPages = new ArrayList<>();
        }
        
        // Ensure we always have MAX_BAG_PAGES
        while (bagPages.size() < MAX_BAG_PAGES) {
            bagPages.add(InventoryData.empty());
        }
        return bagPages;
    }

    public void setBagPages(List<InventoryData> bagPages) {
        this.bagPages = bagPages;
    }

    public InventoryData getBagPage(int page) {
        List<InventoryData> pages = getBagPages();
        if (page < 0 || page >= pages.size()) {
            return InventoryData.empty();
        }
        InventoryData data = pages.get(page);
        return data != null ? data : InventoryData.empty();
    }

    public void setBagPage(int page, InventoryData data) {
        List<InventoryData> pages = getBagPages();
        if (page >= 0 && page < pages.size()) {
            pages.set(page, data);
        }
    }
}
