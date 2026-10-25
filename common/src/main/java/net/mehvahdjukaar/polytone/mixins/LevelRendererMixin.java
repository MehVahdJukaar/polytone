package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.particle.PreviewRenderTarget;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneRenderHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    public LevelRenderState levelRenderState;

    @ModifyArg(method = "submitBlockOutline",
            at = @At(value = "INVOKE", ordinal = 1,
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V"),
            index = 4)
    private int polytone$changeOutlineColor(int color) {
        Integer c = Polytone.COLORS.getBlockOutlineColor();
        if (c == null) return color;
        float[] col = ColorUtils.unpackWithAlpha(c, ARGB.alphaFloat(color));
        return ARGB.colorFromFloat(col[3], col[0], col[1], col[2]);
    }

    @Inject(method = "particlesTarget", at = @At("HEAD"), cancellable = true)
    private void poly$redirectParticlesTargetForPreview(CallbackInfoReturnable<RenderTarget> cir) {
        if (CompatHandler.NAUTILUS && PreviewRenderTarget.current() != null) cir.setReturnValue(null);
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void poly$preRender(GraphicsResourceAllocator resourceAllocator,
                               DeltaTracker deltaTracker,
                               boolean renderOutline,
                               CameraRenderState cameraState,
                               Matrix4fc modelViewMatrix,
                               GpuBufferSlice terrainFog,
                               Vector4f fogColor,
                               boolean shouldRenderSky,
                               CallbackInfo ci) {
        Polytone.SHADER_UNIFORMS.updateAll();
        if (!LevelRenderPassTracker.pushAndIsMain()) return;
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        Polytone.GLOBAL_UNIFORMS.capture(cameraState.projectionMatrix, modelViewMatrix, deltaTracker.getGameTimeDeltaTicks());
        Polytone.COLORED_LIGHTS.beforeRenderLevel(camera.position());
    }

    //the frame graph already ran here, no pass is open
    @Inject(method = "render", at = @At("RETURN"))
    public void poly$popLevelRenderPass(GraphicsResourceAllocator resourceAllocator,
                                        DeltaTracker deltaTracker,
                                        boolean renderOutline,
                                        CameraRenderState cameraState,
                                        Matrix4fc modelViewMatrix,
                                        GpuBufferSlice terrainFog,
                                        Vector4f fogColor,
                                        boolean shouldRenderSky,
                                        CallbackInfo ci) {
        if (!LevelRenderPassTracker.popAndWasMain()) return;
        PolytoneRenderHooks.afterLevelRender(Minecraft.getInstance().gameRenderer.mainCamera(), modelViewMatrix, cameraState.projectionMatrix);
    }

    // after weather, the last world pass that depth tests
    @Inject(method = "render", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addWeatherPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addWeatherPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4fc;)V",
                    shift = At.Shift.AFTER)})
    public void poly$addGpuParticlesPass(GraphicsResourceAllocator resourceAllocator,
                                         DeltaTracker deltaTracker,
                                         boolean renderOutline,
                                         CameraRenderState cameraState,
                                         Matrix4fc modelViewMatrix,
                                         GpuBufferSlice terrainFog,
                                         Vector4f fogColor,
                                         boolean shouldRenderSky,
                                         CallbackInfo ci,
                                         @Local FrameGraphBuilder frameGraphBuilder) {
        Polytone.CUSTOM_PARTICLES.gpuParticles.addRenderPass(frameGraphBuilder, this.targets, terrainFog,
                this.levelRenderState.cameraRenderState.pos, this.levelRenderState.gameTime,
                deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
            shift = At.Shift.BEFORE))
    public void poly$addPostShaders(GraphicsResourceAllocator resourceAllocator,
                                    DeltaTracker deltaTracker,
                                    boolean renderOutline,
                                    CameraRenderState cameraState,
                                    Matrix4fc modelViewMatrix,
                                    GpuBufferSlice terrainFog,
                                    Vector4f fogColor,
                                    boolean shouldRenderSky,
                                    CallbackInfo ci,
                                    @Local FrameGraphBuilder frameGraphBuilder) {
        if (Polytone.CONFIGS.postChainsAfterHand.get()) return;
        RenderTarget mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        Polytone.POST_CHAINS.addChainsToFrameGraph(mainTarget.width, mainTarget.height, this.targets, frameGraphBuilder,
                terrainFog, this.levelRenderState.cameraRenderState);
    }
}
