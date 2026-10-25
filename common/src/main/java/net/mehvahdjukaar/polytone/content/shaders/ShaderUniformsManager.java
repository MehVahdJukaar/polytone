package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShaderUniformsManager extends ContentManager<ExpressionUniformBuffers> implements IShaderInputPatch {

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

    public static void registerExpressionUniformNames(Map<Identifier, JsonElement> jsons) {
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
        IShaderInputPatch.invalidateEnabledLists();
    }

    public void onClose() {
        synchronized (owned) {
            for (var b : owned) b.close();
        }
    }

    // External callers (e.g. PostChainActivator) bind their buffers under a shader id.
    public void registerExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        byShader.computeIfAbsent(shaderId, k -> new ArrayList<>()).add(buffers);
        IShaderInputPatch.invalidateEnabledLists();
    }

    public void unregisterExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        removeFrom(byShader, shaderId, buffers);
    }

    public void registerOnPostPass(Identifier pipelineLocation, ExpressionUniformBuffers buffers) {
        byPostPassPipeline.computeIfAbsent(pipelineLocation, k -> new ArrayList<>()).add(buffers);
        IShaderInputPatch.invalidateEnabledLists();
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
        IShaderInputPatch.invalidateEnabledLists();
    }

    //MUST be called once per frame from a point where no render pass is open
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

    // Whether any pack (or post chain) registered expression uniforms at all
    public boolean hasAnyRegistered() {
        return !byShader.isEmpty() || !byPostPassPipeline.isEmpty();
    }

    @Override
    public List<String> getEnablingUniforms() {
        return List.of();
    }

    @Override
    public boolean appliesToShader(IShaderInputs shader) {
        return buffersFor(shader.pipeline()) != null || byPostPassPipeline.containsKey(shader.pipeline().getLocation());
    }

    @Override
    public void bindToShader(IShaderInputs shader) {
        RenderPipeline pipeline = shader.pipeline();
        List<ExpressionUniformBuffers> postPassBuffers = byPostPassPipeline.get(pipeline.getLocation());
        if (postPassBuffers != null) {
            for (ExpressionUniformBuffers b : postPassBuffers) b.bind(shader);
        }
        List<ExpressionUniformBuffers> list = buffersFor(pipeline);
        if (list == null) return;
        for (ExpressionUniformBuffers b : list) {
            b.bind(shader);
        }
    }

    @Nullable
    private List<ExpressionUniformBuffers> buffersFor(RenderPipeline pipeline) {
        List<ExpressionUniformBuffers> list = byShader.get(pipeline.getFragmentShader());
        return list != null ? list : byShader.get(pipeline.getVertexShader());
    }
}
