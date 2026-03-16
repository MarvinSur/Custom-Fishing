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

package net.momirealms.customfishing.bukkit.command.feature;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.bukkit.bag.BukkitBagManager;
import net.momirealms.customfishing.bukkit.command.BukkitCommandFeature;
import net.momirealms.customfishing.common.command.CustomFishingCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.parser.standard.IntegerParser;

public class EditOnlineBagCommand extends BukkitCommandFeature<CommandSender> {

    public EditOnlineBagCommand(CustomFishingCommandManager<CommandSender> commandManager) {
        super(commandManager);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .required("player", PlayerParser.playerParser())
                .optional("page", IntegerParser.integerParser(1, 20))
                .handler(context -> {
                    Player admin = context.sender();
                    Player online = context.get("player");
                    int page = context.getOrDefault("page", 1);
                    
                    BukkitCustomFishingPlugin plugin = BukkitCustomFishingPlugin.getInstance();
                    BukkitBagManager bagManager = (BukkitBagManager) plugin.getBagManager();
                    
                    bagManager.openBagAtPage(admin, online.getUniqueId(), page);
                });
    }

    @Override
    public String getFeatureID() {
        return "edit_online_bag";
    }
}
