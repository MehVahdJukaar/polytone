package net.mehvahdjukaar.polytone.api;

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
}
