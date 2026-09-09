package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.polytone.Polytone;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(GlRenderPass.class)
public class GlRenderPassMixin {

    @Shadow
    @Nullable
    protected GlRenderPipeline pipeline;

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(RenderPipeline renderPipeline, CallbackInfo ci) {
        if (this.pipeline == null) return;
        Set<String> declared = this.pipeline.program().getUniforms().keySet();
        if (declared.isEmpty()) return;
        RenderPass pass = (RenderPass) this;
        Polytone.POST_CHAINS.setupExtraUniforms(pass, declared);
        Polytone.POST_CHAINS.bindExtraSamplers(pass, renderPipeline, declared);
        Polytone.SHADER_EFFECTS.tryApply(pass, renderPipeline, declared);
    }
}
