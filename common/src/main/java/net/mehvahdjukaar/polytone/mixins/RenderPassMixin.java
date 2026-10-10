package net.mehvahdjukaar.polytone.mixins;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.CompiledPipelineSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(FrontendRenderPass.class)
public class RenderPassMixin {

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(CompiledRenderPipeline compiled, CallbackInfo ci) {
        if (!Polytone.POST_CHAINS.hasAnyPassBindings() && !Polytone.SHADER_EFFECTS.hasAnyRegistered()) return;
        if (!(compiled instanceof FrontendRenderPipeline frontend)) return;
        RenderPipeline renderPipeline = CompiledPipelineSources.get(compiled);
        if (renderPipeline == null) return;
        Set<String> declared = frontend.uniformIndices().keySet();
        if (declared.isEmpty()) return;
        RenderPass pass = (RenderPass) (Object) this;
        Polytone.POST_CHAINS.bindUniformBlocks(pass, declared);
        Polytone.POST_CHAINS.bindSamplers(pass, renderPipeline, declared);
        Polytone.SHADER_EFFECTS.tryApply(pass, renderPipeline, declared);
    }
}
