package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import net.mehvahdjukaar.polytone.mixins.accessor.GlRenderPassAccessor;
import net.mehvahdjukaar.polytone.mixins.accessor.VulkanRenderPassAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DeclaredUniforms {

    private static final Map<VulkanRenderPipeline, VulkanProgram> VULKAN_PROGRAMS = new IdentityHashMap<>();

    public static @Nullable IProgramWithModifiers boundProgram(RenderPassBackend backend) {
        if (backend instanceof GlRenderPassAccessor gl) {
            GlRenderPipeline pipeline = gl.polytone$getPipeline();
            if (pipeline == null) return null;
            return (IProgramWithModifiers) pipeline.program();
        }
        if (backend instanceof VulkanRenderPassAccessor vulkan) {
            VulkanRenderPipeline pipeline = vulkan.polytone$getPipeline();
            if (pipeline == null) return null;
            return VULKAN_PROGRAMS.computeIfAbsent(pipeline, p -> new VulkanProgram(namesOf(p.layout())));
        }
        return null;
    }

    public static void clearCache() {
        VULKAN_PROGRAMS.clear();
    }

    private static Set<String> namesOf(VulkanBindGroupLayout layout) {
        List<VulkanBindGroupLayout.Entry> entries = layout.entries();
        Set<String> names = new HashSet<>(entries.size());
        for (var e : entries) names.add(e.name());
        return names;
    }

    private static class VulkanProgram implements IProgramWithModifiers {
        private final Set<String> uniformNames;
        private EnabledModifiers modifiers;

        private VulkanProgram(Set<String> uniformNames) {
            this.uniformNames = uniformNames;
        }

        @Override
        public @Nullable EnabledModifiers polytone$getModifiers() {
            return modifiers;
        }

        @Override
        public void polytone$setModifiers(EnabledModifiers modifiers) {
            this.modifiers = modifiers;
        }

        @Override
        public Set<String> polytone$getUniformNames() {
            return uniformNames;
        }

        @Override
        public int polytone$getProgramId() {
            return -1;
        }
    }
}
