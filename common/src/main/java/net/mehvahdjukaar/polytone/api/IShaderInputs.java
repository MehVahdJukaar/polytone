package net.mehvahdjukaar.polytone.api;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

//both an effect instance or shaderInstance. exposes its input capabilities
public interface IShaderInputs {

    AbstractUniform getUniform(String name);

    boolean hasUniform(String uniformName);

    //listed in the shader json for veil nonsense
    boolean declaresUniform(String uniformName);

    void setSampler(String name, int textureId);

    int programId();

    //what shader_modifiers files are matched against. post effects have none
    List<ResourceLocation> getIds();

    static IShaderInputs ofShaderInstance(ShaderInstance shader, List<Uniform> jsonUniforms) {
        List<ResourceLocation> ids = new ArrayList<>(3);
        addCoreId(ids, shader.getName());
        addCoreId(ids, shader.getVertexProgram().getName());
        addCoreId(ids, shader.getFragmentProgram().getName());
        return new IShaderInputs() {
            @Override
            public AbstractUniform getUniform(String name) {
                return shader.safeGetUniform(name);
            }

            @Override
            public boolean hasUniform(String uniformName) {
                return shader.getUniform(uniformName) != null;
            }

            @Override
            public boolean declaresUniform(String uniformName) {
                for (Uniform u : jsonUniforms) {
                    if (u.getName().equals(uniformName)) return true;
                }
                return false;
            }

            @Override
            public void setSampler(String name, int textureId) {
                shader.setSampler(name, textureId);
            }

            @Override
            public int programId() {
                return shader.getId();
            }

            @Override
            public List<ResourceLocation> getIds() {
                return ids;
            }
        };
    }

    static IShaderInputs ofEffectInstance(EffectInstance effect) {
        return new IShaderInputs() {
            @Override
            public AbstractUniform getUniform(String name) {
                return effect.safeGetUniform(name);
            }

            @Override
            public boolean hasUniform(String uniformName) {
                return effect.getUniform(uniformName) != null;
            }

            @Override
            public boolean declaresUniform(String uniformName) {
                return hasUniform(uniformName);
            }

            @Override
            public void setSampler(String name, int textureId) {
                effect.setSampler(name, () -> textureId);
            }

            @Override
            public int programId() {
                return effect.getId();
            }

            @Override
            public List<ResourceLocation> getIds() {
                return List.of();
            }
        };
    }

    static IShaderInputs ofGenericProgram(int program) {
        AbstractUniform noUniform = new AbstractUniform();
        return new IShaderInputs() {
            @Override
            public AbstractUniform getUniform(String name) {
                return noUniform;
            }

            @Override
            public boolean hasUniform(String uniformName) {
                return false;
            }

            @Override
            public boolean declaresUniform(String uniformName) {
                return false;
            }

            @Override
            public void setSampler(String name, int textureId) {
            }

            @Override
            public int programId() {
                return program;
            }

            @Override
            public List<ResourceLocation> getIds() {
                return List.of();
            }
        };
    }

    private static void addCoreId(List<ResourceLocation> ids, String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null) return;
        id = id.withPrefix("core/");
        if (!ids.contains(id)) ids.add(id);
    }
}
