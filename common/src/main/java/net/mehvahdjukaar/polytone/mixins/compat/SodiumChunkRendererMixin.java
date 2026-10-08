package net.mehvahdjukaar.polytone.mixins.compat;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlTexelBuffer;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.mehvahdjukaar.polytone.content.shaders.light.sodium.SodiumChunkProgram;
import net.mehvahdjukaar.polytone.content.shaders.shadow.sodium.SodiumShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

//sodium chunk programs bind their own uniforms, they never go through RenderPass.setUniform
@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class SodiumChunkRendererMixin {

    @Shadow(remap = false)
    protected GlProgram<ChunkShaderInterface> activeProgram;

    @Unique
    private final Map<GlProgram<?>, SodiumChunkProgram> polytone$shaderByProgram = new IdentityHashMap<>();

    @Inject(method = "begin", remap = false, at = @At("TAIL"), require = 0)
    private void polytone$bindExtraUniforms(TerrainRenderPass pass, FogParameters parameters,
                                            GpuSampler terrainSampler, GpuBufferSlice dynamicTransforms,
                                            GlTexelBuffer texelBuffer, CallbackInfo ci) {
        SodiumShadowRenderer.captureTerrainSampler(terrainSampler);
        SodiumShadowRenderer.rebindShadowFramebufferIfActive();
        polytone$shaderByProgram.computeIfAbsent(activeProgram, p -> new SodiumChunkProgram(p.handle(), pass.getPipeline()))
                .bindModifiers();
    }
}
