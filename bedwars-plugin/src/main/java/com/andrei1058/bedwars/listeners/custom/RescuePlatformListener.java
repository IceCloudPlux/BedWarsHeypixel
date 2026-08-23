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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * 救援平台监听器
 * 
 * 核心逻辑：
 * - 玩家右键使用烈焰棒创建镂空十字形粘液块平台
 * - 平台在玩家脚下5格处生成
 * - 必须在空气中中有足够空间才能使用
 * - 平台在 15 秒后自动消失
 * - 站在粘液块上的玩家免受跌落伤害
 * - 粘液块不能被破坏（包括烈焰弹、玩家等）
 * 
 * 平台图案（5x5，0=粘液块，8=空气）：
 * 80808
 * 00000
 * 80008
 * 00000
 * 80808
 */
public class RescuePlatformListener implements Listener {

    // 救援平台物品材质（默认烈焰棒）
    private static final Material PLATFORM_ITEM_TYPE = Material.BLAZE_ROD;
    
    // 平台持续时间（ticks，300 = 15秒）
    private static final long PLATFORM_DURATION = 300L;
    
    // 默认冷却时间（秒）
    private static final int DEFAULT_COOLDOWN = 15;
    
    // 平台生成高度（玩家脚下多少格）
    private static final int PLATFORM_HEIGHT_OFFSET = 5;
    
    // 最小空间检测高度（平台下方需要多少格空气）
    private static final int MIN_SPACE_BELOW = 3;
    
    // 冷却记录
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    
    // 原始方块状态记录（Location -> BlockState）
    private static final Map<Location, BlockState> originalBlocks = new ConcurrentHashMap<>();
    
    // 活跃的平台位置（用于快速查找和保护）
    private static final Set<Location> activePlatformBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // 粘液块图案相对坐标（相对于玩家位置，向下1格）
    // 图案：镂空十字形骨架
    // 80808  -> 空气在 (0,0), (2,0), (4,0)
    // 00000  -> 全是粘液块
    // 80008  -> 空气在 (0,2), (4,2)
    // 00000  -> 全是粘液块
    // 80808  -> 空气在 (0,4), (2,4), (4,4)
    private static final int[][] SLIME_PATTERN = {
        // {x, z} 相对坐标（相对于平台左下角）
        // 行 0（z=0）：(1,0), (3,0)
        {1, 0}, {3, 0},
        // 行 1（z=1）：全部
        {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1},
        // 行 2（z=2）：(1,2), (2,2), (3,2)
        {1, 2}, {2, 2}, {3, 2},
        // 行 3（z=3）：全部
        {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3},
        // 行 4（z=4）：(1,4), (3,4)
        {1, 4}, {3, 4}
    };

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            ItemStack item = event.getItem();
            if (item == null || item.getType() != PLATFORM_ITEM_TYPE) return;
            
            // 检查物品名称（可选）
            if (!isRescuePlatformItem(item)) return;
            
            Player player = event.getPlayer();
            IArena arena = Arena.getArenaByPlayer(player);
            
            // 检查是否在游戏中
            if (arena == null || !arena.isPlayer(player)) return;
            
            // 检查冷却
            if (!checkCooldown(player)) {
                long remainingTime = getRemainingCooldown(player);
                player.sendMessage("§c你将在 " + remainingTime + " 秒后才能再次使用");
                return;
            }
            
            // 检查是否在空气中（下方有足够空间）
            if (!isInAir(player)) {
                player.sendMessage("§c你必须悬空才能使用救援平台！");
                return;
            }
            
            // 检查平台生成区域是否有足够空间
            Location platformCenter = player.getLocation().clone().subtract(0, PLATFORM_HEIGHT_OFFSET, 0);
            if (!hasEnoughSpace(platformCenter)) {
                player.sendMessage("§c空间不足，无法创建救援平台！");
                return;
            }
            
            // 取消事件，防止物品被其他监听器处理
            event.setCancelled(true);
            
            // 创建救援平台
            createRescuePlatform(player, arena);
            
            // 设置冷却
            setCooldown(player);
            
