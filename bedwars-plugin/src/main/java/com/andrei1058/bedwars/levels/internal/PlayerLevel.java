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

package com.andrei1058.bedwars.levels.internal;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.events.player.PlayerLevelUpEvent;
import com.andrei1058.bedwars.api.events.player.PlayerXpGainEvent;
import com.andrei1058.bedwars.configuration.LevelsConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家等级系统
 * 
 * 阶级系统：
 * - 每达到40星增加1阶
 * - 例如：40星=1阶40星，80星=2阶80星，120星=3阶120星
 * - 星级不会重置，阶级增长时保持原始星级
 */
@SuppressWarnings("WeakerAccess")
public class PlayerLevel {

    private UUID uuid;
    private int level; // 总星级
    private int nextLevelCost;
    private String levelName;
    private int currentXp;
    private String progressBar;
    private String requiredXp;
    private String formattedCurrentXp;
    
    // 阶级系统常量
    private static final int STARS_PER_TIER = 40;
    
    // 缓存的阶数
    private int tier;

    // keep trace if current level is different than the one in database
    private boolean modified = false;

    private static ConcurrentHashMap<UUID, PlayerLevel> levelByPlayer = new ConcurrentHashMap<>();


    /**
     * Cache a player level.
     */
    public PlayerLevel(UUID player, int level, int currentXp) {
        this.uuid = player;
        setLevelName(level);
        setNextLevelCost(level, true);

        //fix levels broken in the past by an issue
        if (level < 1) level = 1;
        if (currentXp < 0) currentXp = 0;

        this.level = level;
        this.currentXp = currentXp;
        updateTier();
        updateProgressBar();
        if (!levelByPlayer.containsKey(player)) levelByPlayer.put(player, this);
    }

    /**
     * 更新阶数
     * 每达到40星增加1阶，星级不重置
     */
    private void updateTier() {
        updateTier(this.level);
    }

    /**
     * 更新阶数（使用指定的星数）
     * 每达到40星增加1阶，星级不重置
     * @param stars 当前星数
     */
    private void updateTier(int stars) {
        // 计算阶数（每40星一阶）
        // 1-39星 = 0阶, 40-79星 = 1阶, 80-119星 = 2阶, 以此类推
        this.tier = stars / STARS_PER_TIER;
    }

    public void setLevelName(int level) {
        // 更新阶数（使用传入的 level 参数）
        updateTier(level);

        // 根据阶数和星级生成显示名称
        this.levelName = ChatColor.translateAlternateColorCodes('&', generateLevelDisplay(level, tier));
    }

    /**
     * 生成等级显示字符串
     */
    private String generateLevelDisplay(int stars, int tier) {
        // 13阶及以上使用特殊格式
        if (tier >= 13) {
            return generateHighTierDisplay(stars);
        }
        
        // 获取阶数对应的颜色
        String color = getTierColor(tier);
        
        // 12阶使用粗体
        if (tier == 12) {
            return color + "&l[" + tier + "阶" + stars + "星] ";
        }
        
        // 普通格式
        return color + "[" + tier + "阶" + stars + "星] ";
    }

    /**
     * 生成13阶及以上的特殊显示格式
     * 格式：[&aA&3B&b阶&dC&5D&6E&e星]
     */
    private String generateHighTierDisplay(int stars) {
        String tierStr = String.valueOf(tier);
        String starsStr = String.valueOf(stars);
        
        StringBuilder result = new StringBuilder();
        result.append("&r[");
        
        // 阶数的彩色显示（交替颜色）
        String[] tierColors = {"&a", "&3", "&b", "&d", "&5"};
        for (int i = 0; i < tierStr.length(); i++) {
            String color = tierColors[i % tierColors.length];
            result.append(color).append(tierStr.charAt(i));
        }
        result.append("&b阶");
        
        // 星数的彩色显示
        String[] starColors = {"&d", "&5", "&6", "&e", "&c"};
        for (int i = 0; i < starsStr.length(); i++) {
            String color = starColors[i % starColors.length];
            result.append(color).append(starsStr.charAt(i));
        }
        result.append("&c星");
        result.append("&r]");
        
        return result.toString() + " ";
    }

    /**
     * 根据阶数获取颜色
     */
    private String getTierColor(int tier) {
        switch (tier) {
            case 0: return "&7";  // 灰色
            case 1: return "&f";  // 白色
            case 2: return "&2";  // 深绿色
            case 3: return "&a";  // 绿色
            case 4: return "&3";  // 青色
            case 5: return "&b";  // 蓝色
            case 6: return "&d";  // 粉色
            case 7: return "&5";  // 深粉色
            case 8: return "&6";  // 橙色
            case 9: return "&e";  // 黄色
            case 10: return "&c"; // 红色
            case 11: return "&4"; // 深红色
            case 12: return "&e"; // 黄色+粗体（在generateLevelDisplay中处理）
            default: return "&f"; // 默认白色
        }
    }

    public void setNextLevelCost(int level, boolean initialize) {
        if (!initialize) modified = true;
        this.nextLevelCost = LevelsConfig.getNextCost(level);
    }

    public void lazyLoad(int level, int currentXp) {
        modified = false;
        if (level < 1) level = 1;
        if (currentXp < 0) currentXp = 0;
        setLevelName(level);
        setNextLevelCost(level, true);
        this.level = level;
        this.currentXp = currentXp;
        updateTier();
        updateProgressBar();

        modified = false;
    }

