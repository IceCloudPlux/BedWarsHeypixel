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

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class CmdTime extends SubCommand {

    public CmdTime(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(16);
        showInList(true);
        setDisplayInfo(MainCommand.createTC("§6 ▪ §7/" + MainCommand.getInstance().getName() + " " + getSubCommandName() + " add <time> §8 - §eadd time to the current game",
                "/" + getParent().getName() + " " + getSubCommandName(), "§fAdd time to current game.\n§fExample: /bw time add 59m\n§fPermission: §c" + Permissions.PERMISSION_ALL));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) {
            s.sendMessage("§cThis command is only for players!");
            return true;
        }

        Player p = (Player) s;

        if (!s.hasPermission(Permissions.PERMISSION_ALL)) {
            p.sendMessage(getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
            return true;
        }

        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /" + MainCommand.getInstance().getName() + " time add <time>");
            p.sendMessage(ChatColor.GRAY + "Example: /bw time add 59m (adds 59 minutes)");
            p.sendMessage(ChatColor.GRAY + "Example: /bw time add 30s (adds 30 seconds)");
            return true;
        }

        String subCommand = args[0];
        if (!subCommand.equalsIgnoreCase("add")) {
            p.sendMessage(ChatColor.RED + "Usage: /" + MainCommand.getInstance().getName() + " time add <time>");
            return true;
        }

        IArena arena = Arena.getArenaByPlayer(p);
        if (arena == null) {
            p.sendMessage(ChatColor.RED + "You must be in a game to use this command!");
            return true;
        }

        if (arena.getStatus() != GameState.playing) {
            p.sendMessage(ChatColor.RED + "The game is not currently running!");
            return true;
        }

        String timeStr = args[1];
        int seconds = parseTime(timeStr);

        if (seconds <= 0) {
            p.sendMessage(ChatColor.RED + "Invalid time format! Use format like: 59m, 30s, 1h");
            return true;
        }

        if (arena instanceof Arena) {
            if (((Arena) arena).addGameTime(seconds)) {
                p.sendMessage(ChatColor.GREEN + "Added " + formatTime(seconds) + " to the game!");
            } else {
                p.sendMessage(ChatColor.RED + "Failed to add time to the game!");
            }
        }

        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("add");
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;

        IArena a = Arena.getArenaByPlayer(p);
        if (a != null) {
            if (a.getStatus() != GameState.playing) {
                return false;
            }
        } else {
            return false;
        }

        return s.hasPermission(Permissions.PERMISSION_ALL);
    }

    /**
     * Parse time string like "59m", "30s", "1h" to seconds
     */
    private int parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return 0;
        }

        timeStr = timeStr.toLowerCase().trim();

        try {
            if (timeStr.endsWith("s")) {
                return Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));
            } else if (timeStr.endsWith("m")) {
                return Integer.parseInt(timeStr.substring(0, timeStr.length() - 1)) * 60;
            } else if (timeStr.endsWith("h")) {
                return Integer.parseInt(timeStr.substring(0, timeStr.length() - 1)) * 3600;
            } else {
                // If no suffix, treat as seconds
                return Integer.parseInt(timeStr);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Format seconds to human-readable string
     */
    private String formatTime(int seconds) {
        if (seconds >= 3600) {
            int hours = seconds / 3600;
            int mins = (seconds % 3600) / 60;
            if (mins > 0) {
                return hours + "h " + mins + "m";
            }
            return hours + "h";
        } else if (seconds >= 60) {
            int mins = seconds / 60;
            int secs = seconds % 60;
            if (secs > 0) {
                return mins + "m " + secs + "s";
            }
            return mins + "m";
        } else {
            return seconds + "s";
        }
    }
}