package net.mehvahdjukaar.polytone.content.shaders.light.sodium;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.mehvahdjukaar.polytone.content.shaders.IProgramWithModifiers.EnabledModifiers;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneRenderHooks;
import net.mehvahdjukaar.polytone.content.shaders.light.PolyShaderPointLights;
import net.mehvahdjukaar.polytone.content.shaders.voxel.VoxelVolumeManager;
import net.mehvahdjukaar.polytone.mixins.accessor.GlBufferAccessor;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;

import java.util.ArrayList;
import java.util.List;

//sodium binds its chunk program itself. its source comes from the jar so our light include is all that can end up in there
public class SodiumChunkProgram implements IShaderInputs {

    //our includes read CameraBlockPos from vanilla Globals, sodium never binds that one
    private static final List<String> BUILT_IN_BLOCKS = List.of("Globals", VoxelVolumeManager.BLOCK_NAME, PolyShaderPointLights.BLOCK_NAME);
    //sodium keeps u_Globals on 0
    private static final int FIRST_BINDING_POINT = 1;


    private final int program;
    private final List<String> blocks = new ArrayList<>(BUILT_IN_BLOCKS);
    private final RenderPipeline pipeline;
    private EnabledModifiers enabled = null;

    public SodiumChunkProgram(int program, RenderPipeline pipeline) {
        this.program = program;
        this.pipeline = pipeline;
        blocks.addAll(PolytoneBuiltInUniformsSet.dynamicBlocks());
        for (int i = 0; i < blocks.size(); i++) {
            int blockIndex = GL32C.glGetUniformBlockIndex(program, blocks.get(i));
            if (blockIndex >= 0) GL32C.glUniformBlockBinding(program, blockIndex, FIRST_BINDING_POINT + i);
        }
        VoxelVolumeManager.assignTextureUnits(program);
    }

    //program must be bound
    public void bindModifiers() {
        GpuBuffer globals = RenderSystem.getGlobalSettingsUniform();
        if (globals != null) setUniform("Globals", globals.slice());

        int generation = PolytoneRenderHooks.inputPatchesGeneration();
        if (enabled == null || enabled.generation() != generation) {
            enabled = new EnabledModifiers(PolytoneRenderHooks.inputPatchesFor(this), generation);
        }
        for (var modifier : enabled.modifiers()) {
            modifier.bindToShader(this);
        }
    }

    @Override
    public boolean hasUniform(String name) {
        return GL32C.glGetUniformBlockIndex(program, name) >= 0 || GLHelper.usesUniform(program, name);
    }

    @Override
    public void setUniform(String name, GpuBufferSlice value) {
        int i = blocks.indexOf(name);
        if (i < 0) return;
        int buffer = ((GlBufferAccessor) value.buffer()).polytone$getHandle();
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
