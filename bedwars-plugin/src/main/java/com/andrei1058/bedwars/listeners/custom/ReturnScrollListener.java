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
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * 回城卷轴监听器
 * 
 * 完整功能实现：
 * 1. 检测玩家是否在游戏中且是存活玩家
 * 2. 无论手上有多少火药，都替换为一个荧石粉
 * 3. 5秒倒计时，显示聊天消息和经验等级
 * 4. 前4秒：队伍出生点紫色粒子圆环，缓慢上升至2格
 * 5. 最后1秒：玩家周围白色粒子圆环
 * 6. 移动检测：精确到0.001格
 * 7. 支持右键取消和移动取消
 * 8. 物品返还：原有火药数量-1
 */
public class ReturnScrollListener implements Listener {

    // 回城卷轴物品材质（火药）
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
    
    // 粒子圆环的点数（至少8个点）
    private static final int PARTICLE_POINTS = 8;
    
    // 记录正在回城的玩家信息
    private static final Map<UUID, TeleportSession> activeSessions = new HashMap<>();
    
    /**
     * 处理玩家右键点击物品
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        // 检查是否为右键点击
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        Player player = event.getPlayer();
        
        // 检查是否右键荧石粉（取消传送）
        if (item.getType() == CHARGING_ITEM_TYPE) {
            TeleportSession session = activeSessions.get(player.getUniqueId());
            if (session != null) {
                event.setCancelled(true);
                cancelTeleport(player, session, "右键取消");
            }
            return;
        }
        
        // 检查是否右键火药（开始传送）
        if (item.getType() != SCROLL_ITEM_TYPE) return;
        
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
     * 处理玩家移动（精确检测，0.001格即判定为移动）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        TeleportSession session = activeSessions.get(player.getUniqueId());
        
        if (session == null) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // 精确检测坐标变化（精确到小数点后3位，即0.001格）
        double dx = Math.abs(from.getX() - to.getX());
        double dy = Math.abs(from.getY() - to.getY());
        double dz = Math.abs(from.getZ() - to.getZ());
        
        // 只要坐标变化超过0.001格，就判定为移动
        if (dx > 0.001 || dy > 0.001 || dz > 0.001) {
            // 玩家移动，取消传送
            cancelTeleport(player, session, "移动取消");
        }
    }
    
    /**
     * 禁止玩家丢弃物品
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (!activeSessions.containsKey(player.getUniqueId())) return;
        
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        // 禁止丢弃荧石粉或火药
        if (droppedItem.getType() == CHARGING_ITEM_TYPE || 
            droppedItem.getType() == SCROLL_ITEM_TYPE) {
            event.setCancelled(true);
            player.sendMessage("§c倒计时期间不能丢弃物品！");
        }
    }
    
    /**
     * 禁止玩家通过点击物品栏移动物品
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (!activeSessions.containsKey(player.getUniqueId())) return;
        
        // 检查是否点击了荧石粉或火药
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && 
            (clickedItem.getType() == CHARGING_ITEM_TYPE || 
             clickedItem.getType() == SCROLL_ITEM_TYPE)) {
            event.setCancelled(true);
            player.sendMessage("§c倒计时期间不能移动物品！");
        }
    }
    
    /**
     * 开始回城会话
     */
    private void startTeleportSession(Player player, IArena arena, ITeam team, ItemStack originalItem) {
        // 记录原始经验等级
        int originalExpLevel = player.getLevel();
        
        // 记录火药原始数量
        int originalAmount = originalItem.getAmount();
        
        // 创建荧石粉（1个）
        ItemStack chargingItem = new ItemStack(CHARGING_ITEM_TYPE, 1);
        if (originalItem.hasItemMeta() && originalItem.getItemMeta().hasDisplayName()) {
            ItemMeta meta = chargingItem.getItemMeta();
            meta.setDisplayName(originalItem.getItemMeta().getDisplayName());
            chargingItem.setItemMeta(meta);
        }
        
        // 直接将手上的火药替换为一个荧石粉
        player.getInventory().setItemInHand(chargingItem);
        
        // 创建回城会话
        TeleportSession session = new TeleportSession(
            player, arena, team, originalExpLevel, 
            originalAmount, chargingItem
        );
        
        // 记录会话
        activeSessions.put(player.getUniqueId(), session);
        
        // 开始倒计时任务
        session.startCountdown();
        
        player.sendMessage("§e开始回城...");
    }
    
    /**
     * 取消传送（统一处理右键取消和移动取消）
     */
    private void cancelTeleport(Player player, TeleportSession session, String reason) {
        // 停止任务
        session.stop();
        
        // 移除荧石粉
        player.getInventory().remove(session.chargingItem);
        
        // 返还火药（原有数量-1）
        if (session.originalAmount > 1) {
            ItemStack gunpowder = new ItemStack(SCROLL_ITEM_TYPE, session.originalAmount - 1);
            if (session.chargingItem.hasItemMeta() && session.chargingItem.getItemMeta().hasDisplayName()) {
                ItemMeta meta = gunpowder.getItemMeta();
                meta.setDisplayName(session.chargingItem.getItemMeta().getDisplayName());
                gunpowder.setItemMeta(meta);
            }
            player.getInventory().addItem(gunpowder);
        }
        
        // 恢复经验等级
        player.setLevel(session.originalExpLevel);
        
        // 发送取消消息
        if ("右键取消".equals(reason)) {
            player.sendMessage("§c回城已取消！消耗了1个回城卷轴。");
        } else if ("移动取消".equals(reason)) {
            player.sendMessage("§c回城已取消！你移动了，消耗了1个回城卷轴。");
        }
        
        // 清理会话
        activeSessions.remove(player.getUniqueId());
    }
    
