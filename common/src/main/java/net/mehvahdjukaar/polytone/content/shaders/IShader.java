package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

public interface IShader {

    //true for samplers too
    boolean hasUniform(String name);

    void setUniform(String name, GpuBufferSlice value);

    void setSampler(String name, GpuTextureView texture, GpuSampler sampler);

    int programId();

    RenderPipeline pipeline();

    static IShader ofRenderPass(RenderPass pass, GlProgram program, RenderPipeline pipeline) {
        return new IShader() {
            @Override
            public boolean hasUniform(String name) {
                return program.getUniforms().containsKey(name);
            }

            //skip blocks the program doesn't declare, else Iris/Sodium logs binding errors
            @Override
            public void setUniform(String name, GpuBufferSlice value) {
                if (hasUniform(name)) pass.setUniform(name, value);
            }

            @Override
            public void setSampler(String name, GpuTextureView texture, GpuSampler sampler) {
                if (hasUniform(name)) pass.bindTexture(name, texture, sampler);
            }

            @Override
            public int programId() {
                return program.getProgramId();
            }

            @Override
            public RenderPipeline pipeline() {
                return pipeline;
            }
        };
    }
}
