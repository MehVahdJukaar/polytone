package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import net.mehvahdjukaar.polytone.Polytone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

// GL half of the shader_modifiers block check, GlslCompilerMixin is the vulkan one
@Mixin(GlDevice.class)
public class GlDeviceMixin {

    @Inject(method = "compileProgram", at = @At("RETURN"))
    private void polytone$checkExpressionBlocks(RenderPipeline pipeline, ShaderSource shaderSource,
                                                CallbackInfoReturnable<GlProgram> cir) {
        GlProgram program = cir.getReturnValue();
        if (program == null) return;
        Set<String> blocks = new HashSet<>();
        for (var e : program.getUniforms().entrySet()) {
            // blocks only
            if (e.getValue() instanceof Uniform.Ubo) blocks.add(e.getKey());
        }
        Polytone.SHADER_EFFECTS.onPipelineLinked(pipeline, blocks);
    }
}
