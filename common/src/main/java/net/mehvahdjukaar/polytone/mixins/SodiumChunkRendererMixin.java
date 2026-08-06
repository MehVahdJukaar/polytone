package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the terrain atlas {@code GpuSampler} Sodium was handed, to feed back into the shadow-map
 * replay's {@code drawChunkLayer} (see {@link SodiumShadowRenderer}).
 *
 * <p>Since Sodium 0.9.2 terrain is drawn through Mojang's {@code RenderPass}/{@code RenderPipeline}
 * instead of Sodium's own {@code GlProgram}, so our expression-uniform UBOs reach chunk shaders
 * through {@link RenderPassMixin} like every other draw - no raw-GL uniform-block binding needed
 * here anymore, and neither is "Sodium Core Shader Support" for that path. The shadow-pass
 * framebuffer swap moved to {@link SodiumDefaultChunkRendererMixin} for the same reason: {@code begin}
 * no longer binds anything, the attachments are chosen when the render pass is created.
 *
 * <p>{@code require = 0}: this targets a Sodium internal that may change across versions; if the
 * method isn't found we silently no-op rather than crash.
 */
@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class SodiumChunkRendererMixin {

    @Inject(method = "begin", at = @At("TAIL"), require = 0)
    private void polytone$captureTerrainSampler(TerrainRenderPass pass, FogParameters parameters,
                                                GpuSampler terrainSampler, CallbackInfo ci) {
        SodiumShadowRenderer.captureTerrainSampler(terrainSampler);
    }
}
