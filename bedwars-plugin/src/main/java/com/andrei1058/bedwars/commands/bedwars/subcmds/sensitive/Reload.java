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

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Reload extends SubCommand {

    public Reload(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(11);
        showInList(true);
        setPermission(Permissions.PERMISSION_RELOAD);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " "+getSubCommandName()+"       §8 - §ereload messages",
                "§fReload messages.\n§cNot recommended!", "/"+ getParent().getName() + " "+getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof Player) {
            if (!MainCommand.isLobbySet((Player) s)) return true;
        } else {
            if (!MainCommand.isLobbySet(null)) return true;
        }
        
        // 重新加载语言文件
        for (Language l : Language.getLanguages()){
            l.reload();
            s.sendMessage("§6 ▪ §7"+l.getLangName()+" reloaded!");
        }
        
        // 重新加载主配置文件
        try {
            com.andrei1058.bedwars.BedWars.config.reload();
            s.sendMessage("§6 ▪ §7Main configuration (config.yml) reloaded!");
        } catch (Exception e) {
            s.sendMessage("§c ▪ §7Failed to reload config.yml: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 重新加载生成器配置
        try {
            com.andrei1058.bedwars.BedWars.generators.reload();
            s.sendMessage("§6 ▪ §7Generators configuration (generators.yml) reloaded!");
        } catch (Exception e) {
            s.sendMessage("§c ▪ §7Failed to reload generators.yml: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 重新加载牌子配置
        if (com.andrei1058.bedwars.BedWars.signs != null) {
            try {
                com.andrei1058.bedwars.BedWars.signs.reload();
                s.sendMessage("§6 ▪ §7Signs configuration (signs.yml) reloaded!");
            } catch (Exception e) {
                s.sendMessage("§c ▪ §7Failed to reload signs.yml: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 重新加载商店配置
        try {
            com.andrei1058.bedwars.BedWars.shop = new com.andrei1058.bedwars.shop.ShopManager();
            s.sendMessage("§6 ▪ §7Shop configuration reloaded!");
        } catch (Exception e) {
            s.sendMessage("§c ▪ §7Failed to reload shop configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 重新加载升级配置
        try {
            new com.andrei1058.bedwars.configuration.UpgradesConfig(
                "upgrades2", 
                com.andrei1058.bedwars.BedWars.plugin.getDataFolder().getPath()
            );
            s.sendMessage("§6 ▪ §7Upgrades configuration (upgrades2.yml) reloaded!");
        } catch (Exception e) {
            s.sendMessage("§c ▪ §7Failed to reload upgrades2.yml: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            new com.andrei1058.bedwars.configuration.UpgradesConfig(
                "upgrades3", 
                com.andrei1058.bedwars.BedWars.plugin.getDataFolder().getPath()
            );
            s.sendMessage("§6 ▪ §7Upgrades configuration (upgrades3.yml) reloaded!");
        } catch (Exception e) {
            s.sendMessage("§c ▪ §7Failed to reload upgrades3.yml: " + e.getMessage());
            e.printStackTrace();
        }
        
        s.sendMessage("§a▪ §7All configurations have been reloaded!");
        
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {
        if (s instanceof Player) {
            Player p = (Player) s;
            if (Arena.isInArena(p)) return false;
            if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        }
        return hasPermission(s);
    }
}