    /**
     * Update the player progress bar.
     */
    private void updateProgressBar() {
        double l1 = ((nextLevelCost - currentXp) / (double) (nextLevelCost)) * 10;
        int locked = (int) l1;
        int unlocked = 10 - locked;
        if (locked < 0 || unlocked < 0) {
            locked = 10;
            unlocked = 0;
        }
        progressBar = ChatColor.translateAlternateColorCodes('&', LevelsConfig.levels.getString("progress-bar.format").replace("{progress}",
                LevelsConfig.levels.getString("progress-bar.unlocked-color") + String.valueOf(new char[unlocked]).replace("\0", LevelsConfig.levels.getString("progress-bar.symbol"))
                        + LevelsConfig.levels.getString("progress-bar.locked-color") + String.valueOf(new char[locked]).replace("\0", LevelsConfig.levels.getString("progress-bar.symbol"))));
        requiredXp = formatNumber(nextLevelCost);
        formattedCurrentXp = formatNumber(currentXp);
    }

    /**
     * Get player current level (total stars).
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get player current tier.
     */
    public int getTier() {
        return tier;
    }

    /**
     * Get the amount of xp required to level up.
     */
    public int getNextLevelCost() {
        return nextLevelCost;
    }

    /**
     * Get PlayerLevel by player.
     */
    public static PlayerLevel getLevelByPlayer(UUID player) {
        return levelByPlayer.getOrDefault(player, new PlayerLevel(player, 1, 0));
    }

    /**
     * Get player uuid.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Get player current level display name.
     */
    public String getLevelName() {
        return levelName;
    }

    /**
     * Get player xp.
     */
    public int getCurrentXp() {
        return currentXp;
    }

    /**
     * Get progress bar for player.
     */
    public String getProgress() {
        return progressBar;
    }

    /**
     * Get target xp already formatted.
     * Like: 2000 is 2k
     */
    public String getFormattedRequiredXp() {
        return requiredXp;
    }

    /**
     * Add xp to player with source.
     */
    public void addXp(int xp, PlayerXpGainEvent.XpSource source) {
        if (xp < 0) return;
        this.currentXp += xp;
        upgradeLevel();
        updateProgressBar();
        
        // 播放经验球拾取音效
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            try {
                // 使用entity.experience_orb.pickup音效
                player.playSound(player.getLocation(), 
                    Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP"), 
                    1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // 如果新版本音效不存在，尝试旧版本音效
                try {
                    player.playSound(player.getLocation(), 
                        Sound.valueOf("ORB_PICKUP"), 
                        1.0f, 1.0f);
                } catch (IllegalArgumentException ignored) {
                    // 忽略音效播放失败
                }
            }
        }
        
        Bukkit.getPluginManager().callEvent(new PlayerXpGainEvent(Bukkit.getPlayer(uuid), xp, source));
        modified = true;
    }

    /**
     * Set player xp.
     */
    public void setXp(int currentXp) {
        if (currentXp <= 0) currentXp = 0;
        this.currentXp = currentXp;
        upgradeLevel();
        updateProgressBar();
        modified = true;
    }

    /**
     * Set player level.
     */
    public void setLevel(int level) {
        this.level = level;
        nextLevelCost = LevelsConfig.getNextCost(level);
        updateTier();
        setLevelName(level);
        updateProgressBar();
        modified = true;
    }

    /**
     * Get player xp already formatted.
     * Like: 1000 is 1k
     */
    public String getFormattedCurrentXp() {
        return formattedCurrentXp;
    }

    /**
     * Used to upgrade player level.
     */
    public void upgradeLevel() {
        if (currentXp >= nextLevelCost) {
            int oldTier = this.tier;
            
            currentXp = currentXp - nextLevelCost;
            level++;
            nextLevelCost = LevelsConfig.getNextCost(level);
            
            // 更新阶数
            updateTier();
            setLevelName(level);
            
            requiredXp = formatNumber(nextLevelCost);
            formattedCurrentXp = formatNumber(currentXp);
            
            // 触发升级事件
            Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(Bukkit.getPlayer(getUuid()), level, nextLevelCost));
            
            // 如果阶数提升，记录日志
            if (this.tier > oldTier) {
                Bukkit.getLogger().info("[BedWars] 玩家 " + uuid + " 晋升到 " + this.tier + " 阶！（" + level + "星）");
            }
            
            modified = true;
        }
    }

    private String formatNumber(int score) {
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);

        if (score >= 1000) {
            return format.format(score/1000.0)+"k";
        }
        return format.format(score);
    }

    /**
     * Get player level as int.
     */
    public int getPlayerLevel() {
        return level;
    }

    /**
     * Get formatted level display with tier and stars.
     */
    public String getFormattedLevel() {
        return levelName;
    }

    /**
     * Destroy data.
     */
    public void destroy() {
        levelByPlayer.remove(uuid);
        updateDatabase();
    }

    public void updateDatabase() {
        if (modified) {
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> BedWars.getRemoteDatabase().setLevelData(uuid, level, currentXp, LevelsConfig.getLevelName(level), nextLevelCost));
            modified = false;
        }
    }
}