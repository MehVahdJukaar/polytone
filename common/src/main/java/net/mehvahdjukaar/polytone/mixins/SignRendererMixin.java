package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {

    //Black is the only dye with a hardcoded glow color
    @ModifyConstant(method = "getDarkColor", constant = @Constant(intValue = -988212))
    private static int polytone$blackSignGlow(int original) {
        Integer custom = Polytone.COLORS.getBlackSignGlow();
        return custom != null ? custom : original;
    }
}
