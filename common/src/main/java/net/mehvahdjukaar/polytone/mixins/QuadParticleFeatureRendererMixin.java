package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// particles pick their own target and ignore the output override, viewpoints need them to follow it
@Mixin(QuadParticleFeatureRenderer.class)
public abstract class QuadParticleFeatureRendererMixin {

    @ModifyArg(method = "executeGroup", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"),
            index = 1)
    private GpuTextureView polytone$useOutputColorOverride(GpuTextureView color) {
        GpuTextureView override = RenderSystem.outputColorTextureOverride;
        return override != null ? override : color;
    }

    @ModifyArg(method = "executeGroup", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"),
            index = 3)
    private GpuTextureView polytone$useOutputDepthOverride(GpuTextureView depth) {
        GpuTextureView override = RenderSystem.outputDepthTextureOverride;
        return override != null ? override : depth;
    }
}
