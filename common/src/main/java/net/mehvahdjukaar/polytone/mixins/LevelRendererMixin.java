package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void preRender(GraphicsResourceAllocator graphicsResourceAllocator,
                          DeltaTracker deltaTracker,
                          boolean bl,
                          Camera camera,
                          Matrix4f modelView,
                          Matrix4f project,
                          Matrix4f matrix4f3,
                          GpuBufferSlice gpuBufferSlice,
                          Vector4f vector4f,
                          boolean bl2, CallbackInfo ci) {
        Polytone.POST_SHADERS.onRenderLevel(project, modelView);
    }
}
