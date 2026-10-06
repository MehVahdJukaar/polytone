package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.content.shaders.IProgramWithModifiers;
import net.mehvahdjukaar.polytone.content.shaders.IProgramWithModifiers.EnabledModifiers;
import net.mehvahdjukaar.polytone.content.shaders.PolyGlobalUniforms;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.mehvahdjukaar.polytone.content.shaders.light.PolyShaderPointLights;
import net.mehvahdjukaar.polytone.content.shaders.shadow.ShadowMapManager;
import net.mehvahdjukaar.polytone.content.shaders.voxel.VoxelVolumeManager;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Mixin(GlProgram.class)
public class GlProgramMixin implements IProgramWithModifiers {

    @Unique
    private static final List<String> polytone$DYNAMIC_SAMPLERS = List.of(ShadowMapManager.SAMPLER);

    @Unique
    @Nullable
    private EnabledModifiers polytone$modifiers = null;

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet([Ljava/lang/Object;)Ljava/util/HashSet;"))
    private static HashSet<String> poly$addBuiltInBlocks(HashSet<String> original) {
        PolytoneBuiltInUniformsSet s = new PolytoneBuiltInUniformsSet(original);
        s.add(PolyGlobalUniforms.BLOCK_NAME);
        s.add(ShadowMapManager.BLOCK_NAME);
        s.add(VoxelVolumeManager.BLOCK_NAME);
        s.add(PolyShaderPointLights.BLOCK_NAME);
        return s;
    }

    @Inject(method = "setupUniforms", at = @At("TAIL"))
    private void poly$registerDynamicSamplers(List<RenderPipeline.UniformDescription> uniforms,
                                              List<String> samplers, CallbackInfo ci) {
        GlProgram self = (GlProgram) (Object) this;
        Map<String, Uniform> byName = self.getUniforms();
        VoxelVolumeManager.assignTextureUnits(self.getProgramId());
        for (String name : polytone$DYNAMIC_SAMPLERS) {
            if (byName.containsKey(name)) continue;
            int location = GL20C.glGetUniformLocation(self.getProgramId(), name);
            if (location == -1) continue;
            int nextUnit = 0;
            for (Uniform u : byName.values()) {
                if (u instanceof Uniform.Sampler || u instanceof Uniform.Utb) nextUnit++;
            }
            byName.put(name, new Uniform.Sampler(location, nextUnit));
        }
    }

    @Override
    public @Nullable EnabledModifiers polytone$getModifiers() {
        return polytone$modifiers;
    }

    @Override
    public void polytone$setModifiers(EnabledModifiers modifiers) {
        this.polytone$modifiers = modifiers;
    }
}
