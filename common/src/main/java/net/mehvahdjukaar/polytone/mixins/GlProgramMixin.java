package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
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
        return new PolytoneBuiltInUniformsSet(original);
    }

    @Inject(method = "setupBindGroupLayouts", at = @At("TAIL"))
    private void poly$registerDynamicSamplers(List<BindGroupLayout> bindGroupLayouts, CallbackInfo ci) {
        GlProgram self = (GlProgram) (Object) this;
        Map<String, Uniform> byName = self.getUniforms();
        PostChainsManager.onProgramLinked(byName.keySet());
        for (String name : PostChainsManager.DYNAMIC_SAMPLERS) {
            if (byName.containsKey(name)) continue;
            int location = GL20C.glGetUniformLocation(self.getProgramId(), name);
            if (location == -1) continue;
            int nextUnit = 0;
            for (Uniform u : byName.values()) {
                if (u instanceof Uniform.Sampler || u instanceof Uniform.Utb) nextUnit++;
            }
            byName.put(name, new Uniform.Sampler(location, nextUnit));
            PostChainsManager.onDynamicSamplerDeclared(name);
        }
    }
}
