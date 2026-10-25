package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.api.IShaderSourcePatch;
import net.mehvahdjukaar.polytone.api.LevelRenderListener;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

//all render nonsense stuff goes through here
public class PolytoneRenderHooks {

    private static final List<IShaderSourcePatch> SOURCE_PATCHERS = new ArrayList<>(List.of(Polytone.COLORED_LIGHTS));
    private static final List<IShaderInputPatch> INPUT_PATCHES = new ArrayList<>(List.of(
            Polytone.GLOBAL_UNIFORMS, Polytone.VOXEL_VOLUME, Polytone.COLORED_LIGHTS, Polytone.SHADER_UNIFORMS));
    private static final List<LevelRenderListener> LEVEL_RENDER_LISTENERS = new ArrayList<>(List.of(Polytone.VOXEL_VOLUME));

    public static void addSourcePatcher(IShaderSourcePatch patcher) {
        SOURCE_PATCHERS.add(patcher);
    }

    public static void addInputPatch(IShaderInputPatch patch) {
        INPUT_PATCHES.add(patch);
    }

    public static void addLevelRenderListener(LevelRenderListener listener) {
        LEVEL_RENDER_LISTENERS.add(listener);
    }

    public static String patchShader(String source) {
        for (var patcher : SOURCE_PATCHERS) {
            source = patcher.patchShaderSource(source);
        }
        return source;
    }

    public static String patchSodiumChunkShader(String source) {
        for (var patcher : SOURCE_PATCHERS) {
            source = patcher.patchSodiumChunkShaderSource(source);
        }
        return source;
    }

    public static List<IShaderInputPatch> inputPatchesFor(IShaderInputs shader) {
        List<IShaderInputPatch> applying = new ArrayList<>();
        for (var patch : INPUT_PATCHES) {
            if (patch.appliesToShader(shader)) applying.add(patch);
        }
        return applying;
    }

    public static void afterLevelRender(Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        for (var listener : LEVEL_RENDER_LISTENERS) {
            listener.afterLevelRender(camera, frustumMatrix, projectionMatrix);
        }
    }
}
