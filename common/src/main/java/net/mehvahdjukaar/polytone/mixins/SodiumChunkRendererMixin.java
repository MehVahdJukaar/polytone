package net.mehvahdjukaar.polytone.mixins;

import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.mehvahdjukaar.polytone.Polytone;
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
 * <p>{@code require = 0}: this targets a Sodium internal that may change across versions; if the
 * method isn't found we silently no-op rather than crash.
 */
@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class SodiumChunkRendererMixin {

    @Inject(method = "begin", at = @At("TAIL"), remap = false, require = 0)
    private void polytone$bindExtraUniforms(CallbackInfo ci) {
        Polytone.SHADER_EFFECTS.bindToCurrentGlProgram();
    }
}
