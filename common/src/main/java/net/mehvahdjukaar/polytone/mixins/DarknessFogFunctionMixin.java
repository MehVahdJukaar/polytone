package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FogRenderer.DarknessFogFunction.class)
public class DarknessFogFunctionMixin {

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 15.0F))
    private float polytone$fogDistance(float original) {
        return Polytone.COLORS.getDarknessFogDistance(original);
    }
}
