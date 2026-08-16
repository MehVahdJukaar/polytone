package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderTarget;
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
}
