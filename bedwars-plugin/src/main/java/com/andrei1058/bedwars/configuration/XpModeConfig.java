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

package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigManager;

/**
 * 经验模式配置管理类
 * 管理资源转换为经验值的配置
 */
public class XpModeConfig extends ConfigManager {

    // 配置路径常量
    public static final String IRON_XP = "iron-xp";
    public static final String GOLD_XP = "gold-xp";
    public static final String EMERALD_XP = "emerald-xp";
    public static final String DIAMOND_XP = "diamond-xp";
    
    private static XpModeConfig instance;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public XpModeConfig(BedWars plugin) {
        super(plugin, "xpmode", plugin.getDataFolder().getPath());
        instance = this;
        saveDefaults();
    }

    /**
     * 保存默认配置
     */
    private void saveDefaults() {
        getYml().options().header("经验模式配置文件\n设置资源转换为经验值的数量");
        
        // 设置默认值
        getYml().addDefault(IRON_XP, 1);
        getYml().addDefault(GOLD_XP, 10);  // 1金锭=10经验
        getYml().addDefault(EMERALD_XP, 100);
        getYml().addDefault(DIAMOND_XP, 0); // 钻石默认不转换为经验值
        
        getYml().options().copyDefaults(true);
        save();
    }

    /**
     * 获取铁锭转换的经验值数量
     * @return 经验值数量
     */
    public int getIronXp() {
        return getYml().getInt(IRON_XP, 1);
    }

    /**
     * 获取金锭转换的经验值数量
     * @return 经验值数量
     */
    public int getGoldXp() {
        return getYml().getInt(GOLD_XP, 10);  // 默认值改为10
    }

    /**
     * 获取绿宝石转换的经验值数量
     * @return 经验值数量
     */
    public int getEmeraldXp() {
        return getYml().getInt(EMERALD_XP, 100);
    }

    /**
     * 获取钻石转换的经验值数量
     * @return 经验值数量
     */
    public int getDiamondXp() {
        return getYml().getInt(DIAMOND_XP, 0);
    }

    /**
     * 获取实例
     * @return XpModeConfig实例
     */
    public static XpModeConfig getInstance() {
        return instance;
    }

    /**
     * 设置铁锭转换的经验值数量
     * @param value 经验值数量
     */
    public void setIronXp(int value) {
        getYml().set(IRON_XP, value);
        save();
    }

    /**
     * 设置金锭转换的经验值数量
     * @param value 经验值数量
     */
    public void setGoldXp(int value) {
        getYml().set(GOLD_XP, value);
        save();
    }

    /**
     * 设置绿宝石转换的经验值数量
     * @param value 经验值数量
     */
    public void setEmeraldXp(int value) {
        getYml().set(EMERALD_XP, value);
        save();
    }

    /**
     * 设置钻石转换的经验值数量
     * @param value 经验值数量
     */
    public void setDiamondXp(int value) {
        getYml().set(DIAMOND_XP, value);
        save();
    }
}