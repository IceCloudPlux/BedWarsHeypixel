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
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * 回城卷轴监听器
 *
 * 功能：
 * - 玩家右键使用火药开始回城倒计时
 * - 5秒倒计时期间显示粒子效果和倒计时提示
 * - 倒计时结束后传送到队伍出生点
 */
public class TeleportScrollListener implements Listener {

    // 回城卷轴物品材质（火药）
    // 在 1.8.8 中叫 SULPHUR，在 1.9+ 中叫 GUNPOWDER
    private static final Material SCROLL_ITEM_TYPE;

    // 变身后的物品材质（荧石粉）
    private static final Material CHARGING_ITEM_TYPE = Material.GLOWSTONE_DUST;

    static {
        // 兼容不同版本的火药名称
        Material gunpowder = Material.matchMaterial("GUNPOWDER");
        if (gunpowder == null) {
            gunpowder = Material.matchMaterial("SULPHUR");
        }
        SCROLL_ITEM_TYPE = gunpowder;
    }

    // 倒计时时长（秒）
    private static final int TELEPORT_DURATION = 5;

    // 记录正在回城的玩家信息
    private static final Map<UUID, TeleportSession> activeSessions = new ConcurrentHashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 检查是否为右键点击
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != SCROLL_ITEM_TYPE) return;

        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);

        // 检查是否在游戏中且是存活玩家
        if (arena == null || !arena.isPlayer(player)) return;

        // 检查是否已经在回城过程中
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§c你正在回城过程中！");
            event.setCancelled(true);
            return;
        }

        // 获取玩家所在的队伍
        ITeam team = arena.getTeam(player);
        if (team == null) {
            player.sendMessage("§c你不在任何队伍中！");
            return;
        }

        // 检查队伍出生点
        Location spawnLocation = team.getSpawn();
        if (spawnLocation == null) {
            player.sendMessage("§c队伍出生点无效！");
            return;
        }

        // 取消默认的物品使用行为
        event.setCancelled(true);

        // 开始回城会话
        startTeleportSession(player, arena, team, item);
    }

    /**
     * 开始回城会话
     */
    private void startTeleportSession(Player player, IArena arena, ITeam team, ItemStack originalItem) {
        // 记录原始经验等级
        int originalExpLevel = player.getLevel();

        // 记录火药数量（减少1个）
        int originalAmount = originalItem.getAmount();
        int newAmount = originalAmount - 1;

        // 变换物品：火药 -> 荧石粉
        ItemStack chargingItem = new ItemStack(CHARGING_ITEM_TYPE);
        if (originalItem.hasItemMeta()) {
            ItemMeta meta = chargingItem.getItemMeta();
            ItemMeta originalMeta = originalItem.getItemMeta();
            if (originalMeta.hasDisplayName()) {
                meta.setDisplayName(originalMeta.getDisplayName());
            }
            chargingItem.setItemMeta(meta);
        }

        // 如果火药数量大于1，减少数量；否则移除物品
        if (newAmount > 0) {
            originalItem.setAmount(newAmount);
            // 给玩家添加荧石粉（在相同位置）
            player.getInventory().addItem(chargingItem);
        } else {
            // 移除火药，添加荧石粉
            player.getInventory().setItemInHand(chargingItem);
        }

        // 创建回城会话
        TeleportSession session = new TeleportSession(player, arena, team, originalExpLevel, chargingItem);

        // 记录会话
        activeSessions.put(player.getUniqueId(), session);

        // 开始倒计时任务
        session.startCountdown();

        player.sendMessage("§e开始回城...");
    }

    /**
     * 回城会话类
     */
    private static class TeleportSession {
        private final Player player;
        private final IArena arena;
        private final ITeam team;
        private final int originalExpLevel;
        private final ItemStack chargingItem;
        private final Location spawnLocation;

        private BukkitTask countdownTask;
        private BukkitTask particleTask;
        private int countdown;

        public TeleportSession(Player player, IArena arena, ITeam team, int originalExpLevel, ItemStack chargingItem) {
            this.player = player;
            this.arena = arena;
            this.team = team;
            this.originalExpLevel = originalExpLevel;
            this.chargingItem = chargingItem;
            this.spawnLocation = team.getSpawn().clone();
            this.countdown = TELEPORT_DURATION;
        }

        /**
         * 开始倒计时
         */
        public void startCountdown() {
            // 倒计时任务（每秒执行）
            countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline() || !arena.isPlayer(player)) {
                    // 玩家不在线或不在游戏中，取消回城
                    cancelTeleport();
                    return;
                }

                // 显示倒计时消息
                player.sendMessage("§e" + countdown + " 秒后将传送至队伍出生点...");

                // 设置经验等级为倒计时数字
                player.setLevel(countdown);

                // 倒计时结束
                if (countdown <= 0) {
                    completeTeleport();
                }

                countdown--;
            }, 0L, 20L); // 立即开始，每秒执行一次

            // 粒子效果任务（每tick执行，显示流畅的粒子效果）
            particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!activeSessions.containsKey(player.getUniqueId())) {
                    return;
                }

                // 在队伍出生点显示紫色粒子（逐渐上升）
                double height = (TELEPORT_DURATION - countdown) * 0.4; // 每秒上升0.4格，最多上升2格
                if (height > 2.0) height = 2.0;

                Location particleLoc = spawnLocation.clone().add(0, height, 0);

                // 使用 PORTAL 效果（紫色粒子）在出生点
                spawnLocation.getWorld().playEffect(particleLoc, Effect.PORTAL, 0);

                // 在玩家周围显示白色粒子（使用 SMOKE 效果）
                Location playerLoc = player.getLocation().add(0, 1, 0);
                player.getWorld().playEffect(playerLoc, Effect.SMOKE, 0);

            }, 0L, 1L); // 每 tick 执行一次
        }

        /**
         * 完成传送
         */
        private void completeTeleport() {
            // 停止任务
            if (countdownTask != null) {
                countdownTask.cancel();
            }
            if (particleTask != null) {
                particleTask.cancel();
            }

            // 传送到队伍出生点
            player.teleport(spawnLocation);

            // 恢复经验等级
            player.setLevel(originalExpLevel);

            // 移除荧石粉
            player.getInventory().remove(chargingItem);

            // 发送成功消息
            player.sendMessage("§a已传送至队伍出生点！");

            // 清理会话
            activeSessions.remove(player.getUniqueId());
        }

        /**
         * 取消传送
         */
        private void cancelTeleport() {
            // 停止任务
            if (countdownTask != null) {
                countdownTask.cancel();
            }
            if (particleTask != null) {
                particleTask.cancel();
            }

            // 恢复经验等级
            if (player.isOnline()) {
                player.setLevel(originalExpLevel);

                // 移除荧石粉
                player.getInventory().remove(chargingItem);
            }

            // 清理会话
            activeSessions.remove(player.getUniqueId());
        }
    }

    /**
     * 检查玩家是否正在回城（供其他监听器调用）
     */
    public static boolean isTeleporting(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    /**
     * 清理所有回城会话（游戏结束时调用）
     */
    public static void clearAllSessions() {
        for (TeleportSession session : activeSessions.values()) {
            if (session.countdownTask != null) {
                session.countdownTask.cancel();
            }
            if (session.particleTask != null) {
                session.particleTask.cancel();
            }

            // 恢复玩家状态
            if (session.player.isOnline()) {
                session.player.setLevel(session.originalExpLevel);
                session.player.getInventory().remove(session.chargingItem);
            }
        }

        activeSessions.clear();
    }
}