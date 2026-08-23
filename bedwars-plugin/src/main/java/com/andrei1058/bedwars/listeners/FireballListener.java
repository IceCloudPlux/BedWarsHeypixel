package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collection;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.BedWars.getAPI;

public class FireballListener implements Listener {

    private final double fireballExplosionSize;
    private final boolean fireballMakeFire;

    // 溅射击退（从配置 fireball.knockback 读取）
    private final double splashHorizontal;
    private final double splashVertical;
    // 直击击退（从配置 fireball.direct-hit 读取）
    private final double directHorizontal;
    private final double directVertical;

    // 伤害
    private final double damageSelf;
    private final double damageDirectEnemy;   // 直击伤害（从 damage.enemy 读取）
    private final double damageSplashEnemy;   // 溅射伤害（从 damage.splash-enemy 读取）
    private final double damageTeammates;

    public FireballListener() {
        this.fireballExplosionSize = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE);
        this.fireballMakeFire = config.getYml().getBoolean(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE);

        // ---- 击退参数 ----
        // 溅射
        this.splashHorizontal = Math.abs(config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL));
        this.splashVertical = Math.abs(config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL));

        // 直击
        double defaultDirectH = splashHorizontal * 0.3;
        double defaultDirectV = splashVertical * 0.3;
        this.directHorizontal = Math.abs(config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DIRECT_HIT_HORIZONTAL, defaultDirectH));
        this.directVertical = Math.abs(config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DIRECT_HIT_VERTICAL, defaultDirectV));

        // ---- 伤害参数 ----
        this.damageSelf = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF);
        this.damageDirectEnemy = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY);
        this.damageSplashEnemy = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_SPLASH_ENEMY, damageDirectEnemy);
        this.damageTeammates = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_TEAMMATES);
    }

    @EventHandler
    public void fireballHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;
        Fireball fireball = (Fireball) e.getEntity();
        Location explosionLoc = fireball.getLocation();

        ProjectileSource shooter = fireball.getShooter();
        if (!(shooter instanceof Player)) return;
        Player source = (Player) shooter;

        IArena arena = Arena.getArenaByPlayer(source);
        if (arena == null) return;

        World world = explosionLoc.getWorld();
        if (world == null) return;

        // 判断是否直击玩家（使用兼容 1.8.8 的方法）
        // 通过检测爆炸半径内最近的玩家来判断直击
        Player directTarget = null;
        double minDistance = Double.MAX_VALUE;
        for (Entity entity : world.getNearbyEntities(explosionLoc, 1.5, 1.5, 1.5)) {
            if (entity instanceof Player) {
                double dist = entity.getLocation().distance(explosionLoc);
                if (dist < minDistance) {
                    minDistance = dist;
                    directTarget = (Player) entity;
                }
            }
        }
        boolean directHitPlayer = (directTarget != null && minDistance < 1.0);

        double radius = fireballExplosionSize;
        Collection<Entity> nearby = world.getNearbyEntities(explosionLoc, radius, radius, radius);

        for (Entity entity : nearby) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;
            if (!getAPI().getArenaUtil().isPlaying(target)) continue;

            boolean isDirect = (directHitPlayer && directTarget != null && directTarget.equals(target));

            // ---- 击退计算 ----
            Vector dir = target.getLocation().toVector().subtract(explosionLoc.toVector());
            double distance = dir.length();
            if (distance < 0.1) continue;
            dir.normalize();

            double horiz, vert;
            if (isDirect) {
                horiz = directHorizontal;
                vert = directVertical;
            } else {
                horiz = splashHorizontal;
                vert = splashVertical;
            }

            // 垂直速度限制（防止过猛）
            double finalVert = Math.max(-1.5, Math.min(1.5, dir.getY() * vert));
            Vector velocity = new Vector(dir.getX() * horiz, finalVert, dir.getZ() * horiz);
            target.setVelocity(velocity);

            // ---- 伤害逻辑 ----
            double damage = 0;
            if (target.equals(source)) {
                damage = damageSelf;
            } else if (arena.getTeam(target).equals(arena.getTeam(source))) {
                damage = damageTeammates;
            } else {
                // 敌人：根据直击/溅射选择伤害
                damage = isDirect ? damageDirectEnemy : damageSplashEnemy;
            }
            if (damage > 0) {
                target.damage(damage);
            }

            // ---- 更新 LastHit ----
            LastHit lh = LastHit.getLastHit(target);
            if (lh != null) {
                lh.setDamager(source);
                lh.setTime(System.currentTimeMillis());
            } else {
                new LastHit(target, source, System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Fireball)) return;
        if (!(e.getEntity() instanceof Player)) return;
        if (Arena.getArenaByPlayer((Player) e.getEntity()) == null) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void fireballPrime(ExplosionPrimeEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;
        ProjectileSource shooter = ((Fireball) e.getEntity()).getShooter();
        if (!(shooter instanceof Player)) return;
        Player player = (Player) shooter;
        if (Arena.getArenaByPlayer(player) == null) return;
        e.setFire(fireballMakeFire);
    }
}