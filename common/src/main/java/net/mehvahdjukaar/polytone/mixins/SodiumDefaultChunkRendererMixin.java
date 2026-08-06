package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Points Sodium's terrain draw at the shadow map while a shadow replay is in flight.
 *
 * <p>Sodium 0.9.2 opens a Mojang {@code RenderPass} over {@code TerrainRenderPass.getTarget()}'s
 * color and depth views instead of binding a framebuffer itself, so the redirect happens on those
 * two lookups. Outside a replay both return the real target untouched.
 *
 * <p>{@code require = 0}: Sodium internal, may move across versions - no-op rather than crash.
 */
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
