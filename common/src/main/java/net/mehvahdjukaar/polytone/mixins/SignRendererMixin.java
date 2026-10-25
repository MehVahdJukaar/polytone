package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SignRenderer.class, priority = 1100)
public abstract class SignRendererMixin {

    //Black is the only dye with a hardcoded glow color
    @ModifyExpressionValue(method = "getDarkColor", at = @At(value = "CONSTANT", args = "intValue=-988212"))
    private static int polytone$blackSignGlow(int original) {
        Integer custom = Polytone.COLORS.getBlackSignGlow();
        return custom != null ? custom : original;
    }
}
