package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import net.mehvahdjukaar.polytone.mixins.accessor.GlRenderPassAccessor;
import net.mehvahdjukaar.polytone.mixins.accessor.VulkanRenderPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DeclaredUniforms {

    // every driver allows a uniform block at least this big, so one buffer stands in for any of ours
    private static final int ZERO_UBO_SIZE = 16384;

    private record Ours(List<String> blocks, List<String> samplers) {
    }

    private static final Map<VulkanRenderPipeline, Set<String>> VULKAN_CACHE = new IdentityHashMap<>();
    private static final Map<Object, Ours> OURS = new IdentityHashMap<>();
    private static int blockNamesSeen = -1;
    private static @Nullable GpuBuffer zeroUbo;

    public static @Nullable Set<String> forBackend(RenderPassBackend backend) {
        if (backend instanceof GlRenderPassAccessor gl) {
            GlRenderPipeline pipeline = gl.polytone$getPipeline();
            if (pipeline == null) return null;
            return pipeline.program().getUniforms().keySet();
        }
        if (backend instanceof VulkanRenderPassAccessor vulkan) {
            VulkanRenderPipeline pipeline = vulkan.polytone$getPipeline();
            if (pipeline == null) return null;
            return VULKAN_CACHE.computeIfAbsent(pipeline, p -> namesOf(p.layout()));
        }
        return null;
    }

    // vulkan won't draw with a block or sampler unbound and gl reads stale data, so ours get zeros or the missing texture
    public static void bindUnbound(RenderPass pass, RenderPassBackend backend) {
        Object pipeline;
        Map<String, GpuBufferSlice> boundBlocks;
        Map<String, ?> boundSamplers;
        if (backend instanceof GlRenderPassAccessor gl) {
            pipeline = gl.polytone$getPipeline();
            boundBlocks = gl.polytone$getUniforms();
            boundSamplers = gl.polytone$getSamplers();
        } else if (backend instanceof VulkanRenderPassAccessor vulkan) {
            pipeline = vulkan.polytone$getPipeline();
            boundBlocks = vulkan.polytone$getUniforms();
            boundSamplers = vulkan.polytone$getTextures();
        } else return;
        if (pipeline == null) return;

        Set<String> blockNames = PolytoneBuiltInUniformsSet.dynamicNames();
        // block names are only ever added, at reload
        if (blockNames.size() != blockNamesSeen) {
            OURS.clear();
            blockNamesSeen = blockNames.size();
        }
        Ours ours = OURS.computeIfAbsent(pipeline, p -> oursOf(p, blockNames));

        for (String name : ours.blocks) {
            if (!boundBlocks.containsKey(name)) pass.setUniform(name, zeroUbo());
        }
        for (String name : ours.samplers) {
            if (boundSamplers.containsKey(name)) continue;
            GpuTextureView missing = Minecraft.getInstance().getTextureManager()
                    .getTexture(MissingTextureAtlasSprite.getLocation()).getTextureView();
            pass.bindTexture(name, missing, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
    }

    // a sampler the pipeline doesn't list is ours, even one whose name a later reload dropped
    private static Ours oursOf(Object pipeline, Set<String> blockNames) {
        List<String> blocks = new ArrayList<>();
        List<String> samplers = new ArrayList<>();
        if (pipeline instanceof GlRenderPipeline gl) {
            List<String> listed = BindGroupLayout.flattenSamplers(gl.info().getBindGroupLayouts());
            gl.program().getUniforms().forEach((name, uniform) -> {
                if (uniform instanceof Uniform.Ubo && blockNames.contains(name)) blocks.add(name);
                else if (uniform instanceof Uniform.Sampler && !listed.contains(name)) samplers.add(name);
            });
        } else if (pipeline instanceof VulkanRenderPipeline vulkan) {
            List<String> listed = BindGroupLayout.flattenSamplers(vulkan.info().getBindGroupLayouts());
            for (var e : vulkan.layout().entries()) {
                if (e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER && blockNames.contains(e.name())) {
                    blocks.add(e.name());
                } else if (e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.SAMPLED_IMAGE && !listed.contains(e.name())) {
                    samplers.add(e.name());
                }
            }
        }
        return new Ours(blocks, samplers);
    }

    private static GpuBuffer zeroUbo() {
        if (zeroUbo == null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                zeroUbo = RenderSystem.getDevice().createBuffer(() -> "Polytone zero UBO",
                        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, stack.calloc(ZERO_UBO_SIZE));
            }
        }
        return zeroUbo;
    }

    public static void clearCache() {
        VULKAN_CACHE.clear();
        OURS.clear();
        if (zeroUbo != null) {
            zeroUbo.close();
            zeroUbo = null;
        }
    }

    private static Set<String> namesOf(VulkanBindGroupLayout layout) {
        List<VulkanBindGroupLayout.Entry> entries = layout.entries();
        Set<String> names = new HashSet<>(entries.size());
        for (var e : entries) names.add(e.name());
        return names;
    }
}
