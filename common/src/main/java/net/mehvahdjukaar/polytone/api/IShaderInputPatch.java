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

    static List<IShaderInputPatch> allApplyingTo(IShaderInputs shader) {
        List<IShaderInputPatch> enabled = new ArrayList<>();
        for (var binder : Polytone.SHADER_INPUT_PATCHES) {
            if (binder.appliesToShader(shader)) enabled.add(binder);
        }
        return enabled;
    }
}
