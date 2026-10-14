package net.mehvahdjukaar.polytone.mixins.accessor;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;

@Mixin(VulkanRenderPass.class)
public interface VulkanRenderPassAccessor {
    @Accessor("pipeline")
    @Nullable
    VulkanRenderPipeline polytone$getPipeline();

    @Accessor("uniforms")
    HashMap<String, GpuBufferSlice> polytone$getUniforms();

    @Accessor("textures")
    HashMap<String, ?> polytone$getTextures();
}
