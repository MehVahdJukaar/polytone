package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.Polytone;

import java.util.ArrayList;
import java.util.List;

public interface IShaderModifier {

    //a shader that declares any of these gets this modifier
    List<String> getEnablingUniforms();

    void bindTo(IShader inputs);

    default boolean isEnabledFor(IShader shader) {
        for (String name : getEnablingUniforms()) {
            if (shader.hasUniform(name)) return true;
        }
        return false;
    }

    //for modifiers whose isEnabledFor reads registrations that change after programs link
    static void invalidateEnabledLists() {
        Generation.value++;
    }

    static int enabledListsGeneration() {
        return Generation.value;
    }

    static List<IShaderModifier> enabledFor(IShader shader) {
        List<IShaderModifier> enabled = new ArrayList<>();
        for (var modifier : Polytone.SHADER_MODIFIERS) {
            if (modifier.isEnabledFor(shader)) enabled.add(modifier);
        }
        return enabled;
    }

    class Generation {
        private static int value = 0;
    }
}
