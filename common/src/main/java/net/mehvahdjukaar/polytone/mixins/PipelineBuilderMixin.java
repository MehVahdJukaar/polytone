package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The GL and Vulkan backends both read their uniforms from this shared SPIR-V step
@Mixin(PipelineBuilder.class)
public class PipelineBuilderMixin {

    // lets any shader declare Polytone's uniform blocks and dynamic samplers without failing the pipeline layout check
    @ModifyExpressionValue(method = "generateBackendCreateInfo", at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/pipeline/BindGroupLayout;flattenUniforms(Ljava/util/List;)Ljava/util/List;"))
    private List<BindGroupLayout.UniformDescription> poly$addBuiltInUniforms(List<BindGroupLayout.UniformDescription> original) {
        Set<String> existing = new HashSet<>(original.size());
        for (var u : original) existing.add(u.name());
        List<BindGroupLayout.UniformDescription> withOurs = new ArrayList<>(original);
        for (String name : PolytoneBuiltInUniformsSet.dynamicBlocks()) {
            if (existing.add(name)) withOurs.add(new BindGroupLayout.UniformDescription(name, UniformType.UNIFORM_BUFFER));
        }
        return withOurs;
    }
}
