/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_2960
 */
package name.fireballboom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.class_2960;

@Environment(value=EnvType.CLIENT)
public class PacketReceiver
implements ClientModInitializer {
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver((class_2960)new class_2960("fireball-boom", "fireball_play_hurt_animation"), (mc, var2, var3, var4) -> {
            if (mc.field_1724 != null) {
                mc.field_1724.field_6254 = 10;
                mc.field_1724.field_6235 = 10;
            }
        });
    }
}

