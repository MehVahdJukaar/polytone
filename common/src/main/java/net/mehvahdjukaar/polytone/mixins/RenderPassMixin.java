package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.polytone.Polytone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.1 split the render pass into a RenderPass frontend (this) and a RenderPassBackend (GlRenderPass);
// setPipeline delegates frontend -> backend, so we hook the frontend where `this` actually IS a RenderPass.
@Mixin(RenderPass.class)
public class RenderPassMixin {

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(RenderPipeline renderPipeline, CallbackInfo ci) {
        RenderPass pass = (RenderPass) (Object) this;
        Polytone.POST_CHAINS.setupExtraUniforms(pass);
        Polytone.SHADER_EFFECTS.tryApply(pass, renderPipeline);
    }
}