    /**
     * 回城会话类
     */
    private class TeleportSession {
        private final Player player;
        private final IArena arena;
        private final ITeam team;
        private final int originalExpLevel;
        private final int originalAmount;
        private final ItemStack chargingItem;
        private final Location spawnLocation;
        
        private BukkitTask countdownTask;
        private BukkitTask particleTask;
        private int countdown;
        
        public TeleportSession(Player player, IArena arena, ITeam team, 
                              int originalExpLevel, int originalAmount,
                              ItemStack chargingItem) {
            this.player = player;
            this.arena = arena;
            this.team = team;
            this.originalExpLevel = originalExpLevel;
            this.originalAmount = originalAmount;
            this.chargingItem = chargingItem;
            this.spawnLocation = team.getSpawn().clone();
            this.countdown = TELEPORT_DURATION;
        }
        
        /**
         * 开始倒计时
         */
        public void startCountdown() {
            // 倒计时任务（每秒执行）
            countdownTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !arena.isPlayer(player)) {
                        // 玩家不在线或不在游戏中，取消回城
                        stop();
                        activeSessions.remove(player.getUniqueId());
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
                }
            }.runTaskTimer(plugin, 0L, 20L); // 立即开始，每秒执行一次
            
            // 粒子效果任务（每tick执行）
            particleTask = new BukkitRunnable() {
                private double currentHeight = 0;
                
                @Override
                public void run() {
                    if (!activeSessions.containsKey(player.getUniqueId())) {
                        return;
                    }
                    
                    // 前4秒：在队伍出生点生成紫色粒子圆环，缓慢上升
                    if (countdown > 1) {
                        // 每秒上升 0.5 格，前 4 秒最多上升到 2 格
                        currentHeight = (TELEPORT_DURATION - countdown) * 0.5;
                        if (currentHeight > 2.0) currentHeight = 2.0;
                        
                        // 在出生点生成紫色粒子圆环
                        createPurpleParticleRing(spawnLocation, currentHeight);
                    } else {
                        // 最后1秒：在玩家周围生成白色粒子圆环
                        createWhiteParticleRing(player.getLocation());
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L); // 每 tick 执行一次
        }
        
        /**
         * 在出生点生成紫色粒子圆环（至少8个点）
         */
        private void createPurpleParticleRing(Location center, double height) {
            double radius = 1.5;
            
            for (int i = 0; i < PARTICLE_POINTS; i++) {
                double angle = 2 * Math.PI * i / PARTICLE_POINTS;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                
                Location particleLoc = new Location(
                    center.getWorld(), 
                    x, 
                    center.getY() + height, 
                    z
                );
                
                // 使用 PORTAL 效果（紫色粒子）
                center.getWorld().playEffect(particleLoc, Effect.PORTAL, 0);
            }
        }
        
        /**
         * 在玩家周围生成白色粒子圆环（至少8个点）
         */
        private void createWhiteParticleRing(Location playerLoc) {
            double radius = 1.0;
            
            for (int i = 0; i < PARTICLE_POINTS; i++) {
                double angle = 2 * Math.PI * i / PARTICLE_POINTS;
                double x = playerLoc.getX() + radius * Math.cos(angle);
                double z = playerLoc.getZ() + radius * Math.sin(angle);
                
                Location particleLoc = new Location(
                    playerLoc.getWorld(), 
                    x, 
                    playerLoc.getY() + 1.0, // 玩家腰部高度
                    z
                );
                
                // 使用 SMOKE 效果（白色/灰色粒子）
                playerLoc.getWorld().playEffect(particleLoc, Effect.SMOKE, 0);
            }
        }
        
        /**
         * 完成传送
         */
        private void completeTeleport() {
            // 停止任务
            stop();
            
            // 传送到队伍出生点
            player.teleport(spawnLocation);
            
            // 恢复经验等级
            player.setLevel(originalExpLevel);
            
            // 移除荧石粉
            player.getInventory().remove(chargingItem);
            
            // 返还火药（原有数量-1）
            if (originalAmount > 1) {
                ItemStack gunpowder = new ItemStack(SCROLL_ITEM_TYPE, originalAmount - 1);
                if (chargingItem.hasItemMeta() && chargingItem.getItemMeta().hasDisplayName()) {
                    ItemMeta meta = gunpowder.getItemMeta();
                    meta.setDisplayName(chargingItem.getItemMeta().getDisplayName());
                    gunpowder.setItemMeta(meta);
                }
                player.getInventory().addItem(gunpowder);
            }
            
            // 发送成功消息
            player.sendMessage("§a已传送至队伍出生点！");
            
            // 清理会话
            activeSessions.remove(player.getUniqueId());
        }
        
        /**
         * 停止所有任务
         */
        public void stop() {
            if (countdownTask != null) {
                countdownTask.cancel();
            }
            if (particleTask != null) {
                particleTask.cancel();
            }
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
            session.stop();
            
            // 恢复玩家状态
            if (session.player.isOnline()) {
                session.player.setLevel(session.originalExpLevel);
                session.player.getInventory().remove(session.chargingItem);
            }
        }
        
        activeSessions.clear();
    }
}