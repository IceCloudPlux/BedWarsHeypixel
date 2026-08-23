/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1668
 *  net.minecraft.class_1674
 *  net.minecraft.class_243
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package name.fireballboom.mixin;

import net.minecraft.class_1668;
import net.minecraft.class_1674;
import net.minecraft.class_243;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1668.class})
public class ExplosiveProjectileEntityMixin {
    @Shadow
    public double field_7601;
    @Shadow
    public double field_7600;
    @Shadow
    public double field_7599;

    @Unique
    class_243 getConstSpeed() {
        return new class_243(this.field_7601, this.field_7600, this.field_7599).method_1021(15.0);
    }

    @Unique
    private static boolean isFireball(Object instance) {
        return instance instanceof class_1674;
    }

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    void resetSpeed(CallbackInfo ci) {
        if (!ExplosiveProjectileEntityMixin.isFireball(this)) {
            return;
        }
        ((class_1668)this).method_18799(this.getConstSpeed());
    }

    @Redirect(method={"tick"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/projectile/ExplosiveProjectileEntity;setPosition(DDD)V"))
    void resetPosition(class_1668 instance, double x, double y, double z) {
        if (!ExplosiveProjectileEntityMixin.isFireball(instance)) {
            instance.method_5814(x, y, z);
            return;
        }
        class_243 pos = instance.method_19538();
        class_243 speed = this.getConstSpeed();
        instance.method_18799(speed);
        instance.method_33574(pos.method_1019(speed));
    }
}

