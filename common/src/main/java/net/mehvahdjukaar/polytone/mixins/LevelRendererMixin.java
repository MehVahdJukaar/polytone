package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleLightCache;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPass;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1300)
public class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyExpressionValue(method = "renderClouds",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"))
    private float polytone$modifyCloudHeight(float original) {
        Float f = Polytone.DIMENSION_MODIFIERS.modifyCloudHeight(this.level);
        return f != null ? f : original;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void polytone$trackLevelRenderPass(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                               Camera camera, GameRenderer gameRenderer,
                                               net.minecraft.client.renderer.LightTexture lightTexture,
                                               org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix,
                                               CallbackInfo ci) {
        LevelRenderPass.push();
    }

    // before GameRenderer clears depth for first-person hand rendering
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void polytone$captureLevelDepth(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                            Camera camera, GameRenderer gameRenderer,
                                            net.minecraft.client.renderer.LightTexture lightTexture,
                                            org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix,
                                            CallbackInfo ci) {
        // Only for the pass that reaches the screen. A mirror or TV feed rendering the world into its
        // own canvas gets here too, and nothing it draws samples either of these (see LevelRenderPass).
        if (!LevelRenderPass.popAndWasMain()) return;

        Polytone.POST_SHADERS.captureLevelDepthSnapshot();
        // Render the directional shadow map here too: the visible-section list and compiled chunk
        // VBOs are still current, and we're before GameRenderer clears depth for the first-person hand.
        Polytone.SHADOWS.renderer().renderShadowPassIfNeeded(camera, frustumMatrix, projectionMatrix);
    }

    // Join the async particle tick batch before anything in the frame reads particle state
    // (particles render inside renderLevel on both loaders).
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void polytone$joinAsyncParticles(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                             Camera camera, GameRenderer gameRenderer,
                                             net.minecraft.client.renderer.LightTexture lightTexture,
                                             org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix,
                                             CallbackInfo ci) {
        PolytoneAsyncParticles.awaitTicks();
    }

    // Every section rebuild (block or light change) funnels through setSectionDirty; bump that
    // section's light-cache version so particles there re-sample. Section coords come in directly.
    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void polytone$invalidateParticleLight(int x, int y, int z, boolean important, CallbackInfo ci) {
        ParticleLightCache.markSectionDirty(x, y, z);
    }

    //TODO: add
    /*
    @ModifyArg(method = "renderLevel",
            at = @At(value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/client/renderer/FogRenderer;setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;Lorg/joml/Vector4f;FZF)Lnet/minecraft/client/renderer/FogParameters;"))
    private Vector4f polytone$modifyTerrainFogColor(Vector4f original, @Local(argsOnly = true) Camera camera,
                                                    @Local(ordinal = 1) float partialTicks,
                                                    @Local(argsOnly = true) GameRenderer gameRenderer) {

        return Polytone.DIMENSION_MODIFIERS.modifyTerrainFogColor(original, this.level,
                camera, partialTicks, gameRenderer, this.minecraft);
    }*/
}
