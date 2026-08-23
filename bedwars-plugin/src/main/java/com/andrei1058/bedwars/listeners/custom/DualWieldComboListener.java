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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.*;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * 双持连携监听器 - 烈焰弹 + 雪球连携
 * 
 * 触发条件：
 * - 玩家抬头角度在75°~90°之间（pitch在-90到-75之间）
 * - 主手持有烈焰弹物品，副手持有雪球物品（仅1.9+）
 * - 玩家不在快速下落状态
 * - 冷却结束（1.5秒）
 * 
 * 效果：
 * - 发射烈焰弹
 * - 延迟1Tick后发射雪球
 * - 雪球撞到烈焰弹时，生成蠹虫并引爆烈焰弹（仅对发射者生效）
 */
public class DualWieldComboListener implements Listener {

    // 冷却时间（毫秒）- 0.5秒
    private static final long COOLDOWN_MS = 500;
    // 自我爆炸伤害（2点 = 1颗心）
    private static final double SELF_EXPLOSION_DAMAGE = 2.0;
    // 垂直弹射系数（降低）
    private static final double VERTICAL_LAUNCH_POWER = 1.0;
    // 烈焰弹速度
    private static final double BLAZE_PROJECTILE_SPEED = 1.5;
    // 雪球速度（比烈焰弹快，确保能追上）
    private static final double SNOWBALL_SPEED = 2.2;
    
    // 冷却记录
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    
    // 活跃的连携烈焰弹（UUID -> 连携数据）
    private static final Map<UUID, ComboData> activeCombos = new HashMap<>();
    
    // 用于标记连携烈焰弹的元数据键
    private static final String COMBO_BLAZE_KEY = "bw_combo_blaze";
    public static final String COMBO_SNOWBALL_KEY = "bw_combo_snowball";
    
    // 使用静态 Set 存储连携雪球的 UUID（比元数据更可靠）
    public static final Set<UUID> comboSnowballUUIDs = new HashSet<>();
    // 存储连携烈焰弹的 UUID
    public static final Set<UUID> comboBlazeUUIDs = new HashSet<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        
        // 检查是否在游戏中
        if (arena == null || !arena.isPlayer(player)) return;
        
        // 检查抬头角度（75°~90°）
        float pitch = player.getLocation().getPitch();
        if (pitch < -90 || pitch > -75) return;
        
        // 检查是否在快速下落状态
        if (player.getVelocity().getY() < -0.1) return;
        
        // 检查主手物品
        ItemStack mainHand = nms.getItemInHand(player);
        boolean mainHandIsBlaze = isBlazeProjectileItem(mainHand);
        
        // 检查副手物品（仅1.9+支持）
        ItemStack offHand = getOffHandItem(player);
        boolean offHandIsSnowball = isSnowballItem(offHand);
        
        if (!mainHandIsBlaze || !offHandIsSnowball) return;
        
        // 检查冷却
        if (!checkCooldown(player)) return;
        
