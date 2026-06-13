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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreShadersManager extends JsonPartialReloader {

    private record ShaderBinding(@Nullable CoreShaderEffect effect, ExpressionUniformBuffers buffers) {
        boolean isEnabled() {
            return effect == null || effect.isOn();
        }
    }

    private final List<CoreShaderEffect> effects = new ArrayList<>();
    private final Map<Identifier, List<ShaderBinding>> byShader = new HashMap<>();

    public CoreShadersManager() {
        super("core_shaders");
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(PreparableReloadListener.SharedState sharedState) {
        Map<Identifier, JsonElement> jsons = super.prepare(sharedState);
        registerUniformNames(jsons);
        return jsons;
    }

    static void registerUniformNames(Map<Identifier, JsonElement> jsons) {
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

    @Override
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        synchronized (effects) {
            for (var j : Parsed.batchParseOnlyEnabled(jsons, CoreShaderEffect.CODEC,
                    ops, "Core Shader Effects")) {
                if (j != null) {
                    CoreShaderEffect eff = j.getValue();
                    effects.add(eff);
                    eff.buffers().ensureInitialized("Polytone core expr uniform");
                    byShader.computeIfAbsent(eff.shader(), k -> new ArrayList<>())
                            .add(new ShaderBinding(eff, eff.buffers()));
                }
            }
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        synchronized (effects) {
            for (var e : effects) e.buffers().close();
            effects.clear();
            byShader.clear();
        }
    }

    public void tick() {
        synchronized (effects) {
            for (var e : effects) e.refreshEnabled();
        }
    }

    public void onClose() {
        synchronized (effects) {
            for (var e : effects) e.buffers().close();
        }
    }

    /** External callers (e.g. PostChainEffect) bind their buffers under a shader id. */
    public void registerExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        byShader.computeIfAbsent(shaderId, k -> new ArrayList<>())
                .add(new ShaderBinding(null, buffers));
    }

    public void unregisterExternal(Identifier shaderId, ExpressionUniformBuffers buffers) {
        List<ShaderBinding> list = byShader.get(shaderId);
        if (list != null) {
            list.removeIf(b -> b.effect == null && b.buffers == buffers);
            if (list.isEmpty()) byShader.remove(shaderId);
        }
    }

    public void tryApply(RenderPass pass, RenderPipeline pipeline) {
        if (byShader.isEmpty()) return;
        List<ShaderBinding> list = byShader.get(pipeline.getFragmentShader());
        if (list == null) list = byShader.get(pipeline.getVertexShader());
        if (list == null) return;
        for (ShaderBinding b : list) {
            if (!b.isEnabled()) continue;
            b.buffers.update();
            b.buffers.bind(pass);
        }
    }
}
