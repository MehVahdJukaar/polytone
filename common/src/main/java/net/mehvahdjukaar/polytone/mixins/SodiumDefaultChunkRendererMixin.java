package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// swaps the shadow map attachments in while a shadow replay is running
@Pseudo
@Mixin(DefaultChunkRenderer.class)
public abstract class SodiumDefaultChunkRendererMixin {

    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;getColorTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private GpuTextureView polytone$shadowColorAttachment(RenderTarget target) {
        GpuTextureView shadow = SodiumShadowRenderer.activeShadowColorView();
        return shadow != null ? shadow : target.getColorTextureView();
    }

    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;getDepthTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private GpuTextureView polytone$shadowDepthAttachment(RenderTarget target) {
        GpuTextureView shadow = SodiumShadowRenderer.activeShadowDepthView();
        return shadow != null ? shadow : target.getDepthTextureView();
    }

    // the default blocks every vanilla pass gets (Globals, Fog...), sodium only binds its own
    @WrapOperation(method = "render", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private void polytone$bindDefaultUniforms(RenderPass pass, RenderPipeline pipeline, Operation<Void> original) {
        original.call(pass, pipeline);
        RenderSystem.bindDefaultUniforms(pass);
    }
}
