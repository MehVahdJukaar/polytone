package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlTexelBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.shadow.sodium.SodiumShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//sodium chunk programs bind their own uniforms, they never go through RenderPass.setUniform
@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class SodiumChunkRendererMixin {

    @Inject(method = "begin", at = @At("TAIL"), require = 0)
    private void polytone$bindExtraUniforms(TerrainRenderPass pass, FogParameters parameters,
                                            GpuSampler terrainSampler, GpuBufferSlice dynamicTransforms,
                                            GlTexelBuffer texelBuffer, CallbackInfo ci) {
        SodiumShadowRenderer.captureTerrainSampler(terrainSampler);
        SodiumShadowRenderer.rebindShadowFramebufferIfActive();
        Polytone.SHADER_UNIFORMS.bindToCurrentGlProgram();
    }
}
