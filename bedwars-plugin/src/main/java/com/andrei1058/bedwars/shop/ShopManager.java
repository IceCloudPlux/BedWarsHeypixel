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

package com.andrei1058.bedwars.shop;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameType;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.shop.listeners.*;
import com.andrei1058.bedwars.shop.main.QuickBuyButton;
import com.andrei1058.bedwars.shop.main.ShopCategory;
import com.andrei1058.bedwars.shop.main.ShopIndex;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;

@SuppressWarnings("WeakerAccess")
public class ShopManager extends ConfigManager {

    public static ShopIndex shop;
    public static ShopIndex shopXp; // 经验模式商店

    public ShopManager() {
        super(BedWars.plugin, "shop", BedWars.plugin.getDataFolder().getPath());
        saveDefaults();
        loadShop();
        // 加载经验模式商店
        loadXpShop();
        registerListeners();
    }

    private void saveDefaults() {
        getYml().options().header("Shop with quick buy and tiers");

        //quick buy
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_MATERIAL, BedWars.getForCurrentVersion("NETHER_STAR", "NETHER_STAR", "NETHER_STAR"));
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_AMOUNT, 1);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_DATA, 0);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_ENCHANTED, false);

        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_MATERIAL, BedWars.getForCurrentVersion("STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE"));
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_AMOUNT, 1);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_DATA, 4);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_ENCHANTED, false);

        //separator
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_MATERIAL, BedWars.getForCurrentVersion("STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE"));
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_AMOUNT, 1);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_DATA, 7);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_ENCHANTED, false);

        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_MATERIAL, BedWars.getForCurrentVersion("STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GREEN_STAINED_GLASS_PANE"));
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_AMOUNT, 1);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_DATA, 13);
        getYml().addDefault(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_ENCHANTED, false);

        //specials
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_SILVERFISH_ENABLE, true);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_SILVERFISH_MATERIAL, BedWars.getForCurrentVersion("SNOW_BALL", "SNOW_BALL", "SNOWBALL"));
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_SILVERFISH_DATA, 0);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_SILVERFISH_HEALTH, 8.0);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_SILVERFISH_DAMAGE, 4.0);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_SILVERFISH_SPEED, 0.25);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_SILVERFISH_DESPAWN, 15);

        getYml().addDefault(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_ENABLE, true);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_MATERIAL, BedWars.getForCurrentVersion("MONSTER_EGG", "MONSTER_EGG", "HORSE_SPAWN_EGG"));
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_DATA, 0);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_HEALTH, 100.0);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_DESPAWN, 240);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_SPEED, 0.25);

        getYml().addDefault(ConfigPath.SHOP_SPECIAL_TOWER_ENABLE, true);
        getYml().addDefault(ConfigPath.SHOP_SPECIAL_TOWER_MATERIAL, BedWars.getForCurrentVersion("CHEST", "CHEST", "CHEST"));

        if (isFirstTime()) {
            //quick buy defaults
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element1.path", "blocks-category.category-content.wool");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element1.slot", 19);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element2.path", "melee-category.category-content.stone-sword");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element2.slot", 20);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element3.path", "armor-category.category-content.chainmail");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element3.slot", 21);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element4.path", "ranged-category.category-content.bow1");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element4.slot", 23);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element5.path", "potions-category.category-content.speed-potion");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element5.slot", 24);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element6.path", "utility-category.category-content.tnt");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element6.slot", 25);

            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element7.path", "blocks-category.category-content.wood");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element7.slot", 28);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element8.path", "melee-category.category-content.iron-sword");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element8.slot", 29);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element9.path", "armor-category.category-content.iron-armor");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element9.slot", 30);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element10.path", "tools-category.category-content.shears");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element10.slot", 31);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element11.path", "ranged-category.category-content.arrow");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element11.slot", 32);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element12.path", "potions-category.category-content.jump-potion");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element12.slot", 33);
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element13.path", "utility-category.category-content.water-bucket");
            getYml().addDefault(ConfigPath.SHOP_QUICK_DEFAULTS_PATH + ".element13.slot", 34);
        }

        //save default shop categories if the file was just generated
        //so the user can remove categories or add new ones
        if (isFirstTime()) {
            //BLOCKS CATEGORY
            addDefaultShopCategory(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, 1, BedWars.getForCurrentVersion("STAINED_CLAY", "STAINED_CLAY", "ORANGE_TERRACOTTA"), 1, 1, false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "wool", 19, "tier1",
                    BedWars.getForCurrentVersion("WOOL", "WOOL", "WHITE_WOOL"), 0, 16, false, 4, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "wool", "tier1", "wool", BedWars.getForCurrentVersion("WOOL", "WOOL", "WHITE_WOOL"),
                    0, 16, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "clay", 20, "tier1",
                    BedWars.getForCurrentVersion("STAINED_CLAY", "STAINED_CLAY", "ORANGE_TERRACOTTA"), 1, 16, false, 12, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "clay", "tier1", "clay", BedWars.getForCurrentVersion("STAINED_CLAY", "STAINED_CLAY", "ORANGE_TERRACOTTA"),
                    1, 16, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "glass", 21, "tier1",
                    BedWars.getForCurrentVersion("GLASS", "GLASS", "GLASS"), 0, 4, false, 12, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "glass", "tier1", "glass", BedWars.getForCurrentVersion("GLASS", "GLASS", "GLASS"),
                    0, 4, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "stone", 22, "tier1",
                    BedWars.getForCurrentVersion("ENDER_STONE", "ENDER_STONE", "END_STONE"), 0, 16, false, 24, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "stone", "tier1", "stone", BedWars.getForCurrentVersion("ENDER_STONE", "ENDER_STONE", "END_STONE"),
                    0, 16, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "ladder", 23, "tier1",
                    BedWars.getForCurrentVersion("LADDER", "LADDER", "LADDER"), 0, 16, false, 4, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "ladder", "tier1", "ladder", BedWars.getForCurrentVersion("LADDER", "LADDER", "LADDER"),
                    0, 16, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "wood", 24, "tier1",
                    BedWars.getForCurrentVersion("WOOD", "WOOD", "OAK_WOOD"), 0, 16, false, 4, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "wood", "tier1", "wood", BedWars.getForCurrentVersion("WOOD", "WOOD", "OAK_WOOD"),
                    0, 16, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "obsidian", 25, "tier1",
                    BedWars.getForCurrentVersion("OBSIDIAN", "OBSIDIAN", "OBSIDIAN"), 0, 4, false, 4, "emerald", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "obsidian", "tier1", "obsidian", BedWars.getForCurrentVersion("OBSIDIAN", "OBSIDIAN", "OBSIDIAN"),
                    0, 4, "", "", "", false);
            //

            //MELEE CATEGORY
            addDefaultShopCategory(ConfigPath.SHOP_PATH_CATEGORY_MELEE, 2, BedWars.getForCurrentVersion("GOLD_SWORD", "GOLD_SWORD", "GOLDEN_SWORD"), 0, 1, false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "stone-sword", 19, "tier1",
                    BedWars.getForCurrentVersion("STONE_SWORD", "STONE_SWORD", "STONE_SWORD"), 0, 1, false, 10, "iron", false, false, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "stone-sword", "tier1", "sword", BedWars.getForCurrentVersion("STONE_SWORD", "STONE_SWORD", "STONE_SWORD"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "iron-sword", 20, "tier1",
                    BedWars.getForCurrentVersion("IRON_SWORD", "IRON_SWORD", "IRON_SWORD"), 0, 1, false, 7, "gold", false, false, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "iron-sword", "tier1", "sword", BedWars.getForCurrentVersion("IRON_SWORD", "IRON_SWORD", "IRON_SWORD"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "diamond-sword", 21, "tier1",
                    BedWars.getForCurrentVersion("DIAMOND_SWORD", "DIAMOND_SWORD", "DIAMOND_SWORD"), 0, 1, false, 4, "emerald", false, false, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "diamond-sword", "tier1", "sword", BedWars.getForCurrentVersion("DIAMOND_SWORD", "DIAMOND_SWORD", "DIAMOND_SWORD"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "stick", 22, "tier1",
                    BedWars.getForCurrentVersion("STICK", "STICK", "STICK"), 0, 1, true, 10, "gold", false, false, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_MELEE, "stick", "tier1", "stick", BedWars.getForCurrentVersion("STICK", "STICK", "STICK"),
                    0, 1, "KNOCKBACK 1", "", "", false);

            //ARMOR CATEGORY
            addDefaultShopCategory(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, 3, BedWars.getForCurrentVersion("CHAINMAIL_BOOTS", "CHAINMAIL_BOOTS", "CHAINMAIL_BOOTS"), 0, 1, false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "chainmail", 19, "tier1",
                    BedWars.getForCurrentVersion("CHAINMAIL_BOOTS", "CHAINMAIL_BOOTS", "CHAINMAIL_BOOTS"), 0, 1, false, 40, "iron", true, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "chainmail", "tier1", "boots", BedWars.getForCurrentVersion("CHAINMAIL_BOOTS", "CHAINMAIL_BOOTS", "CHAINMAIL_BOOTS"),
                    0, 1, "", "", "", true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "chainmail", "tier1", "leggings", BedWars.getForCurrentVersion("CHAINMAIL_LEGGINGS", "CHAINMAIL_LEGGINGS", "CHAINMAIL_LEGGINGS"),
                    0, 1, "", "", "", true);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "iron-armor", 20, "tier1",
                    BedWars.getForCurrentVersion("IRON_BOOTS", "IRON_BOOTS", "IRON_BOOTS"), 0, 1, false, 12, "gold", true, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "iron-armor", "tier1", "boots", BedWars.getForCurrentVersion("IRON_BOOTS", "IRON_BOOTS", "IRON_BOOTS"),
                    0, 1, "", "", "", true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "iron-armor", "tier1", "leggings", BedWars.getForCurrentVersion("IRON_LEGGINGS", "IRON_LEGGINGS", "IRON_LEGGINGS"),
                    0, 1, "", "", "", true);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "diamond-armor", 21, "tier1",
                    BedWars.getForCurrentVersion("DIAMOND_BOOTS", "DIAMOND_BOOTS", "DIAMOND_BOOTS"), 0, 1, false, 6, "emerald", true, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "diamond-armor", "tier1", "boots", BedWars.getForCurrentVersion("DIAMOND_BOOTS", "DIAMOND_BOOTS", "DIAMOND_BOOTS"),
                    0, 1, "", "", "", true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_ARMOR, "diamond-armor", "tier1", "leggings", BedWars.getForCurrentVersion("DIAMOND_LEGGINGS", "DIAMOND_LEGGINGS", "DIAMOND_LEGGINGS"),
                    0, 1, "", "", "", true);

            //TOOLS CATEGORY
            addDefaultShopCategory(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, 4, BedWars.getForCurrentVersion("STONE_PICKAXE", "STONE_PICKAXE", "STONE_PICKAXE"), 0, 1, false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "shears", 19, "tier1",
                    BedWars.getForCurrentVersion("SHEARS", "SHEARS", "SHEARS"), 0, 1, false, 20, "iron", true, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "shears", "tier1", "shears", BedWars.getForCurrentVersion("SHEARS", "SHEARS", "SHEARS"),
                    0, 1, "", "", "", false);

            //pickaxe
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", 20, "tier1",
                    BedWars.getForCurrentVersion("WOOD_PICKAXE", "WOOD_PICKAXE", "WOODEN_PICKAXE"), 0, 1, false, 10, "iron", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", "tier1", "wooden-pickaxe", BedWars.getForCurrentVersion("WOOD_PICKAXE", "WOOD_PICKAXE", "WOODEN_PICKAXE"),
                    0, 1, "", "", "", false);
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", 20, "tier2",
                    BedWars.getForCurrentVersion("IRON_PICKAXE", "IRON_PICKAXE", "IRON_PICKAXE"), 0, 1, true, 10, "iron", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", "tier2", "iron-pickaxe", BedWars.getForCurrentVersion("IRON_PICKAXE", "IRON_PICKAXE", "IRON_PICKAXE"),
                    0, 1, "DIG_SPEED 2", "", "", false);
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", 20, "tier3",
                    BedWars.getForCurrentVersion("GOLD_PICKAXE", "GOLD_PICKAXE", "GOLDEN_PICKAXE"), 0, 1, true, 3, "gold", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", "tier3", "gold-pickaxe", BedWars.getForCurrentVersion("GOLD_PICKAXE", "GOLD_PICKAXE", "GOLDEN_PICKAXE"),
                    0, 1, "DIG_SPEED 3,DAMAGE_ALL 2", "", "", false);
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", 20, "tier4",
                    BedWars.getForCurrentVersion("DIAMOND_PICKAXE", "DIAMOND_PICKAXE", "DIAMOND_PICKAXE"), 0, 1, true, 6, "gold", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "pickaxe", "tier4", "diamond-pickaxe", BedWars.getForCurrentVersion("DIAMOND_PICKAXE", "DIAMOND_PICKAXE", "DIAMOND_PICKAXE"),
                    0, 1, "DIG_SPEED 3", "", "", false);

            //axe
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", 21, "tier1",
                    BedWars.getForCurrentVersion("WOOD_AXE", "WOOD_AXE", "WOODEN_AXE"), 0, 1, false, 10, "iron", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", "tier1", "wooden-axe", BedWars.getForCurrentVersion("WOOD_AXE", "WOOD_AXE", "WOODEN_AXE"),
                    0, 1, "DIG_SPEED 1", "", "", false);
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", 21, "tier2",
                    BedWars.getForCurrentVersion("IRON_AXE", "IRON_AXE", "IRON_AXE"), 0, 1, true, 10, "iron", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", "tier2", "iron-axe", BedWars.getForCurrentVersion("IRON_AXE", "IRON_AXE", "IRON_AXE"),
                    0, 1, "DIG_SPEED 1", "", "", false);
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", 21, "tier3",
                    BedWars.getForCurrentVersion("GOLD_AXE", "GOLD_AXE", "GOLDEN_AXE"), 0, 1, true, 3, "gold", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", "tier3", "gold-axe", BedWars.getForCurrentVersion("GOLD_AXE", "GOLD_AXE", "GOLDEN_AXE"),
                    0, 1, "DIG_SPEED 2", "", "", false);
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", 21, "tier4",
                    BedWars.getForCurrentVersion("DIAMOND_AXE", "DIAMOND_AXE", "DIAMOND_AXE"), 0, 1, true, 6, "gold", true, true);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "axe", "tier4", "diamond-axe", BedWars.getForCurrentVersion("DIAMOND_AXE", "DIAMOND_AXE", "DIAMOND_AXE"),
                    0, 1, "DIG_SPEED 3", "", "", false);

            //RANGED CATEGORY
            addDefaultShopCategory(ConfigPath.SHOP_PATH_CATEGORY_RANGED, 5, BedWars.getForCurrentVersion("BOW", "BOW", "BOW"), 0, 1, false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "arrow", 19, "tier1",
                    BedWars.getForCurrentVersion("ARROW", "ARROW", "ARROW"), 0, 8, false, 2, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "arrow", "tier1", "arrows", BedWars.getForCurrentVersion("ARROW", "ARROW", "ARROW"),
                    0, 8, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "bow1", 20, "tier1",
                    BedWars.getForCurrentVersion("BOW", "BOW", "BOW"), 0, 1, false, 12, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "bow1", "tier1", "bow", BedWars.getForCurrentVersion("BOW", "BOW", "BOW"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "bow2", 21, "tier1",
                    BedWars.getForCurrentVersion("BOW", "BOW", "BOW"), 0, 1, true, 24, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "bow2", "tier1", "bow", BedWars.getForCurrentVersion("BOW", "BOW", "BOW"),
                    0, 1, "ARROW_DAMAGE 1", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "bow3", 22, "tier1",
                    BedWars.getForCurrentVersion("BOW", "BOW", "BOW"), 0, 1, true, 6, "emerald", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_RANGED, "bow3", "tier1", "bow", BedWars.getForCurrentVersion("BOW", "BOW", "BOW"),
                    0, 1, "ARROW_DAMAGE 1,ARROW_KNOCKBACK 1", "", "", false);

            //POTIONS CATEGORY
            addDefaultShopCategory(ConfigPath.SHOP_PATH_CATEGORY_POTIONS, 6, BedWars.getForCurrentVersion("BREWING_STAND_ITEM", "BREWING_STAND_ITEM", "BREWING_STAND"), 0, 1, false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_POTIONS, "jump-potion", 20, "tier1",
                    BedWars.getForCurrentVersion("POTION", "POTION", "POTION"), 0, 1, false, 1, "emerald", false, false);
            addBuyPotion(ConfigPath.SHOP_PATH_CATEGORY_POTIONS, "jump-potion", "tier1", "jump", BedWars.getForCurrentVersion("POTION", "POTION", "POTION"),
                    0, 1, "", "JUMP 30 4", "Jump Potion");  // 30 seconds, level 4

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_POTIONS, "speed-potion", 19, "tier1",
                    BedWars.getForCurrentVersion("POTION", "POTION", "POTION"), 0, 1, false, 1, "emerald", false, false);
            addBuyPotion(ConfigPath.SHOP_PATH_CATEGORY_POTIONS, "speed-potion", "tier1", "speed", BedWars.getForCurrentVersion("POTION", "POTION", "POTION"),
                    0, 1, "", "SPEED 45 2", "Speed Potion");

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_POTIONS, "invisibility", 21, "tier1",
                    BedWars.getForCurrentVersion("POTION", "POTION", "POTION"), 0, 1, false, 2, "emerald", false, false);
            addBuyPotion(ConfigPath.SHOP_PATH_CATEGORY_POTIONS, "invisibility", "tier1", "invisibility", BedWars.getForCurrentVersion("POTION", "POTION", "POTION"),
                    0, 1, "", "INVISIBILITY 30 1", "Invisibility Potion");

            //UTILITY CATEGORY
            addDefaultShopCategory(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, 7, BedWars.getForCurrentVersion("TNT", "TNT", "TNT"), 0, 1, false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "golden-apple", 19, "tier1",
                    BedWars.getForCurrentVersion("GOLDEN_APPLE", "GOLDEN_APPLE", "GOLDEN_APPLE"), 0, 1, false, 3, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "golden-apple", "tier1", "apple", BedWars.getForCurrentVersion("GOLDEN_APPLE", "GOLDEN_APPLE", "GOLDEN_APPLE"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "bedbug", 20, "tier1",
                    BedWars.getForCurrentVersion("SNOW_BALL", "SNOW_BALL", "SNOWBALL"), 0, 1, false, 40, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "bedbug", "tier1", "bedbug", BedWars.getForCurrentVersion("SNOW_BALL", "SNOW_BALL", "SNOWBALL"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "dream-defender", 21, "tier1",
                    BedWars.getForCurrentVersion("MONSTER_EGG", "MONSTER_EGG", "HORSE_SPAWN_EGG"), 0, 1, false, 120, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "dream-defender", "tier1", "defender", BedWars.getForCurrentVersion("MONSTER_EGG", "MONSTER_EGG", "HORSE_SPAWN_EGG"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "fireball", 22, "tier1",
                    BedWars.getForCurrentVersion("FIREBALL", "FIREBALL", "FIRE_CHARGE"), 0, 1, false, 40, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "fireball", "tier1", "fireball", BedWars.getForCurrentVersion("FIREBALL", "FIREBALL", "FIRE_CHARGE"),
                    0, 1, "", "", "Fireball", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "tnt", 23, "tier1",
                    BedWars.getForCurrentVersion("TNT", "TNT", "TNT"), 0, 1, false, 4, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "tnt", "tier1", "tnt", BedWars.getForCurrentVersion("TNT", "TNT", "TNT"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "ender-pearl", 24, "tier1",
                    BedWars.getForCurrentVersion("ENDER_PEARL", "ENDER_PEARL", "ENDER_PEARL"), 0, 1, false, 4, "emerald", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "ender-pearl", "tier1", "ender-pearl", BedWars.getForCurrentVersion("ENDER_PEARL", "ENDER_PEARL", "ENDER_PEARL"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "water-bucket", 25, "tier1",
                    BedWars.getForCurrentVersion("WATER_BUCKET", "WATER_BUCKET", "WATER_BUCKET"), 0, 1, false, 4, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "water-bucket", "tier1", "water-bucket", BedWars.getForCurrentVersion("WATER_BUCKET", "WATER_BUCKET", "WATER_BUCKET"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "bridge-egg", 26, "tier1",
                    BedWars.getForCurrentVersion("EGG", "EGG", "EGG"), 0, 1, false, 3, "emerald", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "bridge-egg", "tier1", "egg", BedWars.getForCurrentVersion("EGG", "EGG", "EGG"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "magic-milk", 27, "tier1",
                    BedWars.getForCurrentVersion("MILK_BUCKET", "MILK_BUCKET", "MILK_BUCKET"), 0, 1, false, 4, "gold", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "magic-milk", "tier1", "milk", BedWars.getForCurrentVersion("MILK_BUCKET", "MILK_BUCKET", "MILK_BUCKET"),
                    0, 1, "", "", "", false);

            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "tower", 28, "tier1",
                    BedWars.getForCurrentVersion("CHEST", "CHEST", "CHEST"), 0, 1, false, 24, "iron", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "tower", "tier1", "tower", BedWars.getForCurrentVersion("CHEST", "CHEST", "CHEST"),
                    0, 1, "", "", "", false);

            // Rescue Platform - 2 emeralds (200 XP in XP mode) - BLAZE_ROD
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "rescue-platform", 32, "tier1",
                    BedWars.getForCurrentVersion("BLAZE_ROD", "BLAZE_ROD", "BLAZE_ROD"), 0, 1, false, 2, "emerald", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "rescue-platform", "tier1", "rescue-platform", BedWars.getForCurrentVersion("BLAZE_ROD", "BLAZE_ROD", "BLAZE_ROD"),
                    0, 1, "", "", "Rescue Platform", false);

            // Return Scroll - 3 emeralds (500 XP in XP mode)
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "return-scroll", 33, "tier1",
                    BedWars.getForCurrentVersion("SULPHUR", "SULPHUR", "GUNPOWDER"), 0, 1, false, 3, "emerald", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "return-scroll", "tier1", "return-scroll", BedWars.getForCurrentVersion("SULPHUR", "SULPHUR", "GUNPOWDER"),
                    0, 1, "", "", "Return Scroll", false);

            // Defense Wall - 5 emeralds (500 XP in XP mode)
            adCategoryContentTier(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "defense-wall", 34, "tier1",
                    BedWars.getForCurrentVersion("BRICK", "BRICK", "BRICKS"), 0, 1, false, 5, "emerald", false, false);
            addBuyItem(ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "defense-wall", "tier1", "defense-wall", BedWars.getForCurrentVersion("BRICK", "BRICK", "BRICKS"),
                    0, 1, "", "", "Defense Wall", false);

        }

        if (getYml().get(ConfigPath.SHOP_PATH_CATEGORY_ARMOR + ConfigPath.SHOP_CATEGORY_CONTENT_PATH + ".diamond-armor") != null) {
            getYml().addDefault(ConfigPath.SHOP_PATH_CATEGORY_ARMOR + ConfigPath.SHOP_CATEGORY_CONTENT_PATH + ".diamond-armor" + "." + ConfigPath.SHOP_CATEGORY_CONTENT_WEIGHT, 2);
        }
        if (getYml().get(ConfigPath.SHOP_PATH_CATEGORY_ARMOR + ConfigPath.SHOP_CATEGORY_CONTENT_PATH + ".iron-armor") != null) {
            getYml().addDefault(ConfigPath.SHOP_PATH_CATEGORY_ARMOR + ConfigPath.SHOP_CATEGORY_CONTENT_PATH + ".iron-armor" + "." + ConfigPath.SHOP_CATEGORY_CONTENT_WEIGHT, 1);
        }

        //try materials
        try {
            String material = getYml().getString(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_MATERIAL);
            BedWars.debug(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_MATERIAL + " is set to: " + material);
            Material.valueOf(material);
        } catch (Exception ex) {
            BedWars.plugin.getLogger().severe("Invalid material at " + ConfigPath.SHOP_SPECIAL_IRON_GOLEM_MATERIAL);
        }
        try {
            String material = getYml().getString(ConfigPath.SHOP_SPECIAL_SILVERFISH_MATERIAL);
            BedWars.debug(ConfigPath.SHOP_SPECIAL_SILVERFISH_MATERIAL + " is set to: " + material);
            Material.valueOf(material);
        } catch (Exception ex) {
            BedWars.plugin.getLogger().severe("Invalid material at " + ConfigPath.SHOP_SPECIAL_SILVERFISH_MATERIAL);
        }

        getYml().options().copyDefaults(true);
        save();
    }

    private void loadShop() {
        //Quick Buy Button
        ItemStack button = BedWars.nms.createItemStack(getYml().getString(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_MATERIAL),
                getYml().getInt(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_AMOUNT), (short) getYml().getInt(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_DATA));
        if (getYml().getBoolean(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_ENCHANTED)) button = enchantItem(button);
        QuickBuyButton qbb = new QuickBuyButton(0, button, Messages.SHOP_QUICK_BUY_NAME, Messages.SHOP_QUICK_BUY_LORE);

        //Separator
        ItemStack separatorStandard = BedWars.nms.createItemStack(getYml().getString(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_MATERIAL),
                getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_AMOUNT), (short) getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_DATA));
        if (getYml().getBoolean(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_ENCHANTED))
            separatorStandard = enchantItem(separatorStandard);
        ItemStack separatorSelected = BedWars.nms.createItemStack(getYml().getString(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_MATERIAL),
                getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_AMOUNT), (short) getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_DATA));
        if (getYml().getBoolean(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_ENCHANTED))
            separatorSelected = enchantItem(separatorSelected);

        shop = new ShopIndex(Messages.SHOP_INDEX_NAME, qbb, Messages.SHOP_SEPARATOR_NAME, Messages.SHOP_SEPARATOR_LORE, separatorSelected, separatorStandard);

        // 设置当前正在加载的商店
        ShopCategory.setCurrentLoadingShop(shop);

        for (String s : getYml().getConfigurationSection("").getKeys(false)) {
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SETTINGS_PATH)) continue;
            if (s.equals(ConfigPath.SHOP_QUICK_DEFAULTS_PATH)) continue;
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SPECIALS_PATH)) continue;
            ShopCategory sc = new ShopCategory(s, getYml());
            if (sc.isLoaded()) shop.addShopCategory(sc);
        }
        
        // 清除引用
        ShopCategory.setCurrentLoadingShop(null);
    }

    /**
     * Hide an item details
     */
    public static ItemMeta hideItemStuff(ItemMeta im) {
        if (im != null) {
            im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON);
        }
        return im;
    }

    /**
     * Enchant item stack and hide details
     */
    public static ItemStack enchantItem(ItemStack itemStack) {
        ItemStack i = new ItemStack(itemStack);
        ItemMeta im = i.getItemMeta();
        if (im != null) {
            im.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
            i.setItemMeta(hideItemStuff(im));
        }
        return i;
    }

    /**
     * Initialize a shop category to config
     */
    @SuppressWarnings("SameParameterValue")
    private void addDefaultShopCategory(String path, int slot, String material, int data, int amount, boolean enchant) {
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_SLOT, slot);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_ITEM_MATERIAL, material);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_ITEM_DATA, data);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_ITEM_AMOUNT, amount);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_ITEM_ENCHANTED, enchant);
    }

    /**
     * Create a tier for a shop content
     */
    public void adCategoryContentTier(String path, String contentName, int contentSlot, String tierName, String tierMaterial, int tierData, int amount, boolean enchant, int tierCost, String tierCurrency, boolean permanent,
                                      boolean downgradable) {
        path += ConfigPath.SHOP_CATEGORY_CONTENT_PATH + "." + contentName + ".";
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_SLOT, contentSlot);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_CONTENT_IS_PERMANENT, permanent);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_CONTENT_IS_DOWNGRADABLE, downgradable);
        path += ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_TIERS + "." + tierName;
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_MATERIAL, tierMaterial);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_DATA, tierData);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_AMOUNT, amount);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_ENCHANTED, enchant);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_COST, tierCost);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_CURRENCY, tierCurrency);
    }

    /**
     * Create a tier for a shop content (unbreakable)
     * Comment: Reason I made a new function; not all items can be unbreakable, thus shouldn't have the option.
     */
    public void adCategoryContentTier(String path, String contentName, int contentSlot, String tierName, String tierMaterial, int tierData, int amount, boolean enchant, int tierCost, String tierCurrency, boolean permanent,
                                      boolean downgradable, boolean unbreakable) {
        path += ConfigPath.SHOP_CATEGORY_CONTENT_PATH + "." + contentName + ".";
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_SLOT, contentSlot);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_CONTENT_IS_PERMANENT, permanent);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_CONTENT_IS_DOWNGRADABLE, downgradable);
        getYml().addDefault(path + ConfigPath.SHOP_CATEGORY_CONTENT_IS_UNBREAKABLE, unbreakable);
        path += ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_TIERS + "." + tierName;
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_MATERIAL, tierMaterial);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_DATA, tierData);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_AMOUNT, amount);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_ITEM_ENCHANTED, enchant);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_COST, tierCost);
        getYml().addDefault(path + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_CURRENCY, tierCurrency);
    }

    /**
     * Add buy items to a content tier
     */
    public void addBuyItem(String path, String contentName, String tierName, String item, String material, int data, int amount, String enchant, String potion, String itemName, boolean autoEquip) {
        path += ConfigPath.SHOP_CATEGORY_CONTENT_PATH + "." + contentName + "." + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_TIERS + "." + tierName + "." + ConfigPath.SHOP_CONTENT_BUY_ITEMS_PATH + "." + item + ".";
        getYml().addDefault(path + "material", material);
        getYml().addDefault(path + "data", data);
        getYml().addDefault(path + "amount", amount);
        if (!enchant.isEmpty()) {
            getYml().addDefault(path + "enchants", enchant);
        }
        if (!potion.isEmpty()) {
            getYml().addDefault(path + "potion", potion);
        }
        if (autoEquip) {
            getYml().addDefault(path + "auto-equip", true);
        }
        if (!itemName.isEmpty()) {
            getYml().addDefault(path + "name", itemName);
        }
    }

    public void addBuyPotion(String path, String contentName, String tierName, String item, String material, int data, int amount, String enchant, String potion, String itemName) {
        path += ConfigPath.SHOP_CATEGORY_CONTENT_PATH + "." + contentName + "." + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_TIERS + "." + tierName + "." + ConfigPath.SHOP_CONTENT_BUY_ITEMS_PATH + "." + item + ".";
        getYml().addDefault(path + "material", material);
        getYml().addDefault(path + "data", data);
        getYml().addDefault(path + "amount", amount);
        if (!enchant.isEmpty()) {
            getYml().addDefault(path + "enchants", enchant);
        }
        if (!potion.isEmpty()) {
            getYml().addDefault(path + "potion", potion);
        }
        getYml().addDefault(path + "potion-color", "");
        if (!itemName.isEmpty()) {
            getYml().addDefault(path + "name", itemName);
        }
    }

    public static ShopIndex getShop() {
        return shop;
    }

    /**
     * Register shop related listeners
     */
    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new InventoryListener(), BedWars.plugin);
        pm.registerEvents(new ShopCacheListener(), BedWars.plugin);
        pm.registerEvents(new QuickBuyListener(), BedWars.plugin);
        pm.registerEvents(new ShopOpenListener(), BedWars.plugin);
        pm.registerEvents(new PlayerDropListener(), BedWars.plugin);
        pm.registerEvents(new SpecialsListener(), BedWars.plugin);
    }

    /**
     * 加载经验模式商店
     */
    private void loadXpShop() {
        // 创建经验模式商店配置
        ConfigManager xpConfig = new ConfigManager(BedWars.plugin, "shopxp", BedWars.plugin.getDataFolder().getPath());
        
        // 如果文件是第一次创建，复制普通商店的结构
        if (xpConfig.isFirstTime()) {
            // 复制基本设置
            xpConfig.getYml().options().header("经验模式商店配置 - 使用玩家等级作为货币");
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_MATERIAL, BedWars.getForCurrentVersion("EXP_BOTTLE", "EXP_BOTTLE", "EXPERIENCE_BOTTLE"));
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_AMOUNT, 1);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_DATA, 0);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_ENCHANTED, false);

            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_MATERIAL, BedWars.getForCurrentVersion("STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE"));
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_AMOUNT, 1);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_DATA, 4);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_QUICK_BUY_EMPTY_ENCHANTED, false);

            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_MATERIAL, BedWars.getForCurrentVersion("STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE"));
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_AMOUNT, 1);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_DATA, 7);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_ENCHANTED, false);

            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_MATERIAL, BedWars.getForCurrentVersion("STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GREEN_STAINED_GLASS_PANE"));
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_AMOUNT, 1);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_DATA, 13);
            xpConfig.getYml().set(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_ENCHANTED, false);

            // 复制默认快速购买设置
            copyQuickBuyDefaults(xpConfig);

            // 复制所有商店分类内容，货币改为 xp
            copyShopCategories(xpConfig);
            
            // 添加特殊物品到经验模式商店
            addXpShopSpecialItems(xpConfig);

            xpConfig.getYml().options().copyDefaults(true);
            xpConfig.save();
        }
        
        // 加载经验模式商店
        ItemStack button = BedWars.nms.createItemStack(xpConfig.getYml().getString(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_MATERIAL, "EXPERIENCE_BOTTLE"),
                xpConfig.getYml().getInt(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_AMOUNT, 1), (short) xpConfig.getYml().getInt(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_DATA, 0));
        if (xpConfig.getYml().getBoolean(ConfigPath.SHOP_SETTINGS_QUICK_BUY_BUTTON_ENCHANTED, false)) button = enchantItem(button);
        QuickBuyButton qbb = new QuickBuyButton(0, button, Messages.SHOP_QUICK_BUY_NAME, Messages.SHOP_QUICK_BUY_LORE);

        ItemStack separatorStandard = BedWars.nms.createItemStack(xpConfig.getYml().getString(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_MATERIAL, "GRAY_STAINED_GLASS_PANE"),
                xpConfig.getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_AMOUNT, 1), (short) xpConfig.getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_DATA, 7));
        if (xpConfig.getYml().getBoolean(ConfigPath.SHOP_SETTINGS_SEPARATOR_REGULAR_ENCHANTED, false))
            separatorStandard = enchantItem(separatorStandard);
        ItemStack separatorSelected = BedWars.nms.createItemStack(xpConfig.getYml().getString(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_MATERIAL, "GREEN_STAINED_GLASS_PANE"),
                xpConfig.getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_AMOUNT, 1), (short) xpConfig.getYml().getInt(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_DATA, 13));
        if (xpConfig.getYml().getBoolean(ConfigPath.SHOP_SETTINGS_SEPARATOR_SELECTED_ENCHANTED, false))
            separatorSelected = enchantItem(separatorSelected);

        shopXp = new ShopIndex(Messages.SHOP_INDEX_NAME, qbb, Messages.SHOP_SEPARATOR_NAME, Messages.SHOP_SEPARATOR_LORE, separatorSelected, separatorStandard);

        // 设置当前正在加载的商店
        ShopCategory.setCurrentLoadingShop(shopXp);

        for (String s : xpConfig.getYml().getConfigurationSection("").getKeys(false)) {
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SETTINGS_PATH)) continue;
            if (s.equals(ConfigPath.SHOP_QUICK_DEFAULTS_PATH)) continue;
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SPECIALS_PATH)) continue;
            ShopCategory sc = new ShopCategory(s, xpConfig.getYml());
            if (sc.isLoaded()) shopXp.addShopCategory(sc);
        }
        
        // 清除引用
        ShopCategory.setCurrentLoadingShop(null);
        
        // 如果经验模式商店没有内容，使用普通商店的引用
        if (shopXp.getCategoryList().isEmpty()) {
            BedWars.plugin.getLogger().warning("XP Shop is empty, using normal shop as fallback.");
        }
    }

    /**
     * 复制快速购买默认设置到经验模式商店
     */
    private void copyQuickBuyDefaults(ConfigManager xpConfig) {
        if (getYml().getConfigurationSection(ConfigPath.SHOP_QUICK_DEFAULTS_PATH) != null) {
            for (String key : getYml().getConfigurationSection(ConfigPath.SHOP_QUICK_DEFAULTS_PATH).getKeys(false)) {
                String basePath = ConfigPath.SHOP_QUICK_DEFAULTS_PATH + "." + key;
                if (getYml().get(basePath + ".path") != null) {
                    xpConfig.getYml().addDefault(basePath + ".path", getYml().getString(basePath + ".path"));
                }
                if (getYml().get(basePath + ".slot") != null) {
                    xpConfig.getYml().addDefault(basePath + ".slot", getYml().getInt(basePath + ".slot"));
                }
            }
        }
    }

    /**
     * 复制商店分类内容到经验模式商店，将货币改为 xp
     */
    private void copyShopCategories(ConfigManager xpConfig) {
        // 复制所有分类
        for (String categoryKey : getYml().getKeys(false)) {
            if (categoryKey.equalsIgnoreCase(ConfigPath.SHOP_SETTINGS_PATH) || 
                categoryKey.equals(ConfigPath.SHOP_QUICK_DEFAULTS_PATH) || 
                categoryKey.equalsIgnoreCase(ConfigPath.SHOP_SPECIALS_PATH)) {
                continue;
            }

            // 复制分类设置
            String categoryPath = categoryKey;
            if (getYml().get(categoryPath + ConfigPath.SHOP_CATEGORY_SLOT) != null) {
                xpConfig.getYml().addDefault(categoryPath + ConfigPath.SHOP_CATEGORY_SLOT, 
                    getYml().getInt(categoryPath + ConfigPath.SHOP_CATEGORY_SLOT));
            }
            if (getYml().get(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_MATERIAL) != null) {
                xpConfig.getYml().addDefault(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_MATERIAL, 
                    getYml().getString(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_MATERIAL));
            }
            if (getYml().get(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_DATA) != null) {
                xpConfig.getYml().addDefault(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_DATA, 
                    getYml().getInt(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_DATA));
            }
            if (getYml().get(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_AMOUNT) != null) {
                xpConfig.getYml().addDefault(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_AMOUNT, 
                    getYml().getInt(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_AMOUNT));
            }
            if (getYml().get(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_ENCHANTED) != null) {
                xpConfig.getYml().addDefault(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_ENCHANTED, 
                    getYml().getBoolean(categoryPath + ConfigPath.SHOP_CATEGORY_ITEM_ENCHANTED));
            }

            // 复制分类内容
            String contentPath = categoryPath + ConfigPath.SHOP_CATEGORY_CONTENT_PATH;
            if (getYml().getConfigurationSection(contentPath) != null) {
                for (String contentKey : getYml().getConfigurationSection(contentPath).getKeys(false)) {
                    String contentBasePath = contentPath + "." + contentKey;
                    copyCategoryContent(xpConfig, contentBasePath);
                }
            }
        }
    }

    /**
     * 复制分类内容，将货币改为 xp
     */
    private void copyCategoryContent(ConfigManager xpConfig, String contentPath) {
        // 复制内容设置
        if (getYml().get(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_SLOT) != null) {
            xpConfig.getYml().addDefault(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_SLOT, 
                getYml().getInt(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_SLOT));
        }
        if (getYml().get(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_PERMANENT) != null) {
            xpConfig.getYml().addDefault(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_PERMANENT, 
                getYml().getBoolean(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_PERMANENT));
        }
        if (getYml().get(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_DOWNGRADABLE) != null) {
            xpConfig.getYml().addDefault(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_DOWNGRADABLE, 
                getYml().getBoolean(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_DOWNGRADABLE));
        }
        if (getYml().get(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_UNBREAKABLE) != null) {
            xpConfig.getYml().addDefault(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_UNBREAKABLE, 
                getYml().getBoolean(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_IS_UNBREAKABLE));
        }
        if (getYml().get(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_WEIGHT) != null) {
            xpConfig.getYml().addDefault(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_WEIGHT, 
                getYml().getInt(contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_WEIGHT));
        }

        // 复制层级设置，将货币改为 xp
        String tiersPath = contentPath + "." + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_TIERS;
        if (getYml().getConfigurationSection(tiersPath) != null) {
            for (String tierKey : getYml().getConfigurationSection(tiersPath).getKeys(false)) {
                String tierPath = tiersPath + "." + tierKey;
                copyTierContent(xpConfig, tierPath);
            }
        }
    }

    /**
     * 复制层级内容，将货币改为 xp
     */
    private void copyTierContent(ConfigManager xpConfig, String tierPath) {
        // 复制物品设置
        if (getYml().get(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_MATERIAL) != null) {
            xpConfig.getYml().addDefault(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_MATERIAL, 
                getYml().getString(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_MATERIAL));
        }
        if (getYml().get(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_DATA) != null) {
            xpConfig.getYml().addDefault(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_DATA, 
                getYml().getInt(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_DATA));
        }
        if (getYml().get(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_AMOUNT) != null) {
            xpConfig.getYml().addDefault(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_AMOUNT, 
                getYml().getInt(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_AMOUNT));
        }
        if (getYml().get(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_ENCHANTED) != null) {
            xpConfig.getYml().addDefault(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_ENCHANTED, 
                getYml().getBoolean(tierPath + ConfigPath.SHOP_CONTENT_TIER_ITEM_ENCHANTED));
        }

        // 复制价格设置，货币改为 xp
        if (getYml().get(tierPath + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_COST) != null) {
            xpConfig.getYml().addDefault(tierPath + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_COST, 
                getYml().getInt(tierPath + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_COST));
        }
        // 货币固定为 xp
        xpConfig.getYml().addDefault(tierPath + ConfigPath.SHOP_CONTENT_TIER_SETTINGS_CURRENCY, "xp");

        // 复制购买物品
        String buyItemsPath = tierPath + "." + ConfigPath.SHOP_CONTENT_BUY_ITEMS_PATH;
        if (getYml().getConfigurationSection(buyItemsPath) != null) {
            for (String itemKey : getYml().getConfigurationSection(buyItemsPath).getKeys(false)) {
                String itemPath = buyItemsPath + "." + itemKey;
                copyBuyItem(xpConfig, itemPath);
            }
        }
    }

    /**
     * 复制购买物品设置
     */
    private void copyBuyItem(ConfigManager xpConfig, String itemPath) {
        if (getYml().get(itemPath + ".material") != null) {
            xpConfig.getYml().addDefault(itemPath + ".material", 
                getYml().getString(itemPath + ".material"));
        }
        if (getYml().get(itemPath + ".data") != null) {
            xpConfig.getYml().addDefault(itemPath + ".data", 
                getYml().getInt(itemPath + ".data"));
        }
        if (getYml().get(itemPath + ".amount") != null) {
            xpConfig.getYml().addDefault(itemPath + ".amount", 
                getYml().getInt(itemPath + ".amount"));
        }
        if (getYml().get(itemPath + ".enchants") != null) {
            xpConfig.getYml().addDefault(itemPath + ".enchants", 
                getYml().getString(itemPath + ".enchants"));
        }
        if (getYml().get(itemPath + ".potion") != null) {
            xpConfig.getYml().addDefault(itemPath + ".potion", 
                getYml().getString(itemPath + ".potion"));
        }
        if (getYml().get(itemPath + ".auto-equip") != null) {
            xpConfig.getYml().addDefault(itemPath + ".auto-equip", 
                getYml().getBoolean(itemPath + ".auto-equip"));
        }
        if (getYml().get(itemPath + ".name") != null) {
            xpConfig.getYml().addDefault(itemPath + ".name", 
                getYml().getString(itemPath + ".name"));
        }
        if (getYml().get(itemPath + ".potion-color") != null) {
            xpConfig.getYml().addDefault(itemPath + ".potion-color", 
                getYml().getString(itemPath + ".potion-color"));
        }
    }

    /**
     * 根据游戏模式获取正确的商店
     * @param player 玩家
     * @return 商店索引
     */
    public static ShopIndex getShopForPlayer(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena != null && arena.getGameType() == GameType.XP) {
            return shopXp != null && !shopXp.getCategoryList().isEmpty() ? shopXp : shop;
        }
        return shop;
    }
    
    /**
     * 添加特殊物品到经验模式商店（下界合金套、金剑、下界合金剑）
     */
    private void addXpShopSpecialItems(ConfigManager xpConfig) {
        // 添加下界合金套装（全套）
        addNetheriteArmor(xpConfig);
        
        // 添加金剑（石中剑 EX）
        addGoldenSword(xpConfig);
        
        // 添加下界合金剑
        addNetheriteSword(xpConfig);
    }
    
    /**
     * 添加下界合金套装
     */
    private void addNetheriteArmor(ConfigManager xpConfig) {
        // 在护甲分类中添加下界合金套装
        String basePath = "armor-category.category-content.netherite-armor";
        
        // 检查Material是否支持下界合金
        Material netheriteMaterial = Material.matchMaterial("NETHERITE_HELMET");
        if (netheriteMaterial == null) {
            BedWars.plugin.getLogger().info("Netherite materials not supported in this version, skipping netherite armor.");
            return;
        }
        
        // 头盔
        xpConfig.getYml().addDefault(basePath + ".content-settings.content-slot", 14);
        xpConfig.getYml().addDefault(basePath + ".content-settings.is-permanent", true);
        xpConfig.getYml().addDefault(basePath + ".content-settings.is-downgradable", true);
        
        // 层级1
        String tierPath = basePath + ".content-tiers.tier1";
        xpConfig.getYml().addDefault(tierPath + ".tier-item.material", "NETHERITE_HELMET");
        xpConfig.getYml().addDefault(tierPath + ".tier-item.data", 0);
        xpConfig.getYml().addDefault(tierPath + ".tier-item.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".tier-item.enchanted", false);
        xpConfig.getYml().addDefault(tierPath + ".tier-settings.cost", 2000);
        xpConfig.getYml().addDefault(tierPath + ".tier-settings.currency", "xp");
        
        // 购买物品（全套）
        xpConfig.getYml().addDefault(tierPath + ".buy-items.helmet.material", "NETHERITE_HELMET");
        xpConfig.getYml().addDefault(tierPath + ".buy-items.helmet.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".buy-items.helmet.auto-equip", true);
        
        xpConfig.getYml().addDefault(tierPath + ".buy-items.chestplate.material", "NETHERITE_CHESTPLATE");
        xpConfig.getYml().addDefault(tierPath + ".buy-items.chestplate.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".buy-items.chestplate.auto-equip", true);
        
        xpConfig.getYml().addDefault(tierPath + ".buy-items.leggings.material", "NETHERITE_LEGGINGS");
        xpConfig.getYml().addDefault(tierPath + ".buy-items.leggings.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".buy-items.leggings.auto-equip", true);
        
        xpConfig.getYml().addDefault(tierPath + ".buy-items.boots.material", "NETHERITE_BOOTS");
        xpConfig.getYml().addDefault(tierPath + ".buy-items.boots.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".buy-items.boots.auto-equip", true);
    }
    
    /**
     * 添加金剑（石中剑 EX）
     */
    private void addGoldenSword(ConfigManager xpConfig) {
        String basePath = "melee-category.category-content.golden-sword-ex";
        
        xpConfig.getYml().addDefault(basePath + ".content-settings.content-slot", 20);
        xpConfig.getYml().addDefault(basePath + ".content-settings.is-permanent", false);
        xpConfig.getYml().addDefault(basePath + ".content-settings.is-downgradable", false);
        
        String tierPath = basePath + ".content-tiers.tier1";
        xpConfig.getYml().addDefault(tierPath + ".tier-item.material", "GOLDEN_SWORD");
        xpConfig.getYml().addDefault(tierPath + ".tier-item.data", 0);
        xpConfig.getYml().addDefault(tierPath + ".tier-item.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".tier-item.enchanted", true);
        xpConfig.getYml().addDefault(tierPath + ".tier-settings.cost", 700);
        xpConfig.getYml().addDefault(tierPath + ".tier-settings.currency", "xp");
        
        xpConfig.getYml().addDefault(tierPath + ".buy-items.sword.material", "GOLDEN_SWORD");
        xpConfig.getYml().addDefault(tierPath + ".buy-items.sword.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".buy-items.sword.enchants", "DAMAGE_ALL 4");
    }
    
    /**
     * 添加下界合金剑
     */
    private void addNetheriteSword(ConfigManager xpConfig) {
        Material netheriteMaterial = Material.matchMaterial("NETHERITE_SWORD");
        if (netheriteMaterial == null) {
            BedWars.plugin.getLogger().info("Netherite sword not supported in this version, skipping.");
            return;
        }
        
        String basePath = "melee-category.category-content.netherite-sword";
        
        xpConfig.getYml().addDefault(basePath + ".content-settings.content-slot", 21);
        xpConfig.getYml().addDefault(basePath + ".content-settings.is-permanent", false);
        xpConfig.getYml().addDefault(basePath + ".content-settings.is-downgradable", false);
        
        String tierPath = basePath + ".content-tiers.tier1";
        xpConfig.getYml().addDefault(tierPath + ".tier-item.material", "NETHERITE_SWORD");
        xpConfig.getYml().addDefault(tierPath + ".tier-item.data", 0);
        xpConfig.getYml().addDefault(tierPath + ".tier-item.amount", 1);
        xpConfig.getYml().addDefault(tierPath + ".tier-item.enchanted", false);
        xpConfig.getYml().addDefault(tierPath + ".tier-settings.cost", 500);
        xpConfig.getYml().addDefault(tierPath + ".tier-settings.currency", "xp");
        
        xpConfig.getYml().addDefault(tierPath + ".buy-items.sword.material", "NETHERITE_SWORD");
        xpConfig.getYml().addDefault(tierPath + ".buy-items.sword.amount", 1);
    }
}
