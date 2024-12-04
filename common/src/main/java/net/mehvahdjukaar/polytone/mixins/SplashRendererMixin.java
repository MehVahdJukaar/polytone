package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashRenderer.class)
public class SplashRendererMixin {

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void aa(GuiGraphics guiGraphics, int screenWidth, Font font, int color, CallbackInfo ci,
                   @Local(ordinal = 1, argsOnly = true) LocalIntRef colorV) {

        var newCol = Polytone.COLORS.getSplash();
        if (newCol != null) {
            colorV.set(newCol);
        }
    }
}
