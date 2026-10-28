package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.shaders.ShaderType;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.api.IShaderSourcePatch;
import net.mehvahdjukaar.polytone.api.LevelRenderListener;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//all render nonsense stuff goes through here
public class PolytoneRenderHooks {

    private static final List<IShaderSourcePatch> SOURCE_PATCHERS = new ArrayList<>(List.of(Polytone.COLORED_LIGHTS));
    private static final List<IShaderInputPatch> INPUT_PATCHES = new ArrayList<>(List.of(
            Polytone.GLOBAL_UNIFORMS, Polytone.VOXEL_VOLUME, Polytone.COLORED_LIGHTS, Polytone.SHADER_UNIFORMS, Polytone.POST_CHAINS));
    private static final List<LevelRenderListener> LEVEL_RENDER_LISTENERS = new ArrayList<>(List.of(Polytone.VOXEL_VOLUME, Polytone.COLORED_LIGHTS));
    private static int inputPatchesGeneration = 0;

    public static void addSourcePatcher(IShaderSourcePatch patcher) {
        SOURCE_PATCHERS.add(patcher);
    }

    public static void addInputPatch(IShaderInputPatch patch) {
        INPUT_PATCHES.add(patch);
        invalidateInputPatches();
    }

    public static void addLevelRenderListener(LevelRenderListener listener) {
        LEVEL_RENDER_LISTENERS.add(listener);
    }

    public static String patchShader(Identifier shaderId, ShaderType type, String source, Map<Identifier, Resource> files) {
        for (var patcher : SOURCE_PATCHERS) {
            source = patcher.patchShaderSource(shaderId, type, source, files);
        }
        return source;
    }

    public static String patchSodiumChunkShader(String source) {
        for (var patcher : SOURCE_PATCHERS) {
            source = patcher.patchSodiumChunkShaderSource(source);
        }
        return source;
    }

    //programs cache their patch list. bump this when an appliesToShader answer can change after linking
    public static void invalidateInputPatches() {
        inputPatchesGeneration++;
    }

    public static int inputPatchesGeneration() {
        return inputPatchesGeneration;
    }

    public static List<IShaderInputPatch> inputPatchesFor(IShaderInputs shader) {
        List<IShaderInputPatch> applying = new ArrayList<>();
        for (var patch : INPUT_PATCHES) {
            if (patch.appliesToShader(shader)) applying.add(patch);
        }
        return applying;
    }

    public static void beforeLevelRender(Camera camera, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix, GpuBufferSlice terrainFog) {
        for (var listener : LEVEL_RENDER_LISTENERS) {
            listener.beforeLevelRender(camera, modelViewMatrix, projectionMatrix, terrainFog);
        }
    }

    public static void afterLevelRender(Camera camera, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix) {
        for (var listener : LEVEL_RENDER_LISTENERS) {
            listener.afterLevelRender(camera, modelViewMatrix, projectionMatrix);
        }
    }
}
