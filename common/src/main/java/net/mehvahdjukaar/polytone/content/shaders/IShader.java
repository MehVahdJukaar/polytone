package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import java.util.Set;

public interface IShader {

    //true for samplers too
    boolean hasUniform(String name);

    void setUniform(String name, GpuBufferSlice value);

    void setSampler(String name, GpuTextureView texture, GpuSampler sampler);

    int programId();

    default boolean isGlProgram() {
        return programId() > 0;
    }

    RenderPipeline pipeline();

    static IShader ofRenderPass(RenderPass pass, Set<String> uniformNames, RenderPipeline pipeline) {
        return new IShader() {
            @Override
            public boolean hasUniform(String name) {
                return uniformNames.contains(name);
            }

            //skip blocks the program doesn't declare, else Iris/Sodium logs binding errors
            @Override
            public void setUniform(String name, GpuBufferSlice value) {
                if (hasUniform(name)) pass.setUniform(name, value);
            }

            @Override
            public void setSampler(String name, GpuTextureView texture, GpuSampler sampler) {
                if (hasUniform(name)) pass.setUniform(name, texture, sampler);
            }

            @Override
            public int programId() {
                // TODO: vulkan
                return -1;
            }

            @Override
            public RenderPipeline pipeline() {
                return pipeline;
            }
        };
    }
}
