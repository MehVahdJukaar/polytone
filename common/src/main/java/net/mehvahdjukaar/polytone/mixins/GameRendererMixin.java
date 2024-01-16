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

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {

    @Shadow
    public abstract LightTexture lightTexture();

    @Shadow
    @Final
    private LightTexture lightTexture;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemActivationAnimation(IIF)V",
    shift = At.Shift.BEFORE))
    private void polytone$messWithGui(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        LightmapsManager.setupForGUI(true);
        lightTexture.turnOnLightLayer();
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void polytone$resetGuiLightmap(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        LightmapsManager.setupForGUI(false);
        lightTexture.turnOnLightLayer();
    }

}
