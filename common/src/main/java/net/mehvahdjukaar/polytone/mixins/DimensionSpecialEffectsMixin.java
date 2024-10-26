package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DimensionSpecialEffects.class)
public class DimensionSpecialEffectsMixin {


    @ModifyReturnValue(method = "getSunriseColor", at = @At("RETURN"))
    private float[] polytone$modifySunsetColor(float[] original) {
        if (original == null) return null;
        var c = Polytone.DIMENSION_MODIFIERS.modifySunsetColor();
        if (c != null) {
            return new float[]{c[0], c[1], c[2], original[3]};
        }
        return original;
    }
}
