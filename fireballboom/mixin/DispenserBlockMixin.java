/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2315
 *  net.minecraft.class_2342
 *  net.minecraft.class_2347
 *  org.jetbrains.annotations.NotNull
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package name.fireballboom.mixin;

import java.util.Map;
import name.fireballboom.FireballBoom;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2315;
import net.minecraft.class_2342;
import net.minecraft.class_2347;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={class_2315.class})
public class DispenserBlockMixin {
    @Redirect(method={"registerBehavior"}, at=@At(value="INVOKE", target="Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static <K, V> V changeDispenserFireballBehavior(Map instance, K k, V v) {
        class_1935 itemLike = (class_1935)k;
        if (itemLike == class_1802.field_8814) {
            instance.put(itemLike.method_8389(), new class_2347(){

                @NotNull
                public class_1799 method_10135(class_2342 blockPointer, class_1799 itemStack) {
                    FireballBoom.summonFireballFromDispenser(blockPointer, itemStack);
                    return itemStack;
                }

                protected void method_10136(class_2342 blockPointer) {
                    blockPointer.method_10207().method_20290(1018, blockPointer.method_10122(), 0);
                }
            });
        } else {
            instance.put(itemLike.method_8389(), v);
        }
        return null;
    }
}

