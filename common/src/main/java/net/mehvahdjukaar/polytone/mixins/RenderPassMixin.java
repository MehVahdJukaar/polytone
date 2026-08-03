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

// 26.1 split the render pass into a RenderPass frontend (this) and a RenderPassBackend (GlRenderPass);
// setPipeline delegates frontend -> backend, so we hook the frontend where `this` actually IS a RenderPass.
@Mixin(RenderPass.class)
public class RenderPassMixin {

    @Shadow
    @Final
    private RenderPassBackend backend;

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(RenderPipeline renderPipeline, CallbackInfo ci) {
        // This runs for every draw setup in the game, so bail before touching anything when no
        // loaded shader declared one of our blocks/samplers and no pack registered any uniforms.
        if (!Polytone.POST_CHAINS.hasAnyPassBindings() && !Polytone.SHADER_EFFECTS.hasAnyRegistered()) return;
        // the GL backend holds the compiled program; non-GL backends expose no declared uniforms to gate on
        if (!(this.backend instanceof GlRenderPassAccessor acc)) return;
        GlRenderPipeline glPipeline = acc.polytone$getPipeline();
        if (glPipeline == null) return;
        // Only the UBO/uniform block names the bound program actually declares. Binding a uniform
        // the program doesn't have is a no-op in vanilla but makes Iris/Sodium log
        // "Error while binding uniform" spam every frame, so we gate every bind on this set.
        Set<String> declared = glPipeline.program().getUniforms().keySet();
        if (declared.isEmpty()) return;
        RenderPass pass = (RenderPass) (Object) this;
        Polytone.POST_CHAINS.setupExtraUniforms(pass, declared);
        Polytone.POST_CHAINS.bindExtraSamplers(pass, renderPipeline, declared);
        Polytone.SHADER_EFFECTS.tryApply(pass, renderPipeline, declared);
    }
}
