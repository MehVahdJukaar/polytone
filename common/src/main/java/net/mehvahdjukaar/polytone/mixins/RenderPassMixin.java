package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import net.mehvahdjukaar.polytone.content.shaders.IProgramWithModifiers;
import net.mehvahdjukaar.polytone.content.shaders.IProgramWithModifiers.EnabledModifiers;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.mixins.accessor.GlRenderPassAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPass.class)
public class RenderPassMixin {

    @Shadow
    @Final
    private RenderPassBackend backend;

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void poly$onSetPipeline(RenderPipeline renderPipeline, CallbackInfo ci) {
        if (!(this.backend instanceof GlRenderPassAccessor acc)) return;
        GlRenderPipeline glPipeline = acc.polytone$getPipeline();
        if (glPipeline == null) return;
        GlProgram program = glPipeline.program();
        IProgramWithModifiers holder = (IProgramWithModifiers) program;
        EnabledModifiers enabled = holder.polytone$getModifiers();
        int generation = IShaderModifier.enabledListsGeneration();
        boolean isStale = enabled == null || enabled.generation() != generation;
        if (!isStale && enabled.modifiers().isEmpty()) return;

        IShader shader = IShader.ofRenderPass((RenderPass) (Object) this, program, renderPipeline);
        if (isStale) {
            enabled = new EnabledModifiers(IShaderModifier.usedBy(shader), generation);
            holder.polytone$setModifiers(enabled);
        }
        for (var modifier : enabled.modifiers()) {
            modifier.bindTo(shader);
        }
    }
}
