package net.mehvahdjukaar.polytone.mixins.compat;

import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneRenderHooks;
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
    private final Map<GlProgram<?>, List<IShaderInputPatch>> polytone$bindersByProgram = new IdentityHashMap<>();

    @Inject(method = "begin", remap = false, at = @At("TAIL"))
    private void polytone$bindExtraUniforms(TerrainRenderPass pass, CallbackInfo ci) {
        IShaderInputs shader = IShaderInputs.ofGenericProgram(activeProgram.handle());
        List<IShaderInputPatch> binders = polytone$bindersByProgram.computeIfAbsent(activeProgram, p -> PolytoneRenderHooks.inputPatchesFor(shader));
        for (var binder : binders) {
            binder.bindToShader(shader);
        }
    }
}
