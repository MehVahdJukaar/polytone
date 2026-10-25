package net.mehvahdjukaar.polytone.api;

import net.mehvahdjukaar.polytone.Polytone;

import java.util.ArrayList;
import java.util.List;

public interface IShaderInputPatch {

    //a shader that declares any of these gets this patch
    List<String> getEnablingUniforms();

    void bindToShader(IShaderInputs inputs);

    default boolean appliesToShader(IShaderInputs shader) {
        for (String name : getEnablingUniforms()) {
            if (shader.hasUniform(name)) return true;
        }
        return false;
    }

    //for modifiers whose isUsedBy reads registrations that change after programs link
    static void invalidateEnabledLists() {
        Generation.value++;
    }

    static int enabledListsGeneration() {
        return Generation.value;
    }

    static List<IShaderInputPatch> allApplyingTo(IShaderInputs shader) {
        List<IShaderInputPatch> enabled = new ArrayList<>();
        for (var modifier : Polytone.SHADER_INPUT_PATCHES) {
            if (modifier.appliesToShader(shader)) enabled.add(modifier);
        }
        return enabled;
    }

    class Generation {
        private static int value = 0;
    }
}
