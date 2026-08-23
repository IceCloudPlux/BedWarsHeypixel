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

package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.GameType;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.XpModeConfig;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import com.andrei1058.bedwars.BedWars;

/**
 * 经验模式资源拾取监听器
 * 
 * 在经验模式下，玩家拾取资源时自动转换为经验值
 */
public class XpModePickupListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        
        // 检查是否在经验模式下的游戏中
        if (arena == null || arena.getGameType() != GameType.XP) {
            return;
        }
        
        ItemStack item = event.getItem().getItemStack();
        if (item == null) return;
        
        XpModeConfig xpConfig = XpModeConfig.getInstance();
        if (xpConfig == null) return;
        
        int xpAmount = 0;
        Material type = item.getType();
        
        // 根据资源类型计算经验值
        if (type == Material.IRON_INGOT) {
            xpAmount = xpConfig.getIronXp() * item.getAmount();
        } else if (type == Material.GOLD_INGOT) {
            xpAmount = xpConfig.getGoldXp() * item.getAmount();
        } else if (type == Material.EMERALD) {
            xpAmount = xpConfig.getEmeraldXp() * item.getAmount();
        } else if (type == Material.DIAMOND) {
            xpAmount = xpConfig.getDiamondXp() * item.getAmount();
        }
        
        if (xpAmount > 0) {
            // 取消拾取事件
            event.setCancelled(true);
            
            // 移除物品
            event.getItem().remove();
            
            // 给玩家经验值
            player.giveExpLevels(xpAmount);
            
            // 播放声音
            player.playSound(player.getLocation(), Sound.valueOf(BedWars.getForCurrentVersion("ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP")), 0.5f, 1.0f);
        }
    }
}