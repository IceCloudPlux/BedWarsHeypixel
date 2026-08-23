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
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

import static com.andrei1058.bedwars.BedWars.nms;

/**
 * 烈焰弹监听器 - 完全按照模组源码实现
 *
 * 核心逻辑（基于 fireballboom/mixin/ExplosionMixin.java）：
 * - 爆炸半径：7.0 格
 * - 水平击退计算：
 *   - 如果水平距离 > 6.0，击退为 0
 *   - 如果垂直距离 < 0.7 且水平距离 < 1.0，击退为 0
 *   - 如果水平距离 < 3.0，击退 = 水平距离 * 0.35
 *   - 否则击退 = 1.05
 * - 垂直击退：固定 1.27（水平距离 <= 6.0）
 * - 跳跃时水平击退 x2
 * - 玩家加速度缩放：
 *   - 如果原始速度 < 0.2，返回 10 * originSpeed
 *   - 如果原始速度 < 3.6，返回 1.8 + originSpeed
 *   - 否则返回 1.5 * originSpeed
 */
public class BlazeProjectileListener implements Listener {

    // 爆炸半径（模组原值：7.0）
    private static final double EXPLOSION_RADIUS = 7.0;
    // 方块破坏半径
    private static final double BLOCK_BREAK_RADIUS = 3.0;
    // 直接命中伤害
    private static final double DIRECT_HIT_DAMAGE = 6.0;
    // 溅射伤害
    private static final double SPLASH_DAMAGE = 2.0;
    // 冷却时间（毫秒）- 0.1秒
    private static final long COOLDOWN_MS = 100;
    // 方块破坏概率
    private static final double BLOCK_BREAK_CHANCE = 0.3;

