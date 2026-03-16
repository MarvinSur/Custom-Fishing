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

package net.momirealms.customfishing.api.mechanic.bag;

import net.momirealms.customfishing.common.plugin.feature.Reloadable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The BagManager interface defines methods for managing fishing bags.
 */
public interface BagManager extends Reloadable {

    /**
     * Retrieves the number of inventory rows for a player's fishing bag based on their permissions.
     */
    static int getBagInventoryRows(Player player) {
        int size = 1;
        for (int i = 6; i > 1; i--) {
            if (player.hasPermission("fishingbag.rows." + i)) {
                size = i;
                break;
            }
        }
        return size;
    }

    /**
     * Opens the fishing bag of a specified owner for a viewer player asynchronously.
     */
    CompletableFuture<Boolean> openBag(@NotNull Player viewer, @NotNull UUID owner);
    
    /**
     * Opens the fishing bag at a specific page
     */
    CompletableFuture<Boolean> openBagAtPage(@NotNull Player viewer, @NotNull UUID owner, int page);
    
    /**
     * Checks if player has permission to access a specific page
     */
    boolean hasPagePermission(Player player, int page);
    
    /**
     * Gets the maximum page number a player can access
     */
    int getMaxAccessiblePage(Player player);
    
    /**
     * Gets total item count across all bag pages
     */
    int getTotalItemCount(Player player);
    
    /**
     * Clears all items from all bag pages
     */
    void clearAllPages(Player player);
}
