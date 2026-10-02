package net.mehvahdjukaar.polytone.content.viewpoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// renders only while an active chain lists it in uses_viewpoints and its condition passes
public class ViewpointsManager extends ContentManager<Viewpoint> {

    private static final String[] SAMPLER_KEYS = {"depth_sampler", "color_sampler"};

    private final Map<Identifier, Viewpoint> viewpoints = new LinkedHashMap<>();
    // kept across reloads where the id survives
    private final Map<Identifier, ViewpointInstance> instances = new HashMap<>();
    // read raw at prepare, programs can compile before parsing and need these to bind the samplers
    private volatile List<String> declaredSamplerNames = List.of();
    private GpuBuffer emptyUniformBlock = null;

    public ViewpointsManager() {
        super(Spec.of("Viewpoint", () -> Viewpoint.CODEC)
                .wikiPage("Viewpoints")
                .folders("viewpoints"));
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        AssetsFiles resources = super.prepare(sharedState);
        // needed before programs compile, parsing only happens later with the level
        List<String> declared = new ArrayList<>();
        for (JsonElement e : resources.jsons().values()) {
            if (!(e instanceof JsonObject obj)) continue;
            if (obj.get("uniform_block") instanceof JsonPrimitive p && p.isString()) {
                PolytoneBuiltInUniformsSet.register(p.getAsString());
            }
            for (String key : SAMPLER_KEYS) {
                if (obj.get(key) instanceof JsonPrimitive p && p.isString()
                        && !p.getAsString().isEmpty() && !declared.contains(p.getAsString())) {
                    declared.add(p.getAsString());
                }
            }
        }
        declaredSamplerNames = List.copyOf(declared);
        return resources;
    }

    public List<String> declaredSamplerNames() {
        return declaredSamplerNames;
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var j : parseEnabledJsons(resources.jsons(), ops)) {
            if (j == null) continue;
            Viewpoint vp = j.getValue();
            if (!vp.isSamplable()) {
                Polytone.LOGGER.warn("Viewpoint {} declares neither depth_sampler nor color_sampler, so nothing can ever read it - skipping", j.getKey());
                continue;
            }
            if (vp.hasUniformBlock()) PolytoneBuiltInUniformsSet.register(vp.uniformBlock());
            viewpoints.put(j.getKey(), vp);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        viewpoints.clear();
        // not closed here, surviving ids reuse their textures. orphans go in renderActive()
    }

    // missing texture until rendered, a declared sampler can't be left unbound. first viewpoint naming it wins
    public void bindSamplers(RenderPass pass, Set<String> declaredUniforms) {
        if (viewpoints.isEmpty()) return;
        Set<String> bound = new HashSet<>();
        for (var e : viewpoints.entrySet()) {
            Viewpoint vp = e.getValue();
            ViewpointInstance inst = instances.get(e.getKey());
            // not while it renders: its own terrain may be drawn with a shader sampling it
            boolean readable = inst != null && !inst.isRendering();
            bindSampler(pass, declaredUniforms, bound, vp.depthSampler(), readable ? inst.getDepthTexture() : null);
            bindSampler(pass, declaredUniforms, bound, vp.colorSampler(), readable ? inst.getColorTexture() : null);
        }
    }

    private static void bindSampler(RenderPass pass, Set<String> declaredUniforms, Set<String> bound,
                                    String name, @Nullable GpuTextureView texture) {
        if (!declaredUniforms.contains(name) || !bound.add(name)) return;
        if (texture == null) {
            texture = Minecraft.getInstance().getTextureManager()
                    .getTexture(MissingTextureAtlasSprite.getLocation()).getTextureView();
        }
        pass.bindTexture(name, texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }

    // zeros until the viewpoint has rendered once
    public void bindUniformBlocks(RenderPass pass, Set<String> declaredUniforms) {
        if (viewpoints.isEmpty()) return;
        for (var e : viewpoints.entrySet()) {
            Viewpoint vp = e.getValue();
            if (!vp.hasUniformBlock() || !declaredUniforms.contains(vp.uniformBlock())) continue;
            ViewpointInstance inst = instances.get(e.getKey());
            GpuBufferSlice slice = inst == null ? null : inst.getUniformsSlice();
            pass.setUniform(vp.uniformBlock(), slice != null ? slice : emptyUniformBlock());
        }
    }

    private GpuBufferSlice emptyUniformBlock() {
        if (emptyUniformBlock == null) {
            emptyUniformBlock = RenderSystem.getDevice().createBuffer(() -> "Polytone empty viewpoint UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, ViewpointUniforms.UBO_SIZE);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                RenderSystem.getDevice().createCommandEncoder()
                        .writeToBuffer(emptyUniformBlock.slice(), stack.calloc(ViewpointUniforms.UBO_SIZE));
            }
        }
        return emptyUniformBlock.slice();
    }

    public boolean isEmpty() {
        return viewpoints.isEmpty();
    }

    // LevelRenderer.render HEAD: no render pass open and last frame's meshes are still current
    public void renderActive(GpuBufferSlice shaderFog, Camera cam) {
        if (viewpoints.isEmpty()) return;
        var wanted = Polytone.POST_CHAINS.wantedViewpoints();
        if (wanted.isEmpty()) return;

        for (var e : viewpoints.entrySet()) {
            if (!wanted.contains(e.getKey())) continue;
            if (!e.getValue().isActive()) continue;
            instances.computeIfAbsent(e.getKey(), k -> new ViewpointInstance())
                    .renderIfNeeded(e.getValue(), shaderFog, cam);
        }

        // drop GPU state for viewpoints a reload removed
        if (instances.size() > viewpoints.size()) {
            instances.entrySet().removeIf(e -> {
                if (viewpoints.containsKey(e.getKey())) return false;
                e.getValue().close();
                return true;
            });
        }
    }

    public void onClose() {
        for (ViewpointInstance inst : instances.values()) inst.close();
        instances.clear();
        if (emptyUniformBlock != null) {
            emptyUniformBlock.close();
            emptyUniformBlock = null;
        }
    }
}
