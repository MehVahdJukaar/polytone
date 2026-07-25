package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.mehvahdjukaar.polytone.content.shaders.PostChainsManager;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Mixin(GlProgram.class)
public class GlProgramMixin {

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet([Ljava/lang/Object;)Ljava/util/HashSet;"))
    private static HashSet<String> poly$addBuiltInBlocks(HashSet<String> original) {
        // BUILT_IN_UNIFORMS is consulted only for uniform BLOCKS the shader declares without the
        // pipeline listing them, so ONLY UBO block names belong here. InShadow is a sampler and is
        // handled separately in setupUniforms below (adding it here would do nothing).
        PolytoneBuiltInUniformsSet s = new PolytoneBuiltInUniformsSet(original);
        s.add(PostChainsManager.GLOBALS_NAME);
        s.add(PostChainsManager.SHADOW_UBO_NAME);
        return s;
    }

    // Register Polytone's runtime-bound samplers (InShadow) on any program that actually declares
    // them. Vanilla's setupUniforms only allocates a texture unit for samplers in the pipeline's
    // sampler list; a post shader that samples InShadow isn't in that list, so without this the
    // sampler stays at its default (unit 0) and reads the scene colour/depth instead of the shadow
    // map - which made the shadow pass appear to render from the camera's POV. We give it the next
    // free texture unit (unit indices are shared across samplers + texel buffers).
    @Inject(method = "setupUniforms", at = @At("TAIL"))
    private void poly$registerDynamicSamplers(List<RenderPipeline.UniformDescription> uniforms,
                                              List<String> samplers, CallbackInfo ci) {
        GlProgram self = (GlProgram) (Object) this;
        Map<String, Uniform> byName = self.getUniforms();
        for (String name : PostChainsManager.DYNAMIC_SAMPLERS) {
            if (byName.containsKey(name)) continue;
            int location = GL20C.glGetUniformLocation(self.getProgramId(), name);
            if (location == -1) continue; // this program doesn't use the sampler
            int nextUnit = 0;
            for (Uniform u : byName.values()) {
                if (u instanceof Uniform.Sampler || u instanceof Uniform.Utb) nextUnit++;
            }
            byName.put(name, new Uniform.Sampler(location, nextUnit));
        }
    }
}
