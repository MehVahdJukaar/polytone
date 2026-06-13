package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.polytone.Polytone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlRenderPass.class)
public class GlRenderPassMixin {

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(RenderPipeline renderPipeline, CallbackInfo ci) {
        RenderPass pass = (RenderPass) (Object) this;
        Polytone.POST_SHADERS.setupExtraUniforms(pass);
        Polytone.CORE_SHADERS.tryApply(pass, renderPipeline);
    }
}