    // 存储活跃的烈焰弹投掷物
    private static final Map<UUID, Player> activeBlazeProjectiles = new HashMap<>();
    // 冷却记录
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * 取消原版 Fireball 爆炸伤害（防止双重伤害）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION &&
            event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }
        
        if (event.getDamager() instanceof Fireball) {
            Fireball fireball = (Fireball) event.getDamager();
            if (activeBlazeProjectiles.containsKey(fireball.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (event.getEntity().getLocation().getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity() instanceof Fireball) {
            Projectile projectile = event.getEntity();
            
            // 跳过连携烈焰弹
            if (DualWieldComboListener.comboBlazeUUIDs.contains(projectile.getUniqueId())) {
                activeBlazeProjectiles.put(projectile.getUniqueId(), (Player) projectile.getShooter());
                return;
            }
            
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                IArena arena = Arena.getArenaByPlayer(shooter);
                if (arena != null && arena.isPlayer(shooter)) {
                    if (!checkCooldown(shooter)) {
                        event.setCancelled(true);
                        return;
                    }
                    setCooldown(shooter);
                    activeBlazeProjectiles.put(projectile.getUniqueId(), shooter);
                }
            }
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        if (!activeBlazeProjectiles.containsKey(projectile.getUniqueId())) {
            return;
        }

        Player shooter = activeBlazeProjectiles.get(projectile.getUniqueId());
        if (shooter == null) {
            activeBlazeProjectiles.remove(projectile.getUniqueId());
            return;
        }

        IArena arena = Arena.getArenaByPlayer(shooter);
        if (arena == null) {
            activeBlazeProjectiles.remove(projectile.getUniqueId());
            return;
        }

        Location hitLocation = projectile.getLocation();
        World world = hitLocation.getWorld();
        if (world == null) {
            activeBlazeProjectiles.remove(projectile.getUniqueId());
            return;
        }

        // 破坏附近方块
        breakBlocks(hitLocation, arena);

        // 获取附近的玩家
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Entity e : world.getEntities()) {
            if (e instanceof Player && e.getLocation().distance(hitLocation) <= EXPLOSION_RADIUS) {
                nearbyPlayers.add((Player) e);
            }
        }

        // 处理每个玩家
        for (Player player : nearbyPlayers) {
            if (!arena.isPlayer(player)) continue;
            
            // 计算击退（完全按照模组源码）
            applyKnockback(player, hitLocation, shooter);
        }

        activeBlazeProjectiles.remove(projectile.getUniqueId());
        projectile.remove();
    }

    /**
     * 完全按照模组源码计算击退
     */
    private void applyKnockback(Player player, Location explosionPos, Player shooter) {
        // 计算距离向量（模组源码：playerPos = 实体位置 + (0, 1, 0)）
        Vector playerPos = player.getLocation().toVector().add(new Vector(0, 1, 0));
        Vector explosionVec = explosionPos.toVector();
        Vector diff = playerPos.clone().subtract(explosionVec);
        
        // 水平距离和垂直距离
        double horizontalDistance = Math.sqrt(diff.getX() * diff.getX() + diff.getZ() * diff.getZ());
        double verticalDistance = Math.abs(diff.getY());
        
        // 如果水平距离 > 6.0，不施加击退
        if (horizontalDistance > 6.0) {
            return;
        }
        
        // 计算水平击退
        double horizontalKb = calculateHorizontalKb(horizontalDistance, verticalDistance);
        
        // 计算垂直击退
        double yKb = 1.27;
        
        // 检查是否在地面（跳跃时水平击退 x2）
        boolean onGround = player.isOnGround();
        if (!onGround) {
            horizontalKb *= 2.0;
        }
        
        // 计算方向（模组源码）
        double xKb = 0.0;
        double zKb = 0.0;
        if (horizontalDistance != 0.0) {
            xKb = diff.getX() / horizontalDistance * horizontalKb;
            zKb = diff.getZ() / horizontalDistance * horizontalKb;
        }
        
        // 玩家加速度缩放（模组源码：仅对玩家）
        Vector originSpeed = player.getVelocity();
        xKb += playerAccelerationScale(Math.abs(originSpeed.getX())) * Math.signum(originSpeed.getX());
        zKb += playerAccelerationScale(Math.abs(originSpeed.getZ())) * Math.signum(originSpeed.getZ());
        
        // 计算最终速度（模组源码：result = finalSpeed）
        // 模组源码：knockBack = finalSpeed - originSpeed; result = originSpeed + knockBack = finalSpeed
        // 所以直接设置成 finalSpeed
        Vector finalSpeed = new Vector(xKb, yKb, zKb);
        
        // 应用击退
        player.setVelocity(finalSpeed);
        
        // 施加伤害
        player.damage(SPLASH_DAMAGE);
        
        // 更新最后伤害者
        updateLastHit(player, shooter);
    }

    /**
     * 水平击退计算（模组原版）
     */
    private double calculateHorizontalKb(double horizontalDistance, double verticalDistance) {
        if (horizontalDistance > 6.0) {
            return 0.0;
        }
        if (verticalDistance < 0.7 && horizontalDistance < 1.0) {
            return 0.0;
        }
        if (horizontalDistance < 3.0) {
            return horizontalDistance * 0.35;
        }
        return 1.05;
    }

    /**
     * 玩家加速度缩放（模组原版）
     */
    private double playerAccelerationScale(double originSpeed) {
        if (originSpeed < 0.2) {
            return 10.0 * originSpeed;
        }
        if (originSpeed < 3.6) {
            return 1.8 + originSpeed;
        }
        return 1.5 * originSpeed;
    }

    /**
     * 破坏附近可破坏的方块
     */
    private void breakBlocks(Location center, IArena arena) {
        World world = center.getWorld();
        if (world == null) return;

        Random random = new Random();
        
        for (int x = (int) -BLOCK_BREAK_RADIUS; x <= BLOCK_BREAK_RADIUS; x++) {
            for (int y = (int) -BLOCK_BREAK_RADIUS; y <= BLOCK_BREAK_RADIUS; y++) {
                for (int z = (int) -BLOCK_BREAK_RADIUS; z <= BLOCK_BREAK_RADIUS; z++) {
                    Location loc = center.clone().add(x, y, z);
                    
                    if (loc.distance(center) > BLOCK_BREAK_RADIUS) continue;
                    
                    Block block = loc.getBlock();
                    Material type = block.getType();
                    
                    if (!isBreakableMaterial(type)) continue;
                    if (RescuePlatformListener.isRescuePlatformBlock(loc)) continue;
                    if (random.nextDouble() > BLOCK_BREAK_CHANCE) continue;
                    if (!arena.isBlockPlaced(block)) continue;
                    
                    block.setType(Material.AIR);
                    arena.removePlacedBlock(block);
                }
            }
        }
    }

    private boolean isBreakableMaterial(Material material) {
        String name = material.name();
        if (name.contains("WOOL")) return true;
        if (material == Material.LADDER) return true;
        return false;
    }

    private void updateLastHit(Player target, Player damager) {
        LastHit lh = LastHit.getLastHit(target);
        if (lh != null) {
            lh.setDamager(damager);
            lh.setTime(System.currentTimeMillis());
        } else {
            new LastHit(target, damager, System.currentTimeMillis());
        }
    }

    private boolean checkCooldown(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        return lastUse == null || System.currentTimeMillis() - lastUse >= COOLDOWN_MS;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public static boolean isBlazeProjectileItem(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.BLAZE_POWDER || 
               item.getType() == nms.materialFireball();
    }
}