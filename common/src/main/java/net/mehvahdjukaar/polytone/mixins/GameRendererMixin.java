package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
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

    // Run Polytone post-shader chains on top of vanilla's post effect, once the main target is bound
    // for compositing (after optional vanilla postEffect.process()).
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V",
                     ordinal = 0,
                     shift = At.Shift.AFTER))
    private void polytone$renderPolytonePostEffects(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.POST_SHADERS.renderAfterMainPostEffect(deltaTracker.getGameTimeDeltaTicks());
    }

    // Capture the level projection / camera matrices so polytone post shaders can expose them as the
    // PolyProjMat / PolyModelViewMat built-in uniforms. Ordinal 0 is the projection matrix (with bob &
    // confusion applied), ordinal 1 is the camera rotation (view) matrix - the same pair passed to
    // LevelRenderer.renderLevel right after this point.
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
    private void polytone$capturePostShaderMatrices(DeltaTracker deltaTracker, CallbackInfo ci,
                                                    @Local(ordinal = 0) Matrix4f projectionMatrix,
                                                    @Local(ordinal = 1) Matrix4f viewMatrix) {
        Polytone.POST_SHADERS.captureLevelMatrices(projectionMatrix, viewMatrix);
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void polytone$resizePostShaders(int width, int height, CallbackInfo ci) {
        Polytone.POST_SHADERS.resize(width, height);
    }

}
