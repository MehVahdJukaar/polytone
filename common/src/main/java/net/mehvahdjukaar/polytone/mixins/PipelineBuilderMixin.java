package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.mehvahdjukaar.polytone.content.shaders.PostChainsManager;
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
        for (String name : PolytoneBuiltInUniformsSet.dynamicNames()) {
            if (existing.add(name)) withOurs.add(new BindGroupLayout.UniformDescription(name, UniformType.UNIFORM_BUFFER));
        }
        for (String name : PostChainsManager.DYNAMIC_SAMPLERS) {
            if (existing.add(name)) withOurs.add(new BindGroupLayout.UniformDescription(name, UniformType.COMBINED_IMAGE_SAMPLER));
        }
        return withOurs;
    }

    // the create info only lists the uniforms the shaders declare
    @ModifyReturnValue(method = "generateBackendCreateInfo", at = @At("RETURN"))
    private BackendRenderPipeline.CreateInfo poly$latchDeclared(BackendRenderPipeline.CreateInfo createInfo) {
        if (createInfo == null) return null;
        Set<String> declared = new HashSet<>();
        for (var u : createInfo.uniforms()) declared.add(u.name());
        PostChainsManager.onProgramLinked(declared);
        for (String name : PostChainsManager.DYNAMIC_SAMPLERS) {
            if (declared.contains(name)) PostChainsManager.onDynamicSamplerDeclared(name);
        }
        return createInfo;
    }
}
