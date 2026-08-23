/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory
 *  net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry
 *  net.minecraft.class_1268
 *  net.minecraft.class_1297
 *  net.minecraft.class_1299
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_1674
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1928$class_4312
 *  net.minecraft.class_1928$class_4313
 *  net.minecraft.class_1928$class_4314
 *  net.minecraft.class_1928$class_5198
 *  net.minecraft.class_1937
 *  net.minecraft.class_2315
 *  net.minecraft.class_2342
 *  net.minecraft.class_2350
 *  net.minecraft.class_2374
 *  net.minecraft.class_2382
 *  net.minecraft.class_243
 *  net.minecraft.class_2769
 *  net.minecraft.class_3218
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package name.fireballboom;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1674;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1928;
import net.minecraft.class_1937;
import net.minecraft.class_2315;
import net.minecraft.class_2342;
import net.minecraft.class_2350;
import net.minecraft.class_2374;
import net.minecraft.class_2382;
import net.minecraft.class_243;
import net.minecraft.class_2769;
import net.minecraft.class_3218;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FireballBoom
implements ModInitializer {
    public static Logger logger = LoggerFactory.getLogger((String)"fireball-boom");
    public static class_1928.class_4313<class_1928.class_4312> FIREBALL_THROW_COOLDOWN;

    public static void summonFireballFromPlayer(class_1657 player, class_1268 interactionHand) {
        FireballBoom.cooldownFireballItem(player);
        class_1799 stack = player.method_5998(interactionHand);
        if (stack.method_31574(class_1802.field_8814) && stack.method_7947() > 0) {
            class_1937 level = player.method_37908();
            class_243 look = player.method_5720();
            class_243 spawnPos = player.method_33571();
            class_1674 fireball = new class_1674(level, (class_1309)player, look.field_1352, look.field_1351, look.field_1350, 6);
            fireball.method_23327(spawnPos.field_1352, spawnPos.field_1351, spawnPos.field_1350);
            level.method_8649((class_1297)fireball);
            stack.method_7934(1);
        }
    }

    public static void summonFireballFromDispenser(class_2342 blockSource, class_1799 itemStack) {
        if (!itemStack.method_31574(class_1802.field_8814)) {
            return;
        }
        class_3218 level = blockSource.method_10207();
        class_2350 direction = (class_2350)blockSource.method_10120().method_11654((class_2769)class_2315.field_10918);
        class_2374 spawnPos = class_2315.method_10010((class_2342)blockSource);
        class_1674 fireball = new class_1674(class_1299.field_6066, (class_1937)level);
        fireball.method_7432((class_1297)fireball);
        class_2382 speed = direction.method_10163();
        fireball.field_7601 = (double)speed.method_10263() * 0.1;
        fireball.field_7600 = (double)speed.method_10264() * 0.1;
        fireball.field_7599 = (double)speed.method_10260() * 0.1;
        fireball.method_5814(spawnPos.method_10216(), spawnPos.method_10214(), spawnPos.method_10215());
        fireball.method_36456(direction.method_10144());
        fireball.method_36457((float)(direction.method_10148() * -90));
        level.method_8649((class_1297)fireball);
        itemStack.method_7934(1);
    }

    public static void cooldownFireballItem(class_1657 player) {
        int ticks = player.method_37908().method_8450().method_8356(FIREBALL_THROW_COOLDOWN);
        if (ticks <= 0) {
            return;
        }
        player.method_7357().method_7906(class_1802.field_8814, ticks);
    }

    public void onInitialize() {
        FIREBALL_THROW_COOLDOWN = GameRuleRegistry.register((String)"fireballThrowCooldown", (class_1928.class_5198)class_1928.class_5198.field_24094, (class_1928.class_4314)GameRuleFactory.createIntRule((int)4));
    }
}

