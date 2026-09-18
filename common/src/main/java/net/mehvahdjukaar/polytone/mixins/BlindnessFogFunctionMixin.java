package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FogRenderer.BlindnessFogFunction.class)
public class BlindnessFogFunctionMixin {

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 5.0F))
    private float polytone$fogDistance(float original) {
        return Polytone.COLORS.getBlindnessFogDistance(original);
    }
}
