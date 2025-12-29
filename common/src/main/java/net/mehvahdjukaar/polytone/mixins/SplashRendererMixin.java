package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SplashRenderer.class)
public class SplashRendererMixin {


    // TODO(dannyb): This is now in the color style field
//    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;color(FI)I"))
    public int polytone$changeSplashColor(int original) {

        var newCol = Polytone.COLORS.getSplash();
        if (newCol != null) {
            return newCol;
        }
        return original;
    }
}
