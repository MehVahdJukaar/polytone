package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.mixins.accessor.GlRenderPassAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(RenderPass.class)
public class RenderPassMixin {

    @Shadow
    @Final
    private RenderPassBackend backend;

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(RenderPipeline renderPipeline, CallbackInfo ci) {
        if (!Polytone.POST_CHAINS.hasAnyPassBindings() && !Polytone.SHADER_EFFECTS.hasAnyRegistered()) return;
        if (!(this.backend instanceof GlRenderPassAccessor acc)) return;
        GlRenderPipeline glPipeline = acc.polytone$getPipeline();
        if (glPipeline == null) return;
        Set<String> declared = glPipeline.program().getUniforms().keySet();
        if (declared.isEmpty()) return;
        RenderPass pass = (RenderPass) (Object) this;
        Polytone.POST_CHAINS.setupExtraUniforms(pass, declared);
        Polytone.POST_CHAINS.bindExtraSamplers(pass, renderPipeline, declared);
        Polytone.SHADER_EFFECTS.tryApply(pass, renderPipeline, declared);
    }
}
