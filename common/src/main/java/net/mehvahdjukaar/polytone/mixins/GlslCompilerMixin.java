package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.mehvahdjukaar.polytone.content.shaders.PostChainsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(GlslCompiler.class)
public class GlslCompilerMixin {

    @ModifyExpressionValue(method = "addToBindGroup", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lcom/mojang/blaze3d/pipeline/BindGroupLayout;flattenUniforms(Ljava/util/List;)Ljava/util/List;"))
    private static List<BindGroupLayout.UniformDescription> poly$addBuiltInBlocks(List<BindGroupLayout.UniformDescription> original) {
        List<BindGroupLayout.UniformDescription> withOurs = new ArrayList<>(original);
        for (String name : PolytoneBuiltInUniformsSet.dynamicNames()) {
            withOurs.add(new BindGroupLayout.UniformDescription(name, UniformType.UNIFORM_BUFFER));
        }
        return withOurs;
    }

    @ModifyExpressionValue(method = "addToBindGroup", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/BindGroupLayout;flattenSamplers(Ljava/util/List;)Ljava/util/List;"))
    private static List<String> poly$addDynamicSamplers(List<String> original) {
        List<String> withOurs = new ArrayList<>(original);
        withOurs.addAll(PostChainsManager.DYNAMIC_SAMPLERS);
        return withOurs;
    }

    @Inject(method = "addToBindGroup", at = @At("TAIL"))
    private static void poly$latchDeclared(List<VulkanBindGroupLayout.Entry> entries, IntermediaryShaderModule shader,
                                           RenderPipeline pipeline, CallbackInfo ci) {
        Set<String> declared = new HashSet<>(entries.size());
        for (var e : entries) declared.add(e.name());
        PostChainsManager.onProgramLinked(declared);
        for (String name : PostChainsManager.DYNAMIC_SAMPLERS) {
            if (declared.contains(name)) PostChainsManager.onDynamicSamplerDeclared(name);
        }
    }
}
