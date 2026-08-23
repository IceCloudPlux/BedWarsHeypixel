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

package com.andrei1058.bedwars.listeners.custom;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 雪球监听器 - 蠹虫生成
 * 
 * 核心逻辑：
 * - 雪球命中任意目标（方块或实体）后，在落点上方生成1只蠹虫
 * - 蠹虫AI开启，使用原版寻路
 * - 蠹虫不继承投掷者的队伍
 */
public class SilverfishSnowballListener implements Listener {

    // 自定义标签，用于标记生成的蠹虫
    public static final String SILVERFISH_TAG = "bw_silverfish";
    
    // 存储活跃的雪球
    private static final Map<UUID, UUID> activeSnowballs = new HashMap<>();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLaunch(ProjectileLaunchEvent event) {
        // 检查是否在多人大厅世界
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (event.getEntity().getLocation().getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity() instanceof Snowball) {
            Snowball snowball = (Snowball) event.getEntity();
            
            // 使用静态 Set 检查是否为连携雪球（比元数据更可靠）
            if (DualWieldComboListener.comboSnowballUUIDs.contains(snowball.getUniqueId())) {
                return;
            }
            
            if (snowball.getShooter() instanceof Player) {
                Player shooter = (Player) snowball.getShooter();
                IArena arena = Arena.getArenaByPlayer(shooter);
                if (arena != null && arena.isPlayer(shooter)) {
                    // 记录雪球
                    activeSnowballs.put(snowball.getUniqueId(), shooter.getUniqueId());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball snowball = (Snowball) event.getEntity();
        
        // 使用静态 Set 检查是否为连携雪球（比元数据更可靠）
        if (DualWieldComboListener.comboSnowballUUIDs.contains(snowball.getUniqueId())) {
            return;
        }
        
        if (!activeSnowballs.containsKey(snowball.getUniqueId())) return;

        Player shooter = (Player) snowball.getShooter();
        if (shooter == null) {
            activeSnowballs.remove(snowball.getUniqueId());
            return;
        }

        IArena arena = Arena.getArenaByPlayer(shooter);
        if (arena == null) {
            activeSnowballs.remove(snowball.getUniqueId());
            return;
        }

        // 获取命中位置，并在方块上方生成蠹虫
        Location hitLocation = snowball.getLocation().clone();
        
        // 确保在方块上方生成，高度至少为方块上方1格
        hitLocation.setY(hitLocation.getBlockY() + 1);
        
        // 生成蠹虫
        spawnSilverfish(hitLocation, arena);

        // 清理记录
        activeSnowballs.remove(snowball.getUniqueId());
    }

    /**
     * 在指定位置生成蠹虫
     */
    private void spawnSilverfish(Location location, IArena arena) {
        if (location.getWorld() == null) return;
        
        // 生成蠹虫
        Silverfish silverfish = (Silverfish) location.getWorld().spawnEntity(location, EntityType.SILVERFISH);
        
        // 注意：setAI() 和 addScoreboardTag() 在 1.8.8 中不可用
        // 蠹虫默认会攻击玩家，BedWars1058队伍系统会自动处理攻击关系
        // 无需额外设置
    }
}