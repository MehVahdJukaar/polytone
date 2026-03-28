package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/world/level/biome/BiomeSpecialEffects$GrassColorModifier$2")
public class DarkOakColorMixin {

    @Inject(method = "modifyColor", at = @At("HEAD"), cancellable = true)
    public void poly$modifyColor(double d, double e, int i, CallbackInfoReturnable<Integer> cir) {
        Integer a = Polytone.COLORS.getSpecialSwampDark();
        if (a != null) {
            cir.setReturnValue(ColorUtils.blendColor(i, a));
        }
    }
}