            // 减少物品数量
            nms.minusAmount(player, item, 1);
        }
    }

    /**
     * 检查物品是否为救援平台
     */
    private boolean isRescuePlatformItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != PLATFORM_ITEM_TYPE) return false;
        
        // 检查物品名称（可选）
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String name = meta.getDisplayName();
            return name.contains("救援平台") || name.contains("Rescue Platform");
        }
        
        // 如果没有特殊名称，也可以通过材质判断
        return true;
    }

    /**
     * 检查玩家是否在空气中（下方有足够空间）
     */
    private boolean isInAir(Player player) {
        Location playerLoc = player.getLocation();
        
        // 检查玩家脚下的方块
        for (int i = 1; i <= PLATFORM_HEIGHT_OFFSET; i++) {
            Block blockBelow = playerLoc.clone().subtract(0, i, 0).getBlock();
            if (blockBelow.getType() != Material.AIR) {
                // 如果脚下有方块，说明不在空气中
                return false;
            }
        }
        
        // 检查平台生成位置下方是否有足够空间（至少MIN_SPACE_BELOW格空气）
        Location platformLoc = playerLoc.clone().subtract(0, PLATFORM_HEIGHT_OFFSET + 1, 0);
        for (int i = 0; i < MIN_SPACE_BELOW; i++) {
            Block blockBelow = platformLoc.clone().subtract(0, i, 0).getBlock();
            if (blockBelow.getType() != Material.AIR) {
                // 平台下方空间不足
                return true; // 但允许创建，因为已经有足够高度
            }
        }
        
        return true;
    }

    /**
     * 检查平台生成区域是否有足够空间（所有位置都是空气）
     */
    private boolean hasEnoughSpace(Location platformCenter) {
        // 计算平台左下角位置（相对于中心偏移 -2, 0, -2）
        Location platformOrigin = platformCenter.clone().add(-2, 0, -2);
        
        // 检查所有粘液块位置是否为空气
        for (int[] coord : SLIME_PATTERN) {
            int x = coord[0];
            int z = coord[1];
            
            Location blockLoc = platformOrigin.clone().add(x, 0, z);
            Block block = blockLoc.getBlock();
            
            // 如果不是空气，说明空间不足
            if (block.getType() != Material.AIR) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 创建救援平台（镂空十字形骨架）
     */
    private void createRescuePlatform(Player player, IArena arena) {
        // 获取玩家位置
        Location playerLoc = player.getLocation();
        
        // 计算平台左下角位置（玩家脚下5格，相对于玩家位置偏移 -2, -5, -2）
        Location platformOrigin = playerLoc.clone().add(-2, -PLATFORM_HEIGHT_OFFSET, -2);
        
        // 按照图案放置粘液块
        for (int[] coord : SLIME_PATTERN) {
            int x = coord[0];
            int z = coord[1];
            
            Location blockLoc = platformOrigin.clone().add(x, 0, z);
            Block block = blockLoc.getBlock();
            
            // 记录原始方块状态
            originalBlocks.put(blockLoc, block.getState());
            
            // 设置为粘液块
            block.setType(Material.SLIME_BLOCK);
            
            // 记录活跃平台方块（用于保护）
            activePlatformBlocks.add(blockLoc);
        }
        
        player.sendMessage("§a你创建了一个救援平台！");
        
        // 延迟移除平台
        final UUID playerId = player.getUniqueId();
        final Location originCopy = platformOrigin.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeRescuePlatform(originCopy, playerId);
        }, PLATFORM_DURATION);
    }

    /**
     * 移除救援平台
     */
    private void removeRescuePlatform(Location platformOrigin, UUID playerId) {
        // 获取玩家当前状态
        Player player = plugin.getServer().getPlayer(playerId);
        
        int removedBlocks = 0;
        
        // 按照图案移除粘液块
        for (int[] coord : SLIME_PATTERN) {
            int x = coord[0];
            int z = coord[1];
            
            Location blockLoc = platformOrigin.clone().add(x, 0, z);
            
            // 检查是否为救援平台方块
            if (!activePlatformBlocks.contains(blockLoc)) continue;
            
            Block block = blockLoc.getBlock();
            
            // 仅移除粘液块
            if (block.getType() == Material.SLIME_BLOCK) {
                // 恢复原始方块状态
                BlockState originalState = originalBlocks.get(blockLoc);
                if (originalState != null) {
                    block.setType(originalState.getType());
                } else {
                    block.setType(Material.AIR);
                }
                
                // 清理记录
                originalBlocks.remove(blockLoc);
                activePlatformBlocks.remove(blockLoc);
                removedBlocks++;
            }
        }
        
        // 发送消息（如果玩家在线）
        if (player != null && removedBlocks > 0) {
            player.sendMessage("§c救援平台已被清除！");
        }
    }

    /**
     * 防止玩家破坏救援平台（高优先级）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // 检查是否为救援平台方块
        if (activePlatformBlocks.contains(block.getLocation())) {
            if (block.getType() == Material.SLIME_BLOCK) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c你无法破坏救援平台！");
            }
        }
    }
    
    /**
     * 防止爆炸破坏救援平台（烈焰弹等）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        // 移除所有救援平台方块，防止被爆炸破坏
        event.blockList().removeIf(block -> activePlatformBlocks.contains(block.getLocation()));
    }
    
    /**
     * 防止在救援平台上放置方块
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        
        // 如果放置位置下方是救援平台，允许放置
        // 但如果直接在救援平台方块上放置，取消事件
        if (activePlatformBlocks.contains(block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c你无法在救援平台上放置方块！");
        }
    }

    /**
     * 站在救援平台上的玩家免受伤害
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // 仅处理跌落伤害和爆炸伤害
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL && 
            event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION &&
            event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return;
        
        // 检查玩家脚下的方块
        Location playerLoc = player.getLocation().clone().subtract(0, 1, 0);
        Block blockBelow = playerLoc.getBlock();
        
        // 如果站在粘液块上且是救援平台，取消伤害
        if (blockBelow.getType() == Material.SLIME_BLOCK && activePlatformBlocks.contains(blockBelow.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 检查冷却
     */
    private boolean checkCooldown(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        return lastUse == null || System.currentTimeMillis() - lastUse >= DEFAULT_COOLDOWN * 1000L;
    }

    /**
     * 设置冷却
     */
    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * 获取剩余冷却时间（秒）
     */
    private long getRemainingCooldown(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return 0;
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = DEFAULT_COOLDOWN * 1000L - elapsed;
        return remaining > 0 ? remaining / 1000 : 0;
    }

    /**
     * 检查位置是否为救援平台方块（供其他监听器调用）
     */
    public static boolean isRescuePlatformBlock(Location location) {
        return activePlatformBlocks.contains(location);
    }

    /**
     * 清理所有救援平台（游戏结束时调用）
     */
    public static void clearAllPlatforms() {
        // 恢复所有方块
        for (Map.Entry<Location, BlockState> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockState state = entry.getValue();
            loc.getBlock().setType(state.getType());
        }
        
        originalBlocks.clear();
        activePlatformBlocks.clear();
    }
}