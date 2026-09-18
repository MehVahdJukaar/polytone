package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlindnessFogEnvironment.class)
public class BlindnessFogEnvironmentMixin {

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 5.0F))
    private float polytone$fogDistance(float original) {
        return Polytone.COLORS.getBlindnessFogDistance(original);
    }
}
