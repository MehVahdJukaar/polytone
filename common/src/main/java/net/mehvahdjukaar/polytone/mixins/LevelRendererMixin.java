package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleLightCache;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTrack;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticleHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1300)
public class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

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
        LevelRenderPassTrack.push();
    }

    // before GameRenderer clears depth for first-person hand rendering
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void polytone$captureLevelDepth(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                            Camera camera, GameRenderer gameRenderer,
                                            net.minecraft.client.renderer.LightTexture lightTexture,
                                            org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix,
                                            CallbackInfo ci) {
        Polytone.CUSTOM_PARTICLES.gpuParticles.render(camera, gameRenderer, lightTexture, frustumMatrix, projectionMatrix,
                deltaTracker.getGameTimeDeltaPartialTick(false));
        if (!LevelRenderPassTrack.popAndWasMain()) return;

        Polytone.POST_SHADERS.captureLevelDepthSnapshot();
        Polytone.SHADOWS.renderer().renderShadowPassIfNeeded(camera, frustumMatrix, projectionMatrix);
    }

    // Join the async particle tick batch
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void polytone$joinAsyncParticles(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                             Camera camera, GameRenderer gameRenderer,
                                             net.minecraft.client.renderer.LightTexture lightTexture,
                                             org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix,
                                             CallbackInfo ci) {
        PolytoneAsyncParticleHandler.awaitTicks();
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void polytone$invalidateParticleLight(int x, int y, int z, boolean important, CallbackInfo ci) {
        //rebuild light cache
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
