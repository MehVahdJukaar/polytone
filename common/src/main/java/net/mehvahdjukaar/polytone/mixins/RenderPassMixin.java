package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.DeclaredUniforms;
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
        RenderPass pass = (RenderPass) (Object) this;
        if (Polytone.POST_CHAINS.hasAnyPassBindings() || Polytone.SHADER_EFFECTS.hasAnyRegistered()) {
            Set<String> declared = DeclaredUniforms.forBackend(this.backend);
            if (declared != null && !declared.isEmpty()) {
                Polytone.POST_CHAINS.bindUniformBlocks(pass, declared);
                Polytone.POST_CHAINS.bindSamplers(pass, renderPipeline, declared);
                Polytone.SHADER_EFFECTS.tryApply(pass, renderPipeline, declared);
            }
        }
        DeclaredUniforms.bindUnbound(pass, this.backend);
    }
}
