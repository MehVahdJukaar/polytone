package net.mehvahdjukaar.polytone.mixins.compat;

import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class SodiumChunkRendererMixin {

    @Shadow(remap = false)
    protected GlProgram<ChunkShaderInterface> activeProgram;

    @Unique
    private final Map<GlProgram<?>, List<IShaderModifier>> polytone$modifiersByProgram = new IdentityHashMap<>();

    @Inject(method = "begin", remap = false, at = @At("TAIL"))
    private void polytone$bindExtraUniforms(TerrainRenderPass pass, CallbackInfo ci) {
        IShader shader = IShader.ofGenericProgram(activeProgram.handle());
        List<IShaderModifier> modifiers = polytone$modifiersByProgram.computeIfAbsent(activeProgram, p -> IShaderModifier.enabledFor(shader));
        for (var modifier : modifiers) {
            modifier.bindTo(shader);
        }
    }
}
