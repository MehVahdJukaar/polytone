package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.Polytone;

import java.util.ArrayList;
import java.util.List;

public interface IShaderModifier {

    //a shader that declares any of these gets this modifier
    List<String> getEnablingUniforms();

    void bindTo(IShader inputs);

    default boolean isUsedBy(IShader shader) {
        for (String name : getEnablingUniforms()) {
            if (shader.hasUniform(name)) return true;
        }
        return false;
    }

    static List<IShaderModifier> usedBy(IShader shader) {
        List<IShaderModifier> enabled = new ArrayList<>();
        for (var modifier : Polytone.SHADER_MODIFIERS) {
            if (modifier.isUsedBy(shader)) enabled.add(modifier);
        }
        return enabled;
    }
}
