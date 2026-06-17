package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class ShaderUniformsManager extends JsonPartialReloader {

    private final List<ExpressionUniformBuffers> owned = new ArrayList<>();
    private final Map<Identifier, List<ExpressionUniformBuffers>> byShader = new HashMap<>();

    public ShaderUniformsManager() {
        super("shader_modifiers");
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(PreparableReloadListener.SharedState sharedState) {
        Map<Identifier, JsonElement> jsons = super.prepare(sharedState);
        registerUniformNames(jsons);
        return jsons;
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
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        synchronized (owned) {
            for (var j : Parsed.batchParseOnlyEnabled(jsons, ExpressionUniformBuffers.CODEC,
                    ops, "Shader Uniform Effects")) {
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

    /**
     * Evaluates all expressions and uploads their UBO buffers. MUST be called once per frame from a
     * point where no render pass is open (GPU buffer writes are illegal mid-pass), e.g. at
     * {@code renderLevel} HEAD. {@link #tryApply} then only binds the already-updated buffers.
     */
    public void updateAll() {
        if (byShader.isEmpty()) return;
        // the same buffers can be registered under several shader ids; update each only once
        Set<ExpressionUniformBuffers> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<ExpressionUniformBuffers> list : byShader.values()) {
            for (ExpressionUniformBuffers b : list) {
                if (seen.add(b)) b.update();
            }
        }
    }

    public void tryApply(RenderPass pass, RenderPipeline pipeline, Set<String> declaredUniforms) {
        if (byShader.isEmpty()) return;
        List<ExpressionUniformBuffers> list = byShader.get(pipeline.getFragmentShader());
        if (list == null) list = byShader.get(pipeline.getVertexShader());
        if (list == null) return;
        for (ExpressionUniformBuffers b : list) {
            b.bind(pass, declaredUniforms);
        }
    }
}
