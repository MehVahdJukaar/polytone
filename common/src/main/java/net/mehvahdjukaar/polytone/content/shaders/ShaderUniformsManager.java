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
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Expression-driven float UBOs bound to any pipeline whose vertex or fragment shader id matches. The json path
// under polytone/shader_modifiers is the target shader id.
public class ShaderUniformsManager extends ContentManager<ExpressionUniformBuffers> {

    private final List<ExpressionUniformBuffers> owned = new ArrayList<>();
    private final Map<Identifier, List<ExpressionUniformBuffers>> byShader = new HashMap<>();
    private final Map<Identifier, List<ExpressionUniformBuffers>> byPostPassPipeline = new HashMap<>();

    public ShaderUniformsManager() {
        super("Shader uniforms", () -> SchemaCodec.wrap(ExpressionUniformBuffers.CODEC), "shader_modifiers");
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        AssetsFiles resources = super.prepare(sharedState);
        registerUniformNames(resources.jsons());
        return resources;
    }

    // post chain files: block names are the expression_uniforms keys
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

    // shader_modifiers files: block names are the top-level keys
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
            byPostPassPipeline.clear();
        }
    }

    public void onClose() {
        synchronized (owned) {
            for (var b : owned) b.close();
        }
    }

    public void registerExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        byShader.computeIfAbsent(shaderId, k -> new ArrayList<>()).add(buffers);
    }

    public void unregisterExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        removeFrom(byShader, shaderId, buffers);
    }

    public void registerOnPostPass(Identifier pipelineLocation, ExpressionUniformBuffers buffers) {
        byPostPassPipeline.computeIfAbsent(pipelineLocation, k -> new ArrayList<>()).add(buffers);
    }

    public void unregisterFromPostPass(Identifier pipelineLocation, ExpressionUniformBuffers buffers) {
        removeFrom(byPostPassPipeline, pipelineLocation, buffers);
    }

    private static void removeFrom(Map<Identifier, List<ExpressionUniformBuffers>> map, Identifier key,
                                   ExpressionUniformBuffers buffers) {
        List<ExpressionUniformBuffers> list = map.get(key);
        if (list != null) {
            list.remove(buffers);
            if (list.isEmpty()) map.remove(key);
        }
    }

    // once per frame while no render pass is open; tryApply only binds what this uploaded
    public void updateAll() {
        if (!hasAnyRegistered()) return;
        Set<ExpressionUniformBuffers> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<ExpressionUniformBuffers> list : byShader.values()) {
            for (ExpressionUniformBuffers b : list) {
                if (seen.add(b)) b.update();
            }
        }
        for (List<ExpressionUniformBuffers> list : byPostPassPipeline.values()) {
            for (ExpressionUniformBuffers b : list) {
                if (seen.add(b)) b.update();
            }
        }
    }

    public void bindToCurrentGlProgram() {
        if (byShader.isEmpty()) return;
        int program = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        int point = 1;
        Set<ExpressionUniformBuffers> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<ExpressionUniformBuffers> list : byShader.values()) {
            for (ExpressionUniformBuffers b : list) {
                if (seen.add(b)) point = b.bindBlocksToProgram(program, point);
            }
        }
    }

    public boolean hasAnyRegistered() {
        return !byShader.isEmpty() || !byPostPassPipeline.isEmpty();
    }

    public void tryApply(RenderPass pass, RenderPipeline pipeline, Set<String> declaredUniforms) {
        if (!byPostPassPipeline.isEmpty()) {
            List<ExpressionUniformBuffers> postPassBuffers = byPostPassPipeline.get(pipeline.getLocation());
            if (postPassBuffers != null) {
                for (ExpressionUniformBuffers b : postPassBuffers) b.bind(pass, declaredUniforms);
            }
        }
        if (byShader.isEmpty()) return;
        List<ExpressionUniformBuffers> list = byShader.get(pipeline.getFragmentShader());
        if (list == null) list = byShader.get(pipeline.getVertexShader());
        if (list == null) return;
        for (ExpressionUniformBuffers b : list) {
            b.bind(pass, declaredUniforms);
        }
    }
}
