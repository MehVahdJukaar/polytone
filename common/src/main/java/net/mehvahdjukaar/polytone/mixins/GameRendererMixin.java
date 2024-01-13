package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.lightmap.LightmapsManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow public abstract LightTexture lightTexture();

    @Shadow @Final private LightTexture lightTexture;

    @Inject(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V",
    shift = At.Shift.BEFORE))
    private   void polytone$messWithGui(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci){
        LightmapsManager.setupForGUI(true);
        lightTexture.updateLightTexture = true;
        lightTexture.updateLightTexture(partialTicks);
        LightmapsManager.setupForGUI(false);
    }
}
