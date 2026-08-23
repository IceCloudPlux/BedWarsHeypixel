/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1269
 *  net.minecraft.class_1657
 *  net.minecraft.class_1778
 *  net.minecraft.class_1838
 *  net.minecraft.class_3222
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package name.fireballboom.mixin;

import name.fireballboom.FireballBoom;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1778;
import net.minecraft.class_1838;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1778.class})
public class FireChargeItemMixin {
    @Inject(at={@At(value="HEAD")}, method={"useOnBlock"}, cancellable=true)
    private void changeFireballUsage(class_1838 context, CallbackInfoReturnable<class_1269> cir) {
        class_1657 player = context.method_8036();
        if (player instanceof class_3222) {
            FireballBoom.summonFireballFromPlayer(player, context.method_20287());
        }
        cir.setReturnValue((Object)class_1269.method_29236((boolean)context.method_8045().field_9236));
    }
}

