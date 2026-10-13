package net.mehvahdjukaar.polytone.content.shaders.light.sodium;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.mehvahdjukaar.polytone.content.shaders.IProgramWithModifiers.EnabledModifiers;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.shaders.light.PolyShaderPointLights;
import net.mehvahdjukaar.polytone.content.shaders.voxel.VoxelVolumeManager;
import net.mehvahdjukaar.polytone.mixins.accessor.GlBufferAccessor;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;

import java.util.List;

//sodium binds its chunk program itself. its source comes from the jar so our light include is all that can end up in there
public class SodiumChunkProgram implements IShader {

    //our includes read CameraBlockPos from vanilla Globals, sodium never binds that one
    private static final List<String> BLOCKS = List.of("Globals", VoxelVolumeManager.BLOCK_NAME, PolyShaderPointLights.BLOCK_NAME);
    //sodium keeps u_Globals on 0
    private static final int FIRST_BINDING_POINT = 1;

    private final int program;
    private final RenderPipeline pipeline;
    private EnabledModifiers enabled = null;

    public SodiumChunkProgram(int program, RenderPipeline pipeline) {
        this.program = program;
        this.pipeline = pipeline;
        for (int i = 0; i < BLOCKS.size(); i++) {
            int blockIndex = GL32C.glGetUniformBlockIndex(program, BLOCKS.get(i));
            if (blockIndex >= 0) GL32C.glUniformBlockBinding(program, blockIndex, FIRST_BINDING_POINT + i);
        }
        VoxelVolumeManager.assignTextureUnits(program);
    }

    //program must be bound
    public void bindModifiers() {
        GpuBuffer globals = RenderSystem.getGlobalSettingsUniform();
        if (globals != null) setUniform("Globals", globals.slice());

        int generation = IShaderModifier.enabledListsGeneration();
        if (enabled == null || enabled.generation() != generation) {
            enabled = new EnabledModifiers(IShaderModifier.usedBy(this), generation);
        }
        for (var modifier : enabled.modifiers()) {
            modifier.bindTo(this);
        }
    }

    @Override
    public boolean hasUniform(String name) {
        return GL32C.glGetUniformBlockIndex(program, name) >= 0 || GLHelper.usesUniform(program, name);
    }

    @Override
    public void setUniform(String name, GpuBufferSlice value) {
        int i = BLOCKS.indexOf(name);
        if (i < 0) return;
        int buffer = ((GlBufferAccessor) (Object) value.buffer()).polytone$getHandle();
        GL30C.glBindBufferRange(GL31C.GL_UNIFORM_BUFFER, FIRST_BINDING_POINT + i, buffer, value.offset(), value.length());
    }

    @Override
    public void setSampler(String name, GpuTextureView texture, GpuSampler sampler) {
    }

    @Override
    public int programId() {
        return program;
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }
}
