package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DimensionSpecialEffects.OverworldEffects.class)
public class DimensionSpecialEffectsMixin {


    @ModifyReturnValue(method = "getSunriseOrSunsetColor", at = @At("RETURN"))
    private int polytone$modifySunsetColor(int original) {
        if (original == 0) return 0;
        var c = Polytone.DIMENSION_MODIFIERS.modifySunsetColor(original);
        if (c != 0) {
            return c;
        }
        return original;
    }
}
