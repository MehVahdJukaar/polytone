package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.DarknessFogFunction.class)
public class DarknessFogFunctionMixin {

    @ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=15.0F"))
    private float polytone$fogDistance(float original) {
        return Polytone.COLORS.getDarknessFogDistance(original);
    }
}
