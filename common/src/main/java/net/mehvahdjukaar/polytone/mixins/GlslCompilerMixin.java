package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import net.mehvahdjukaar.polytone.Polytone;
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

    // gl lets any shader use vanilla's default blocks and ours without the pipeline listing them, so vulkan does too
    @ModifyExpressionValue(method = "addToBindGroup", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lcom/mojang/blaze3d/pipeline/BindGroupLayout;flattenUniforms(Ljava/util/List;)Ljava/util/List;"))
    private static List<BindGroupLayout.UniformDescription> poly$addBuiltInBlocks(List<BindGroupLayout.UniformDescription> original) {
        List<BindGroupLayout.UniformDescription> withOurs = new ArrayList<>(original);
        for (String name : GlProgram.BUILT_IN_UNIFORMS) {
            withOurs.add(new BindGroupLayout.UniformDescription(name, UniformType.UNIFORM_BUFFER));
        }
        for (String name : PolytoneBuiltInUniformsSet.dynamicNames()) {
            withOurs.add(new BindGroupLayout.UniformDescription(name, UniformType.UNIFORM_BUFFER));
        }
        return withOurs;
    }

    // gl links samplers the pipeline doesn't list too, unbound ones get a stand-in from DeclaredUniforms
    @ModifyExpressionValue(method = "addToBindGroup", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/BindGroupLayout;flattenSamplers(Ljava/util/List;)Ljava/util/List;"))
    private static List<String> poly$acceptSampler(List<String> original, @Local String name) {
        if (original.contains(name)) return original;
        List<String> withIt = new ArrayList<>(original);
        withIt.add(name);
        return withIt;
    }

    @Inject(method = "addToBindGroup", at = @At("TAIL"))
    private static void poly$latchDeclared(List<VulkanBindGroupLayout.Entry> entries, IntermediaryShaderModule shader,
                                           RenderPipeline pipeline, CallbackInfo ci) {
        Set<String> declared = new HashSet<>(entries.size());
        for (var e : entries) declared.add(e.name());
        PostChainsManager.onProgramLinked(declared);
        for (String name : PostChainsManager.dynamicSamplers()) {
            if (declared.contains(name)) PostChainsManager.onDynamicSamplerDeclared(name);
        }
        // vulkan half of the shader_modifiers block check, blocks only
        Set<String> blocks = new HashSet<>(entries.size());
        for (var e : entries) {
            if (e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER) blocks.add(e.name());
        }
        Polytone.SHADER_EFFECTS.onPipelineLinked(pipeline, blocks);
    }
}
