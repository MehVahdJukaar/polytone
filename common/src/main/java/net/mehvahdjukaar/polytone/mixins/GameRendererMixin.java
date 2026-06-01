package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.DeltaTracker;
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
    @Final
    private LightTexture lightTexture;

    @Inject(method = "render", at = @At(value = "NEW",
            target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Lnet/minecraft/client/gui/GuiGraphics;"))
    private void polytone$messWithGui(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(true);
        lightTexture.turnOnLightLayer();
        Polytone.OVERLAY_MODIFIERS.onStartRenderingOverlay();
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void polytone$resetGuiLightmap(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(false);
        lightTexture.turnOnLightLayer();
        Polytone.OVERLAY_MODIFIERS.onEndRenderingOverlay();
    }

    // Run Polytone post-shader chains on top of vanilla's post effect.
    // Targets the first RenderTarget#bindWrite invoke in render(), which fires right after
    // the optional vanilla postEffect.process() and before HUD rendering.
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V",
                     ordinal = 0))
    private void polytone$renderPolytonePostEffects(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.POST_SHADERS.renderAfterMainPostEffect(deltaTracker.getGameTimeDeltaTicks());
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void polytone$resizePostShaders(int width, int height, CallbackInfo ci) {
        Polytone.POST_SHADERS.resize(width, height);
    }

}
