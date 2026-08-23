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
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.events.gameplay.EggBridgeBuildEvent;
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

import java.util.HashSet;
import java.util.Set;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * 搭桥蛋监听器 - 动态跟踪鸡蛋位置持续铺设
 * 
 * 核心逻辑：
 * - 鸡蛋飞行过程中，每隔 1 tick 检查位置并铺设
 * - 方向基于玩家发射时的水平朝向（忽略俯仰）
 * - 宽度：2 格宽（中心 + 左侧）
 * - 长度：直线 23 格，斜向 21 格
 * - 斜向自动补全下一格左侧，消除缺口
 * - 避免在玩家站立位置放置方块，防止挤压
 */
public class BridgeEggListener implements Listener {

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (event.getEntity().getLocation().getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity() instanceof Egg) {
            Egg egg = (Egg) event.getEntity();
            if (egg.getShooter() instanceof Player) {
                Player shooter = (Player) egg.getShooter();
                IArena arena = Arena.getArenaByPlayer(shooter);
                if (arena != null && arena.isPlayer(shooter)) {
                    TeamColor teamColor = arena.getTeam(shooter).getColor();
                    new BridgeEggTask(shooter, egg, teamColor, arena);
                }
            }
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Egg) {
            Egg egg = (Egg) event.getEntity();
            egg.remove();
        }
    }

    /**
     * 动态搭桥任务
     */
    private static class BridgeEggTask implements Runnable {
        private final Player player;
        private final Egg egg;
        private final TeamColor teamColor;
        private final IArena arena;
        private final BukkitTask task;

        // 固定水平方向（基于发射时的 yaw）
        private final Vector direction;
        private final boolean isDiagonal;
        private final int maxBlocks;

        // 已放置的方块位置（用于去重）
        private final Set<Location> placedBlocks = new HashSet<>();
        private int blocksPlaced = 0;
        private Location lastBuildLocation = null;

        // 长度常量
        private static final int STRAIGHT_LENGTH = 23;
        private static final int DIAGONAL_LENGTH = 21;

        public BridgeEggTask(Player player, Egg egg, TeamColor teamColor, IArena arena) {
            this.player = player;
            this.egg = egg;
            this.teamColor = teamColor;
            this.arena = arena;

            // 固定水平方向（忽略俯仰）
            float yaw = player.getLocation().getYaw();
            double rad = Math.toRadians(yaw);
            this.direction = new Vector(-Math.sin(rad), 0, Math.cos(rad)).normalize();

            this.isDiagonal = Math.abs(direction.getX()) > 0.5 && Math.abs(direction.getZ()) > 0.5;
            this.maxBlocks = isDiagonal ? DIAGONAL_LENGTH : STRAIGHT_LENGTH;

            // 每 tick 运行一次
            this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, 0L, 1L);
        }

        @Override
        public void run() {
            // 停止条件：鸡蛋已死、玩家离线/离开竞技场、已铺完指定长度
            if (egg.isDead() || !egg.isValid() || !arena.isPlayer(player) || blocksPlaced >= maxBlocks) {
                cancel();
                return;
            }

            // 鸡蛋离玩家太远则停止（防止无限）
            if (egg.getLocation().distance(player.getLocation()) > 40) {
                cancel();
                return;
            }

            // 鸡蛋还没飞远（2格内）时不铺设，避免铺在脚下
            if (egg.getLocation().distance(player.getLocation()) < 2.0) {
                return;
            }

            // 获取当前鸡蛋位置，向下偏移 1 格作为铺设基准点
            Location eggLoc = egg.getLocation();
            Location buildLoc = eggLoc.clone().subtract(0, 1, 0);

            // 防止同一位置重复处理（如果和上次位置一样，跳过）
            if (lastBuildLocation != null &&
                    lastBuildLocation.getBlockX() == buildLoc.getBlockX() &&
                    lastBuildLocation.getBlockZ() == buildLoc.getBlockZ()) {
                return;
            }
            lastBuildLocation = buildLoc.clone();

            // 计算左侧向量（垂直于方向）
            Vector left = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

            // 尝试放置中心块和左侧块
            boolean placedCenter = tryPlaceBlock(buildLoc);
            boolean placedLeft = tryPlaceBlock(buildLoc.clone().add(left));

            // 只有成功放置了至少一块，才增加计数
            if (placedCenter || placedLeft) {
                blocksPlaced++;
            }

            // 斜向补全：在下一格的左侧额外补一块（提前补，防止缺口）
            if (isDiagonal && blocksPlaced < maxBlocks) {
                Location next = buildLoc.clone().add(direction);
                Location nextLeft = next.clone().add(left);
                tryPlaceBlock(nextLeft);
            }

            // 如果已铺完，结束
            if (blocksPlaced >= maxBlocks) {
                cancel();
            }
        }

        /**
         * 尝试放置一个方块，若位置有效且未被放置，则放置。
         * @return true 如果成功放置
         */
        private boolean tryPlaceBlock(Location location) {
            Location blockLoc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());

            // 检查是否已放置
            if (placedBlocks.contains(blockLoc)) {
                return false;
            }

            Block block = blockLoc.getBlock();

            // 保护区域检查
            if (Misc.isBuildProtected(blockLoc, arena)) return false;
            // 仅替换空气
            if (block.getType() != Material.AIR) return false;

            // 检查是否与玩家碰撞箱重叠（防止挤压玩家）
            if (isBlockOccupiedByPlayer(blockLoc)) {
                return false;
            }

            // 放置羊毛
            block.setType(nms.woolMaterial());
            nms.setBlockTeamColor(block, teamColor);
            arena.addPlacedBlock(block);

            // 记录
            placedBlocks.add(blockLoc);

            // 触发事件
            Bukkit.getPluginManager().callEvent(new EggBridgeBuildEvent(teamColor, arena, block));
            // 播放效果
            blockLoc.getWorld().playEffect(blockLoc, nms.eggBridge(), 3);
            Sounds.playSound("egg-bridge-block", player);

            return true;
        }

        /**
         * 检测方块位置是否与玩家的碰撞箱重叠（防止玩家被卡住）
         */
        private boolean isBlockOccupiedByPlayer(Location blockLoc) {
            if (player == null || !player.isOnline()) return false;
            Location playerLoc = player.getLocation();
            // 玩家碰撞箱通常占据 (x, y, z) 到 (x+1, y+1.8, z+1)
            // 检查方块的 (x, y, z) 是否落在玩家碰撞箱内
            double px = playerLoc.getX();
            double py = playerLoc.getY();
            double pz = playerLoc.getZ();
            double bx = blockLoc.getX();
            double by = blockLoc.getY();
            double bz = blockLoc.getZ();

            // 粗略检查：如果方块中心与玩家位置的水平距离小于 1，且方块高度在玩家脚部和头部之间，则认为重叠
            double dx = Math.abs(bx + 0.5 - px);
            double dz = Math.abs(bz + 0.5 - pz);
            if (dx > 1.0 || dz > 1.0) return false;

            // 检查垂直重叠：玩家脚部 y 到 y+1.8，方块是 y 到 y+1
            if (by < py && (by + 1) < py) return false; // 方块在玩家脚下
            if (by > py + 1.8) return false; // 方块在玩家头顶

            return true;
        }

        private void cancel() {
            task.cancel();
            if (!egg.isDead()) {
                egg.remove();
            }
        }
    }
}