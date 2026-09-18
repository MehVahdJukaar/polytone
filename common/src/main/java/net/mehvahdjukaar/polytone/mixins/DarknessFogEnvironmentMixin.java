package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DarknessFogEnvironment.class)
public class DarknessFogEnvironmentMixin {

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 15.0F))
    private float polytone$fogDistance(float original) {
        return Polytone.COLORS.getDarknessFogDistance(original);
    }
}
