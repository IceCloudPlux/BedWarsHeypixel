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

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.gameplay.EggBridgeThrowEvent;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.configuration.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.BedWars.plugin;

@SuppressWarnings("WeakerAccess")
public class EggBridge implements Listener {

    // 存储活跃的搭桥蛋任务
    private static final Map<Egg, BridgeTask> bridges = new HashMap<>();

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (event.getEntity().getLocation().getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getEntity() instanceof Egg) {
            Egg projectile = (Egg) event.getEntity();
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                IArena arena = Arena.getArenaByPlayer(shooter);
                if (arena != null && arena.isPlayer(shooter)) {
                    EggBridgeThrowEvent throwEvent = new EggBridgeThrowEvent(shooter, arena);
                    Bukkit.getPluginManager().callEvent(throwEvent);
                    if (throwEvent.isCancelled()) {
                        event.setCancelled(true);
                        return;
                    }
                    BridgeTask task = new BridgeTask(shooter, projectile, arena);
                    bridges.put(projectile, task);
                    // 任务已在 BridgeTask 构造函数中自动启动
                }
            }
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof Egg) {
            removeEgg((Egg) e.getEntity());
        }
    }

    /**
     * 移除一个鸡蛋并取消其搭桥任务
     */
    public static void removeEgg(Egg e) {
        BridgeTask task = bridges.remove(e);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 获取当前所有活跃的搭桥任务（只读）
     */
    public static Map<Egg, BridgeTask> getBridges() {
        return Collections.unmodifiableMap(bridges);
    }

    /**
     * 内部搭桥任务 —— 发射时固定方向，预计算所有方块位置，然后分批放置
     */
    private static class BridgeTask {
        private final Player player;
        private final Egg egg;
        private final IArena arena;
        private final BukkitTask task;
        private final List<Location> blocksToPlace = new ArrayList<>();
        private int currentIndex = 0;

        // 长度常量：直线23格，斜向21格
        private static final int STRAIGHT_LENGTH = 23;
        private static final int DIAGONAL_LENGTH = 21;

        public BridgeTask(Player player, Egg egg, IArena arena) {
            this.player = player;
            this.egg = egg;
            this.arena = arena;

            // 1. 固定水平方向（基于 yaw，忽略俯仰）
            float yaw = player.getLocation().getYaw();
            double rad = Math.toRadians(yaw);
            Vector direction = new Vector(-Math.sin(rad), 0, Math.cos(rad)).normalize();

            // 2. 判断是否斜向
            boolean isDiagonal = Math.abs(direction.getX()) > 0.5 && Math.abs(direction.getZ()) > 0.5;
            int maxLen = isDiagonal ? DIAGONAL_LENGTH : STRAIGHT_LENGTH;

            // 3. 起始位置（鸡蛋发射位置，取整到方块层）
            Location start = egg.getLocation().clone();
            start.setY(start.getBlockY());

            // 4. 左侧向量（垂直于方向，用于2格宽）
            Vector left = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

            // 5. 预计算所有方块位置（使用 Set 去重）
            Set<Location> unique = new HashSet<>();
            for (int i = 0; i < maxLen; i++) {
                Location base = start.clone().add(direction.clone().multiply(i));
                base.setY(start.getBlockY());

                // 中心块
                unique.add(base.clone());
                // 左侧块（2格宽）
                unique.add(base.clone().add(left));

                // 斜向补全：在下一格的左侧额外补一格
                if (isDiagonal && i < maxLen - 1) {
                    Location next = base.clone().add(direction);
                    unique.add(next.clone().add(left));
                }
            }
            blocksToPlace.addAll(unique);

            // 6. 启动定时任务（每 tick 放 5 块）
            this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::run, 0L, 1L);
        }

        public void run() {
            // 玩家离线或离开竞技场，或已放完
            if (!arena.isPlayer(player) || currentIndex >= blocksToPlace.size()) {
                cancel();
                return;
            }

            // 每 tick 放 5 块
            int count = 0;
            while (currentIndex < blocksToPlace.size() && count < 5) {
                placeBlock(blocksToPlace.get(currentIndex));
                currentIndex++;
                count++;
            }

            if (currentIndex >= blocksToPlace.size()) {
                cancel();
            }
        }

        private void placeBlock(Location location) {
            Location blockLoc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Block block = blockLoc.getBlock();

            // 保护检查
            if (Misc.isBuildProtected(blockLoc, arena)) return;
            // 仅替换空气
            if (block.getType() != Material.AIR) return;

            // 放置羊毛
            block.setType(nms.woolMaterial());
            nms.setBlockTeamColor(block, arena.getTeam(player).getColor());
            arena.addPlacedBlock(block);

            // 触发事件（可选）
            // Bukkit.getPluginManager().callEvent(new EggBridgeBuildEvent(...));
            // 播放效果
            blockLoc.getWorld().playEffect(blockLoc, nms.eggBridge(), 3);
            Sounds.playSound("egg-bridge-block", player);
        }

        public void cancel() {
            task.cancel();
            if (!egg.isDead()) {
                egg.remove();
            }
        }
    }
}