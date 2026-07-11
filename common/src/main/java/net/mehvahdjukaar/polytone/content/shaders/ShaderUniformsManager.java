package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds expression-driven UBO uniforms to any pipeline whose vertex or fragment shader id
 * matches a key in {@link #byShader}.
 *
 * <p>JSONs live under {@code polytone/shader_effects/<target-shader-path>.json}. The file
 * path determines the target shader id (standard polytone convention — see e.g.
 * {@code BlockPropertiesManager}). The JSON body itself is just an
 * {@link ExpressionUniformBuffers} map of UBO-block-name → expression.
 *
 * <p>Example: {@code assets/minecraft/polytone/shader_effects/core/rendertype_solid.json}
 * targets shader {@code minecraft:core/rendertype_solid}.
 */
public class ShaderUniformsManager extends ContentManager<ExpressionUniformBuffers> {

    private final List<ExpressionUniformBuffers> owned = new ArrayList<>();
    private final Map<Identifier, List<ExpressionUniformBuffers>> byShader = new HashMap<>();

    public ShaderUniformsManager() {
        super("Shader uniforms", () -> SchemaCodec.wrap(ExpressionUniformBuffers.CODEC), "shader_effects");
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        AssetsFiles resources = super.prepare(sharedState);
        registerUniformNames(resources.jsons());
        return resources;
    }

    /** Collects UBO-block names from {@code expression_uniforms} JSON objects (for activator files). */
    static void registerExpressionUniformNames(Map<Identifier, JsonElement> jsons) {
        for (var e : jsons.values()) {
            if (e == null || !e.isJsonObject()) continue;
            JsonElement uniforms = e.getAsJsonObject().get("expression_uniforms");
            if (uniforms instanceof JsonObject obj) {
                for (String name : obj.keySet()) {
                    PolytoneBuiltInUniformsSet.register(name);
                }
            }
        }
    }

    /** Collects UBO-block names directly from the top-level JSON keys of shader_effects files. */
    private static void registerUniformNames(Map<Identifier, JsonElement> jsons) {
        for (var e : jsons.values()) {
            if (e instanceof JsonObject obj) {
                for (String name : obj.keySet()) {
                    PolytoneBuiltInUniformsSet.register(name);
                }
            }
        }
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        synchronized (owned) {
            for (var j : parseEnabledJsons(resources.jsons(), ops)) {
                if (j == null) continue;
                Identifier targetShader = j.getKey();
                ExpressionUniformBuffers buffers = j.getValue();
                buffers.ensureInitialized("Polytone shader expr uniform");
                owned.add(buffers);
                registerExternal(targetShader, buffers);
            }
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        synchronized (owned) {
            for (var b : owned) b.close();
            owned.clear();
            byShader.clear();
        }
    }

    public void onClose() {
        synchronized (owned) {
            for (var b : owned) b.close();
        }
    }

    /** External callers (e.g. PostChainActivator) bind their buffers under a shader id. */
    public void registerExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        byShader.computeIfAbsent(shaderId, k -> new ArrayList<>()).add(buffers);
    }

    public void unregisterExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        List<ExpressionUniformBuffers> list = byShader.get(shaderId);
        if (list != null) {
            list.remove(buffers);
            if (list.isEmpty()) byShader.remove(shaderId);
        }
    }

    public void tryApply(RenderPass pass, RenderPipeline pipeline) {
        if (byShader.isEmpty()) return;
        List<ExpressionUniformBuffers> list = byShader.get(pipeline.getFragmentShader());
        if (list == null) list = byShader.get(pipeline.getVertexShader());
        if (list == null) return;
        for (ExpressionUniformBuffers b : list) {
            b.update();
            b.bind(pass);
        }
    }
}
