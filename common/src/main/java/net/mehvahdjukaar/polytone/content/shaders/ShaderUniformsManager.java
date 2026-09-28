package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderUniformsManager extends ContentManager<Map<String, ISimpleExp>> {

    private static final Codec<Map<String, ISimpleExp>> CODEC = Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC);

    private Map<ResourceLocation, List<ExpressionUniform>> parsed = Map.of();
    //render thread only reads this, so it gets swapped as a whole
    private volatile Map<ResourceLocation, List<ExpressionUniform>> uniformsByShader = Map.of();

    public ShaderUniformsManager() {
        super(Spec.of("Shader uniforms", () -> CODEC)
                .wikiPage("Shaders")
                .folders("shader_modifiers"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        Map<ResourceLocation, List<ExpressionUniform>> byShader = new HashMap<>();
        for (var e : parseEnabledJsons(resources.jsons(), ops)) {
            List<ExpressionUniform> list = byShader.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
            e.getValue().forEach((name, exp) -> list.add(new ExpressionUniform(name, exp)));
        }
        parsed = byShader;
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        uniformsByShader = parsed;
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        parsed = Map.of();
        uniformsByShader = Map.of();
    }

    // once per frame. apply runs many times a frame and just copies the values
    public void updateAll() {
        for (List<ExpressionUniform> list : uniformsByShader.values()) {
            for (ExpressionUniform u : list) u.value = (float) u.expression.evaluate();
        }
    }

    public void applyTo(ShaderInstance shader, List<ResourceLocation> shaderIds) {
        var byShader = uniformsByShader;
        if (byShader.isEmpty()) return;
        for (ResourceLocation id : shaderIds) {
            List<ExpressionUniform> list = byShader.get(id);
            if (list == null) continue;
            for (ExpressionUniform u : list) {
                Uniform uniform = shader.getUniform(u.name);
                if (uniform != null) uniform.set(u.value);
            }
        }
    }

    public static List<ResourceLocation> targetIdsOf(ShaderInstance shader) {
        List<ResourceLocation> ids = new ArrayList<>(3);
        addCoreId(ids, shader.getName());
        addCoreId(ids, shader.getVertexProgram().getName());
        addCoreId(ids, shader.getFragmentProgram().getName());
        return ids;
    }

    private static void addCoreId(List<ResourceLocation> ids, String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null) return;
        id = id.withPrefix("core/");
        if (!ids.contains(id)) ids.add(id);
    }


    private static class ExpressionUniform {
        private final String name;
        private final ISimpleExp expression;
        private float value;

        ExpressionUniform(String name, ISimpleExp expression) {
            this.name = name;
            this.expression = expression;
        }
    }
}
