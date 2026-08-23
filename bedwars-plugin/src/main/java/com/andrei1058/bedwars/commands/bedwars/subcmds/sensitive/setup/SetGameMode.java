/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameType;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * 设置游戏模式的命令
 * 用法: /bw type <normal|xp>
 */
public class SetGameMode extends SubCommand {

    private static final List<String> availableModes = Arrays.asList("normal", "xp");

    public SetGameMode(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) {
            s.sendMessage("§c ▪ §7You're not in a setup session!");
            return true;
        }
        if (args.length == 0) {
            sendUsage(p);
        } else {
            String mode = args[0].toLowerCase();
            if (!availableModes.contains(mode)) {
                sendUsage(p);
                return true;
            }
            
            GameType gameType = GameType.fromIdentifier(mode);
            ss.getConfig().set("gameType", mode);
            
            String modeDisplay = mode.equalsIgnoreCase("normal") ? "§e普通模式 (Normal)" : "§b经验模式 (XP)";
            p.sendMessage("§6 ▪ §7游戏模式已设置为: " + modeDisplay);
            
            if (ss.getSetupType() == SetupType.ASSISTED) {
                Bukkit.dispatchCommand(p, getParent().getName());
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return availableModes;
    }

    private void sendUsage(Player p) {
        p.sendMessage("§9 ▪ §7用法: " + getParent().getName() + " " + getSubCommandName() + " <type>");
        p.sendMessage("§9可用的游戏模式: ");
        p.spigot().sendMessage(Misc.msgHoverClick("§1 ▪ §eNormal §7(普通模式) - 使用物品作为货币", "§d点击设置为普通模式", "/" + getParent().getName() + " " + getSubCommandName() + " normal", ClickEvent.Action.RUN_COMMAND));
        p.spigot().sendMessage(Misc.msgHoverClick("§1 ▪ §bXP §7(经验模式) - 使用经验值作为货币", "§d点击设置为经验模式", "/" + getParent().getName() + " " + getSubCommandName() + " xp", ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}