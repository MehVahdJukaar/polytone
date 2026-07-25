package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium renders terrain with its OWN {@code GlProgram<ChunkShaderInterface>} (compiled from
 * {@code sodium:blocks/block_layer_opaque}), binding its uniforms itself instead of going
 * through Mojang's {@code RenderPass.setUniform} that {@code GlRenderPassMixin} hooks. So our
 * expression-driven UBOs never reach Sodium chunk shaders by the normal path.
 *
 * <p>{@code begin} ends right after {@code activeProgram.bind()} ({@code glUseProgram}), so the
 * Sodium chunk program is the current GL program here. We bind any of our expression-uniform
 * UBO blocks that the program actually declares (gated by {@code glGetUniformBlockIndex}, so
 * non-matching programs are untouched). Requires "Sodium Core Shader Support" for the override
 * shader to declare the blocks in the first place.
 *
 * <p>We also drive the shadow-map terrain replay from here: {@code begin} has just bound the MAIN
 * framebuffer from {@code TerrainRenderPass.getTarget()}, so during a shadow pass we repoint it at
 * the shadow attachments (see {@link SodiumShadowRenderer}), and we capture the terrain atlas
 * {@code GpuSampler} to feed back into the replay's {@code drawChunkLayer}.
 *
 * <p>{@code require = 0}: this targets a Sodium internal that may change across versions; if the
 * method isn't found we silently no-op rather than crash.
 */
@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class SodiumChunkRendererMixin {

    @Inject(method = "begin", at = @At("TAIL"), remap = false, require = 0)
    private void polytone$bindExtraUniforms(TerrainRenderPass pass, FogParameters parameters,
                                            GpuSampler terrainSampler, CallbackInfo ci) {
        SodiumShadowRenderer.captureTerrainSampler(terrainSampler);
        SodiumShadowRenderer.rebindShadowFramebufferIfActive();
        Polytone.SHADER_EFFECTS.bindToCurrentGlProgram();
    }
}
