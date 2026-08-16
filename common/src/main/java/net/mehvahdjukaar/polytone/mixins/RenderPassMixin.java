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

// setPipeline lives on the RenderPass frontend and delegates to the GlRenderPass backend that holds the program
@Mixin(RenderPass.class)
public class RenderPassMixin {

    @Shadow
    @Final
    private RenderPassBackend backend;

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(RenderPipeline renderPipeline, CallbackInfo ci) {
        // runs on every draw in the game, so bail early when no shader wants anything from us
        if (!Polytone.POST_CHAINS.hasAnyPassBindings() && !Polytone.SHADER_EFFECTS.hasAnyRegistered()) return;
        if (!(this.backend instanceof GlRenderPassAccessor acc)) return;
        GlRenderPipeline glPipeline = acc.polytone$getPipeline();
        if (glPipeline == null) return;
        // only bind what the program declares: undeclared binds make Iris/Sodium log errors every frame
        Set<String> declared = glPipeline.program().getUniforms().keySet();
        if (declared.isEmpty()) return;
        RenderPass pass = (RenderPass) (Object) this;
        Polytone.POST_CHAINS.bindUniformBlocks(pass, declared);
        Polytone.POST_CHAINS.bindSamplers(pass, renderPipeline, declared);
        Polytone.SHADER_EFFECTS.tryApply(pass, renderPipeline, declared);
    }
}
