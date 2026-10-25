package net.mehvahdjukaar.polytone.mixins;

import com.google.common.collect.MapMaker;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import net.mehvahdjukaar.polytone.content.shaders.CompiledPipelineSources;
import net.mehvahdjukaar.polytone.content.shaders.IProgramWithModifiers.EnabledModifiers;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneRenderHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(FrontendRenderPass.class)
public class RenderPassMixin {

    @Unique
    private static final Map<CompiledRenderPipeline, EnabledModifiers> poly$enabledByPipeline = new MapMaker().weakKeys().makeMap();

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(CompiledRenderPipeline compiled, CallbackInfo ci) {
        if (!(compiled instanceof FrontendRenderPipeline frontend)) return;
        RenderPipeline renderPipeline = CompiledPipelineSources.get(compiled);
        if (renderPipeline == null) return;
        EnabledModifiers enabled = poly$enabledByPipeline.get(compiled);
        int generation = PolytoneRenderHooks.inputPatchesGeneration();
        boolean isStale = enabled == null || enabled.generation() != generation;
        if (!isStale && enabled.modifiers().isEmpty()) return;

        IShaderInputs shader = IShaderInputs.ofRenderPass((RenderPass) (Object) this, frontend.uniformIndices().keySet(), renderPipeline);
        if (isStale) {
            enabled = new EnabledModifiers(PolytoneRenderHooks.inputPatchesFor(shader), generation);
            poly$enabledByPipeline.put(compiled, enabled);
        }
        for (var modifier : enabled.modifiers()) {
            modifier.bindToShader(shader);
        }
    }
}
