package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    public LevelRenderState levelRenderState;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void poly$preRender(GraphicsResourceAllocator graphicsResourceAllocator,
                          DeltaTracker deltaTracker,
                          boolean bl,
                          Camera camera,
                          Matrix4f modelView,
                          Matrix4f project,
                          Matrix4f matrix4f3,
                          GpuBufferSlice gpuBufferSlice,
                          Vector4f vector4f,
                          boolean bl2, CallbackInfo ci) {
        Polytone.POST_SHADERS.captureLevelRendererParams(project, modelView);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V",
    shift = At.Shift.BEFORE))
    public void poly$addPostShaders(GraphicsResourceAllocator graphicsResourceAllocator,
                                    DeltaTracker deltaTracker,
                                    boolean bl,
                                    Camera camera,
                                    Matrix4f modelView,
                                    Matrix4f project,
                                    Matrix4f matrix4f3,
                                    GpuBufferSlice gpuBufferSlice,
                                    Vector4f vector4f,
                                    boolean bl2, CallbackInfo ci,
                                    @Local FrameGraphBuilder frameGraphBuilder) {
        int i = this.minecraft.getMainRenderTarget().width;
        int j = this.minecraft.getMainRenderTarget().height;
        Polytone.POST_SHADERS.addPostPass(i, j, this.targets, frameGraphBuilder, gpuBufferSlice, this.levelRenderState.cameraRenderState);
    }
}