        // 触发连携
        event.setCancelled(true);
        triggerCombo(player, arena);
    }

    /**
     * 获取副手物品（兼容1.8.8）
     */
    private ItemStack getOffHandItem(Player player) {
        // 仅在1.9+版本检查副手
        if (nms.getVersion() < 9) {
            return null;
        }
        
        try {
            // 使用反射调用 getItemInOffHand 方法
            Method method = player.getInventory().getClass().getMethod("getItemInOffHand");
            return (ItemStack) method.invoke(player.getInventory());
        } catch (Exception e) {
            // 如果反射失败，返回null
            return null;
        }
    }

    /**
     * 减少副手物品数量（兼容1.8.8）
     */
    private void minusOffHandAmount(Player player) {
        if (nms.getVersion() < 9) return;
        
        try {
            ItemStack offHand = getOffHandItem(player);
            if (offHand != null && offHand.getAmount() > 0) {
                if (offHand.getAmount() > 1) {
                    offHand.setAmount(offHand.getAmount() - 1);
                    // 使用反射设置副手物品
                    Method setItemMethod = player.getInventory().getClass().getMethod("setItemInOffHand", ItemStack.class);
                    setItemMethod.invoke(player.getInventory(), offHand);
                } else {
                    // 使用反射清空副手
                    Method setItemMethod = player.getInventory().getClass().getMethod("setItemInOffHand", ItemStack.class);
                    setItemMethod.invoke(player.getInventory(), new ItemStack(Material.AIR));
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * 触发连携
     */
    private void triggerCombo(Player player, IArena arena) {
        // 设置冷却
        setCooldown(player);
        
        // 减少物品数量
        ItemStack mainHand = nms.getItemInHand(player);
        nms.minusAmount(player, mainHand, 1);
        
        // 减少副手物品
        minusOffHandAmount(player);
        
        // 生成唯一ID
        UUID comboId = UUID.randomUUID();
        
        // 发射烈焰弹
        Vector direction = player.getEyeLocation().getDirection();
        Fireball blazeProjectile = player.launchProjectile(Fireball.class);
        blazeProjectile.setVelocity(direction.multiply(BLAZE_PROJECTILE_SPEED));
        blazeProjectile.setMetadata(COMBO_BLAZE_KEY, new FixedMetadataValue(plugin, comboId.toString()));
        
        // 使用静态 Set 标记
        comboBlazeUUIDs.add(blazeProjectile.getUniqueId());
        
        // 存储连携数据
        activeCombos.put(blazeProjectile.getUniqueId(), new ComboData(player.getUniqueId(), comboId, arena));
        
        // 延迟1Tick后发射雪球
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !arena.isPlayer(player)) return;
            
            // 从眼部位置发射雪球（更高点位）
            Location eyeLoc = player.getEyeLocation().clone();
            
            // 先在玩家位置创建雪球实体，设置元数据后再发射
            // 这样可以确保 ProjectileLaunchEvent 触发时元数据已存在
            World world = player.getWorld();
            Snowball snowball = (Snowball) world.spawnEntity(eyeLoc, EntityType.SNOWBALL);
            snowball.setShooter(player);
            snowball.setMetadata(COMBO_SNOWBALL_KEY, new FixedMetadataValue(plugin, comboId.toString()));
            snowball.setVelocity(direction.multiply(SNOWBALL_SPEED));
            
            // 使用静态 Set 标记（比元数据更可靠）
            comboSnowballUUIDs.add(snowball.getUniqueId());
            
            // 更新连携数据，记录雪球
            ComboData data = activeCombos.get(blazeProjectile.getUniqueId());
            if (data != null) {
                data.snowballId = snowball.getUniqueId();
            }
        }, 1L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        // 检查是否为连携雪球
        if (projectile.hasMetadata(COMBO_SNOWBALL_KEY)) {
            handleComboSnowballHit(event, projectile);
            return;
        }
        
        // 检查是否为连携烈焰弹
        if (projectile.hasMetadata(COMBO_BLAZE_KEY)) {
            handleComboBlazeHit(projectile);
        }
    }

    /**
     * 处理连携雪球命中
     */
    private void handleComboSnowballHit(ProjectileHitEvent event, Projectile snowball) {
        String comboIdStr = snowball.getMetadata(COMBO_SNOWBALL_KEY).get(0).asString();
        
        // 检查是否撞到烈焰弹（通过距离判断）
        Location snowballLoc = snowball.getLocation();
        Fireball hitBlaze = null;
        
        for (Entity e : snowball.getNearbyEntities(2, 2, 2)) {
            if (e instanceof Fireball && e.hasMetadata(COMBO_BLAZE_KEY)) {
                String blazeComboId = e.getMetadata(COMBO_BLAZE_KEY).get(0).asString();
                if (blazeComboId.equals(comboIdStr)) {
                    hitBlaze = (Fireball) e;
                    break;
                }
            }
        }
        
        if (hitBlaze != null) {
            // 触发连携爆炸
            triggerComboExplosion(snowball, hitBlaze);
            return;
        }
        
        // 否则，生成蠹虫（同普通雪球）
        spawnSilverfish(snowball.getLocation());
        snowball.remove();
    }

    /**
     * 触发连携爆炸
     */
    private void triggerComboExplosion(Projectile snowball, Fireball blazeProjectile) {
        // 获取连携数据
        ComboData data = activeCombos.get(blazeProjectile.getUniqueId());
        if (data == null) return;
        
        Player shooter = plugin.getServer().getPlayer(data.playerId);
        if (shooter == null) {
            cleanup(blazeProjectile, snowball);
            return;
        }
        
        Location explosionLoc = blazeProjectile.getLocation();
        World world = explosionLoc.getWorld();
        if (world == null) {
            cleanup(blazeProjectile, snowball);
            return;
        }
        
        // 生成蠹虫
        spawnSilverfish(explosionLoc);
        
        // 移除投掷物
        blazeProjectile.remove();
        snowball.remove();
        
        // 对发射者造成伤害和击退
        if (shooter.getLocation().distance(explosionLoc) <= 5) {
            // 极低伤害
            shooter.damage(SELF_EXPLOSION_DAMAGE);
            
            // 垂直向上击退
            Vector launchVector = new Vector(0, VERTICAL_LAUNCH_POWER, 0);
            shooter.setVelocity(launchVector);
            
            // 记录最后伤害者（自己）
            updateLastHit(shooter, shooter);
        }
        
        // 清理数据
        activeCombos.remove(blazeProjectile.getUniqueId());
    }

    /**
     * 处理连携烈焰弹命中（未与雪球碰撞时）
     */
    private void handleComboBlazeHit(Projectile blazeProjectile) {
        // 如果烈焰弹先命中了其他东西（不是雪球），则清理数据
        ComboData data = activeCombos.get(blazeProjectile.getUniqueId());
        if (data != null) {
            // 超时处理：如果烈焰弹已经存在超过5秒，则自动清理
            if (System.currentTimeMillis() - data.timestamp > 5000) {
                blazeProjectile.remove();
                activeCombos.remove(blazeProjectile.getUniqueId());
            }
        }
    }

    /**
     * 生成蠹虫
     */
    private void spawnSilverfish(Location location) {
        if (location.getWorld() == null) return;
        
        location.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.SILVERFISH);
        // 注意：setAI() 和 addScoreboardTag() 在 1.8.8 中不可用
    }

    /**
     * 检查冷却
     */
    private boolean checkCooldown(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        return lastUse == null || System.currentTimeMillis() - lastUse >= COOLDOWN_MS;
    }

    /**
     * 设置冷却
     */
    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * 更新最后伤害者记录
     */
    private void updateLastHit(Player target, Player damager) {
        LastHit lh = LastHit.getLastHit(target);
        if (lh != null) {
            lh.setDamager(damager);
            lh.setTime(System.currentTimeMillis());
        } else {
            new LastHit(target, damager, System.currentTimeMillis());
        }
    }

    /**
     * 清理投掷物
     */
    private void cleanup(Fireball blaze, Projectile snowball) {
        if (blaze != null) blaze.remove();
        if (snowball != null) snowball.remove();
        activeCombos.remove(blaze.getUniqueId());
    }

    /**
     * 检查物品是否为烈焰弹
     */
    private boolean isBlazeProjectileItem(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.BLAZE_POWDER || 
               item.getType() == nms.materialFireball();
    }
    
    /**
     * 检查物品是否为雪球
     */
    private boolean isSnowballItem(ItemStack item) {
        if (item == null) return false;
        // 使用 nms 工具获取正确的材质（跨版本兼容）
        return item.getType() == nms.materialSnowball();
    }

    /**
     * 连携数据类
     */
    private static class ComboData {
        final UUID playerId;
        final UUID comboId;
        final long timestamp;
        UUID snowballId;
        final IArena arena;

        ComboData(UUID playerId, UUID comboId, IArena arena) {
            this.playerId = playerId;
            this.comboId = comboId;
            this.timestamp = System.currentTimeMillis();
            this.arena = arena;
        }
    }
}