package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns post-chain activators (turn a {@link PostChain} on/off based on a condition) and the
 * {@code PolyGlobals} UBO that gets bound to every render pass.
 */
public class PostChainsManager extends ContentManager<PostChainActivator> {

    public static final String GLOBALS_NAME = "PolyGlobals";
    private PolytoneGlobalUniforms globalUniforms = null;

    private final List<PostChainActivator> activators = new ArrayList<>();
    // custom texture samplers keyed by pass fragment-shader id, registered by PostChainActivator
    private final Map<Identifier, List<Map<String, Identifier>>> samplersByShader = new HashMap<>();

    public PostChainsManager() {
        super("Post chain", () -> PostChainActivator.CODEC, "post_chains", "post_shaders");
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        AssetsFiles resources = super.prepare(sharedState);
        ShaderUniformsManager.registerExpressionUniformNames(resources.jsons());
        return resources;
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        synchronized (activators) {
            for (var j : parseEnabledJsons(resources.jsons(), ops)) {
                if (j != null) {
                    activators.add(j.getValue());
                }
            }
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        synchronized (activators) {
            for (var e : activators) e.close();
            activators.clear();
        }
        samplersByShader.clear();
    }

    private PolytoneGlobalUniforms getOrCreateUniforms() {
        if (globalUniforms == null) {
            globalUniforms = new PolytoneGlobalUniforms();
        }
        return globalUniforms;
    }

    public void setupExtraUniforms(RenderPass pass, Set<String> declaredUniforms) {
        // only bind PolyGlobals to passes whose shader actually declares the block (see GlRenderPassMixin)
        if (declaredUniforms.contains(GLOBALS_NAME)) {
            pass.setUniform(GLOBALS_NAME, getOrCreateUniforms().getSlice());
        }
    }

    /** External callers (PostChainActivator) register their custom samplers under a pass shader id. */
    public void registerSamplers(Identifier shaderId, Map<String, Identifier> samplers) {
        if (samplers.isEmpty()) return;
        samplersByShader.computeIfAbsent(shaderId, k -> new ArrayList<>()).add(samplers);
    }

    public void unregisterSamplers(Identifier shaderId, Map<String, Identifier> samplers) {
        List<Map<String, Identifier>> list = samplersByShader.get(shaderId);
        if (list != null) {
            list.remove(samplers);
            if (list.isEmpty()) samplersByShader.remove(shaderId);
        }
    }

    /**
     * Binds custom textures declared in a post chain's {@code samplers} map to any pass whose
     * pipeline fragment shader matches. Gated on {@code declaredUniforms} (which includes sampler
     * names) so we never bind a sampler the program doesn't declare — see {@code RenderPassMixin}.
     */
    public void bindExtraSamplers(RenderPass pass, RenderPipeline pipeline, Set<String> declaredUniforms) {
        if (samplersByShader.isEmpty()) return;
        List<Map<String, Identifier>> list = samplersByShader.get(pipeline.getFragmentShader());
        if (list == null) return;
        var texManager = Minecraft.getInstance().getTextureManager();
        // effect textures (noise/gradients) generally tile and look better filtered
        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
        for (Map<String, Identifier> samplers : list) {
            for (var e : samplers.entrySet()) {
                if (!declaredUniforms.contains(e.getKey())) continue;
                GpuTextureView view = texManager.getTexture(e.getValue()).getTextureView();
                pass.bindTexture(e.getKey(), view, sampler);
            }
        }
    }

    public void onClose() {
        synchronized (activators) {
            for (var e : activators) e.close();
        }
        if (globalUniforms != null) {
            globalUniforms.close();
            globalUniforms = null;
        }
        Polytone.POST_TARGETS.close();
    }

    public void captureLevelRendererParams(Matrix4f projectionMatrix, Matrix4f viewMatrix, float deltaTime) {
        Minecraft mc = Minecraft.getInstance();
        float angle = mc.levelRenderer.levelRenderState.skyRenderState.sunAngle;
        float dayTime = mc.level == null ? 0f : (float) (mc.level.getDayTime() % 24000L);
        getOrCreateUniforms().update(projectionMatrix, viewMatrix, angle, dayTime, deltaTime);
    }

    public void tick() {
        for (var a : activators) {
            a.refreshEnabled();
        }
    }

    public void addPostPass(int width, int height, LevelTargetBundle targets, FrameGraphBuilder frameGraphBuilder, GpuBufferSlice gpuBufferSlice, CameraRenderState cameraRenderState) {
        ShaderManager sm = Minecraft.getInstance().getShaderManager();
        Polytone.POST_TARGETS.ensureAllocated(width, height);
        PostChain.TargetBundle bundle = Polytone.POST_TARGETS.wrap(targets, frameGraphBuilder);
        synchronized (activators) {
            for (var a : activators) {
                PostChain pc = a.getPostChain(sm);
                if (pc != null) {
                    pc.addToFrame(frameGraphBuilder, width, height, bundle);
                }
            }
        }
    }
}
