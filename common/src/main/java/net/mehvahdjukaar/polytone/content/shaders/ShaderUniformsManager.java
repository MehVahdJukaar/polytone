package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShaderUniformsManager extends ContentManager<ExpressionUniformBuffers> {

    private static final String[] SHADER_EXTENSIONS = {".vsh", ".fsh"};

    private final List<ExpressionUniformBuffers> owned = new ArrayList<>();
    private final Map<Identifier, List<ExpressionUniformBuffers>> byShader = new HashMap<>();
    private final Map<Identifier, List<ExpressionUniformBuffers>> byPostPassPipeline = new HashMap<>();

    // written off thread during prepare
    private volatile Set<String> modifierBlockNames = Set.of();
    private final Map<PipelineKey, Set<String>> pendingChecks = new LinkedHashMap<>();
    private final Set<String> warnedPairs = new HashSet<>();

    public ShaderUniformsManager() {
        super("Shader uniforms", () -> SchemaCodec.wrap(ExpressionUniformBuffers.CODEC), "shader_modifiers");
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        AssetsFiles resources = super.prepare(sharedState);
        registerUniformNames(resources.jsons());
        warnAboutOrphanedTargets(sharedState.resourceManager(), resources.jsons());
        return resources;
    }

    // a modifier for a shader nothing provides never binds and nothing else would log it
    private static void warnAboutOrphanedTargets(ResourceManager resourceManager,
                                                 Map<Identifier, JsonElement> jsons) {
        for (Identifier shaderId : jsons.keySet()) {
            if (shaderExists(resourceManager, shaderId)) continue;
            Polytone.LOGGER.warn(
                    "Polytone shader modifier '{}' targets a shader that no loaded pack provides " +
                    "(looked for shaders/{}.vsh and .fsh). It will never bind, silently. Was the shader " +
                    "renamed in this Minecraft version, or does it belong to a mod that isn't installed?",
                    shaderId, shaderId.getPath());
        }
    }

    private static boolean shaderExists(ResourceManager resourceManager, Identifier shaderId) {
        for (String extension : SHADER_EXTENSIONS) {
            Identifier file = shaderId.withPath("shaders/" + shaderId.getPath() + extension);
            if (resourceManager.getResource(file).isPresent()) return true;
        }
        return false;
    }

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

    private void registerUniformNames(Map<Identifier, JsonElement> jsons) {
        Set<String> names = new HashSet<>();
        for (var e : jsons.values()) {
            if (e instanceof JsonObject obj) {
                for (String name : obj.keySet()) {
                    PolytoneBuiltInUniformsSet.register(name);
                    names.add(name);
                }
            }
        }
        // only names a pack declared, so vanilla and sodium blocks are never flagged
        modifierBlockNames = Set.copyOf(names);
    }

    // checked the next frame, pipelines can link before the modifiers are registered
    public void onPipelineLinked(RenderPipeline pipeline, Set<String> declaredBlocks) {
        if (declaredBlocks.isEmpty() || modifierBlockNames.isEmpty()) return;
        synchronized (pendingChecks) {
            pendingChecks.put(new PipelineKey(pipeline.getVertexShader(), pipeline.getFragmentShader(),
                    pipeline.getLocation()), Set.copyOf(declaredBlocks));
        }
    }

    private void runPendingChecks() {
        List<Map.Entry<PipelineKey, Set<String>>> pending;
        synchronized (pendingChecks) {
            if (pendingChecks.isEmpty()) return;
            pending = new ArrayList<>(pendingChecks.entrySet());
            pendingChecks.clear();
        }
        for (var entry : pending) {
            PipelineKey key = entry.getKey();
            Set<String> declared = entry.getValue();
            Set<String> supplied = new HashSet<>();
            for (Identifier shaderId : key.shaderIds()) {
                List<ExpressionUniformBuffers> list = byShader.get(shaderId);
                if (list == null) continue;
                for (ExpressionUniformBuffers b : list) supplied.addAll(b.getExpressions().keySet());
            }
            List<ExpressionUniformBuffers> postPass = byPostPassPipeline.get(key.location());
            if (postPass != null) {
                for (ExpressionUniformBuffers b : postPass) supplied.addAll(b.getExpressions().keySet());
            }

            // declared by the shader, owned by a modifier, supplied by nobody
            for (String name : declared) {
                if (!modifierBlockNames.contains(name) || supplied.contains(name)) continue;
                if (!warnedPairs.add(key + "|" + name)) continue;
                Polytone.LOGGER.warn(
                        "Shader {} declares Polytone expression block '{}', but no shader_modifiers file " +
                        "supplies it for that shader, so it is never written. A modifier applies only to " +
                        "the shader id its file is named after. '{}' is currently supplied for: {}",
                        key, name, name, suppliersOf(name));
            }

            // not the inverse: blocks behind an #ifdef get optimised out, that would warn on healthy packs
        }
    }

    // where it is supplied, to point at the mismatch
    private List<Identifier> suppliersOf(String blockName) {
        List<Identifier> ids = new ArrayList<>();
        for (var e : byShader.entrySet()) {
            for (ExpressionUniformBuffers b : e.getValue()) {
                if (b.getExpressions().containsKey(blockName)) {
                    ids.add(e.getKey());
                    break;
                }
            }
        }
        return ids;
    }

    private record PipelineKey(Identifier vertexShader, Identifier fragmentShader, Identifier location) {
        List<Identifier> shaderIds() {
            return vertexShader.equals(fragmentShader) ? List.of(vertexShader)
                    : List.of(vertexShader, fragmentShader);
        }

        @Override
        public String toString() {
            return vertexShader.equals(fragmentShader) ? vertexShader.toString()
                    : vertexShader + " / " + fragmentShader;
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
        synchronized (pendingChecks) {
            pendingChecks.clear();
        }
        warnedPairs.clear();
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
        runPendingChecks();
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
