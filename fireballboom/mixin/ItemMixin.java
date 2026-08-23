/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1271
 *  net.minecraft.class_1657
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1937
 *  net.minecraft.class_3222
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package name.fireballboom.mixin;

import name.fireballboom.FireballBoom;
import net.minecraft.class_1268;
import net.minecraft.class_1271;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1792.class})
public class ItemMixin {
    @Inject(at={@At(value="HEAD")}, method={"use"}, cancellable=true)
    void useThrowFireball(class_1937 world, class_1657 user, class_1268 hand, CallbackInfoReturnable<class_1271<class_1799>> cir) {
        class_1799 stack = user.method_5998(hand);
        if (stack.method_31574(class_1802.field_8814)) {
            if (user instanceof class_3222) {
                FireballBoom.summonFireballFromPlayer(user, hand);
            }
            cir.setReturnValue((Object)class_1271.method_22428((Object)stack));
        }
    }
}

