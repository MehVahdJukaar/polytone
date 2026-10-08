package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderUniformsManager extends ContentManager<ExpressionUniforms> implements IShaderModifier {

    private Map<ResourceLocation, ExpressionUniforms> parsed = Map.of();
    //render thread only reads this, so it gets swapped as a whole
    private volatile Map<ResourceLocation, ExpressionUniforms> uniformsByShader = Map.of();

    public ShaderUniformsManager() {
        super(Spec.of("Shader uniforms", () -> ExpressionUniforms.CODEC)
                .wikiPage("Shaders")
                .folders("shader_modifiers"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        Map<ResourceLocation, ExpressionUniforms> byShader = new HashMap<>();
        for (var e : parseEnabledJsons(resources.jsons(), ops)) {
            byShader.put(e.getKey(), e.getValue());
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
        for (ExpressionUniforms u : uniformsByShader.values()) u.evaluate();
    }

    @Override
    public List<String> getEnablingUniforms() {
        return List.of();
    }

    //applied to all
    @Override
    public boolean isUsedBy(IShader shader) {
        return true;
    }

    @Override
    public void bindTo(IShader shader) {
        var byShader = uniformsByShader;
        if (byShader.isEmpty()) return;
        for (ResourceLocation id : shader.getIds()) {
            ExpressionUniforms uniforms = byShader.get(id);
            if (uniforms != null) uniforms.bindTo(shader);
        }
    }

}
