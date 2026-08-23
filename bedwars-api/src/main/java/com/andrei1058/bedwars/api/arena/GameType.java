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

package com.andrei1058.bedwars.api.arena;

/**
 * 游戏模式枚举
 * NORMAL - 普通模式，使用物品作为货币
 * XP - 经验模式，使用玩家等级作为货币
 */
public enum GameType {
    /**
     * 普通模式 - 使用铁锭、金锭、绿宝石作为货币
     */
    NORMAL("normal"),
    
    /**
     * 经验模式 - 使用玩家等级作为货币
     */
    XP("xp");
    
    private final String identifier;
    
    GameType(String identifier) {
        this.identifier = identifier;
    }
    
    /**
     * 获取游戏模式标识符
     * @return 标识符字符串
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * 根据标识符获取游戏模式
     * @param identifier 标识符
     * @return 游戏模式，如果找不到则返回 NORMAL
     */
    public static GameType fromIdentifier(String identifier) {
        if (identifier == null) {
            return NORMAL;
        }
        for (GameType type : values()) {
            if (type.identifier.equalsIgnoreCase(identifier)) {
                return type;
            }
        }
        return NORMAL;
    }
}