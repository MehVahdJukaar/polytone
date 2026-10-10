package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.content.shaders.CompiledPipelineSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PipelineCache.class)
public class PipelineCacheMixin {

    @Inject(method = "insert", at = @At("HEAD"))
    private void poly$recordInserted(RenderPipeline pipeline, CompiledRenderPipeline compiled, CallbackInfo ci) {
        CompiledPipelineSources.record(compiled, pipeline);
    }

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private CompiledRenderPipeline poly$recordCompiled(CompiledRenderPipeline compiled, RenderPipeline pipeline) {
        if (compiled != null) CompiledPipelineSources.record(compiled, pipeline);
        return compiled;
    }
}
