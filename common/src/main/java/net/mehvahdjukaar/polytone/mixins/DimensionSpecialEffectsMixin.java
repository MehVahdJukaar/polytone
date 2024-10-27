package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DimensionSpecialEffects.class)
public class DimensionSpecialEffectsMixin {


    @ModifyReturnValue(method = "getSunriseColor", at = @At("RETURN"))
    private float[] polytone$modifySunsetColor(float[] original) {
        if (original == null) return null;
        var c = Polytone.DIMENSION_MODIFIERS.modifySunsetColor(original);
        if (c != null) {
            return c;
        }
        return original;
    }
}
