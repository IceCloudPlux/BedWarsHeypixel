/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.Unpooled
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1674
 *  net.minecraft.class_1927
 *  net.minecraft.class_1937
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_2540
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_3532
 *  net.minecraft.class_5712
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package name.fireballboom.mixin;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1674;
import net.minecraft.class_1927;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3532;
import net.minecraft.class_5712;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1927.class})
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private Map<class_1657, class_243> field_9194;
    @Unique
    double horizontalDistance;
    @Unique
    double verticalDistance;
    @Unique
    double distance;

    @Accessor(value="x")
    public abstract double x();

    @Accessor(value="y")
    public abstract double y();

    @Accessor(value="z")
    public abstract double z();

    @Accessor(value="world")
    public abstract class_1937 world();

    @Accessor(value="entity")
    public abstract class_1297 entity();

    @Unique
    double horizontalKb() {
        if (this.horizontalDistance > 6.0) {
            return 0.0;
        }
        if (this.verticalDistance < 0.7 && this.horizontalDistance < 1.0) {
            return 0.0;
        }
        if (this.horizontalDistance < 3.0) {
            return this.horizontalDistance * 0.35;
        }
        return 1.0499999999999998;
    }

    @Unique
    double verticalKb() {
        if (this.horizontalDistance > 6.0) {
            return 0.0;
        }
        return 1.27;
    }

    @Unique
    double jumpingHorizontalKbScale(double originSpeed) {
        return 2.0 * originSpeed;
    }

    @Unique
    double playerAccelerationScale(double originSpeed) {
        if (originSpeed < 0.2) {
            return 10.0 * originSpeed;
        }
        if (originSpeed < 3.6) {
            return 1.8 + originSpeed;
        }
        return 1.5 * originSpeed;
    }

    @Inject(method={"collectBlocksAndDamageEntities"}, at={@At(value="HEAD")}, cancellable=true)
    void changeExplosionKnockBack(CallbackInfo ci) {
        if (!(this.entity() instanceof class_1674)) {
            return;
        }
        class_243 explosionPos = new class_243(this.x(), this.y(), this.z());
        this.world().method_43275(this.entity(), class_5712.field_28178, explosionPos);
        float radius = 7.0f;
        int minX = class_3532.method_15357((double)(this.x() - (double)radius - 1.0));
        int maxX = class_3532.method_15357((double)(this.x() + (double)radius + 1.0));
        int minY = class_3532.method_15357((double)(this.y() - (double)radius - 1.0));
        int maxY = class_3532.method_15357((double)(this.y() + (double)radius + 1.0));
        int minZ = class_3532.method_15357((double)(this.z() - (double)radius - 1.0));
        int maxZ = class_3532.method_15357((double)(this.z() + (double)radius + 1.0));
        List list = this.world().method_8335(this.entity(), new class_238((double)minX, (double)minY, (double)minZ, (double)maxX, (double)maxY, (double)maxZ));
        for (int v = 0; v < list.size(); ++v) {
            class_3222 player;
            class_1297 instance = (class_1297)list.get(v);
            if (instance.method_5659() || instance instanceof class_1674) continue;
            class_243 playerPos = instance.method_19538().method_1031(0.0, 1.0, 0.0);
            class_243 diff = playerPos.method_1019(explosionPos.method_1021(-1.0));
            class_243 originSpeed = instance.method_18798();
            this.horizontalDistance = diff.method_37267();
            this.verticalDistance = Math.abs(diff.field_1351);
            this.distance = diff.method_1033();
            if (this.horizontalDistance > 6.0) continue;
            double hKb = this.horizontalKb();
            double yKb = this.verticalKb();
            if (!instance.method_24828()) {
                hKb = this.jumpingHorizontalKbScale(hKb);
            }
            double xKb = 0.0;
            double zKb = 0.0;
            if (this.horizontalDistance != 0.0) {
                xKb = diff.field_1352 / this.horizontalDistance * hKb;
                zKb = diff.field_1350 / this.horizontalDistance * hKb;
            }
            if (instance instanceof class_1657) {
                xKb += this.playerAccelerationScale(Math.abs(originSpeed.field_1352)) * Math.signum(originSpeed.field_1352);
                zKb += this.playerAccelerationScale(Math.abs(originSpeed.field_1350)) * Math.signum(originSpeed.field_1350);
            }
            class_243 finalSpeed = new class_243(xKb, yKb, zKb);
            class_243 knockBack = finalSpeed.method_1019(originSpeed.method_1021(-1.0));
            class_243 result = originSpeed.method_1019(knockBack);
            instance.method_18799(result);
            if (instance instanceof class_3222 && !(player = (class_3222)instance).method_7325()) {
                ServerPlayNetworking.send((class_3222)player, (class_2960)new class_2960("fireball-boom", "fireball_play_hurt_animation"), (class_2540)new class_2540(Unpooled.buffer()));
            }
            if (!(instance instanceof class_1657) || (player = (class_1657)instance).method_7325() || player.method_7337() && player.method_31549().field_7479) continue;
            this.field_9194.put((class_1657)player, knockBack);
        }
        ci.cancel();
    }
}

