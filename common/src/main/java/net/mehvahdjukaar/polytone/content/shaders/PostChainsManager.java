package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Owns post-chain activators (turn a {@link PostChain} on/off based on a condition) and the
 * {@code PolyGlobals} UBO that gets bound to every render pass.
 */
public class PostChainsManager extends JsonPartialReloader {

    public static final String GLOBALS_NAME = "PolyGlobals";
    private PolytoneGlobalUniforms globalUniforms = null;

    private final List<PostChainActivator> activators = new ArrayList<>();
    // custom texture samplers keyed by pass fragment-shader id, registered by PostChainActivator
    private final Map<Identifier, List<Map<String, Identifier>>> samplersByShader = new HashMap<>();

    public PostChainsManager() {
        super("post_chains", "post_shaders");
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(PreparableReloadListener.SharedState sharedState) {
        Map<Identifier, JsonElement> jsons = super.prepare(sharedState);
        ShaderUniformsManager.registerExpressionUniformNames(jsons);
        return jsons;
    }

    @Override
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        synchronized (activators) {
            for (var j : Parsed.batchParseOnlyEnabled(jsons, PostChainActivator.CODEC,
                    ops, "Post Chain Activators")) {
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
        // only bind PolyGlobals to passes whose shader actually declares the block (see RenderPassMixin)
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
        if (worldDepthSnapshot != null) {
            worldDepthSnapshot.destroyBuffers();
            worldDepthSnapshot = null;
        }
    }

    public void captureLevelRendererParams(Matrix4fc projectionMatrix, Matrix4fc viewMatrix) {
        Minecraft mc = Minecraft.getInstance();
        float angle = mc.levelRenderer.levelRenderState.skyRenderState.sunAngle;
        float dayTime = (float) ClientFrameTicker.getDayTime();
        getOrCreateUniforms().update(projectionMatrix, viewMatrix, angle, dayTime);
    }

    public void tick() {
        for (var a : activators) {
            a.refreshEnabled();
        }
    }

    /**
     * Standard placement: add every active chain to the level FrameGraph. Runs before the
     * first-person hand is drawn, so depth-reading chains don't see held items. Used when
     * {@code post_chains_after_hand} is off. See {@link #runAfterHand} for the default path.
     */
    public void addPostPass(int width, int height, LevelTargetBundle targets, FrameGraphBuilder frameGraphBuilder, GpuBufferSlice gpuBufferSlice, CameraRenderState cameraRenderState) {
        ShaderManager sm = Minecraft.getInstance().getShaderManager();
        synchronized (activators) {
            for (var a : activators) {
                PostChain pc = a.getPostChain(sm);
                if (pc != null) {
                    pc.addToFrame(frameGraphBuilder, width, height, targets);
                }
            }
        }
    }

    // ---- Depth-aware post chains -------------------------------------------------------------
    // Post chains used to be added to the level FrameGraph, which runs before the first-person
    // hand is drawn, so depth-reading effects (e.g. godrays) never saw the held item and would
    // leak past a raised shield. Instead we now run them right after the hand:
    //   1) snapshotWorldDepth() copies the finished world depth just before vanilla clears it to
    //      draw the hand in its own near projection;
    //   2) runAfterHand() folds that world depth back into the (hand-only) main depth via a
    //      LEQUAL depth-write pass -> min(world, hand) -> then processes each chain.
    // The chains sample the main target's depth exactly as before, so pack shaders are unchanged.

    private TextureTarget worldDepthSnapshot;
    private boolean worldDepthCaptured = false;

    /** Copy the world depth before vanilla clears it for the hand. No-op unless a chain is active. */
    public void snapshotWorldDepth(RenderTarget main) {
        worldDepthCaptured = false;
        if (!hasActiveChains()) return;
        ensureSnapshotSized(main.width, main.height);
        worldDepthSnapshot.copyDepthFrom(main);
        worldDepthCaptured = true;
    }

    /** Fold the saved world depth back into the main depth, then run every active chain. */
    public void runAfterHand(RenderTarget main, GraphicsResourceAllocator resourceAllocator) {
        if (!worldDepthCaptured) return;
        worldDepthCaptured = false;

        ShaderManager sm = Minecraft.getInstance().getShaderManager();
        List<PostChain> active = new ArrayList<>();
        synchronized (activators) {
            for (var a : activators) {
                PostChain pc = a.getPostChain(sm);
                if (pc != null) active.add(pc);
            }
        }
        if (active.isEmpty()) return;

        combineHandDepthIntoWorld(main);
        for (PostChain pc : active) {
            pc.process(main, resourceAllocator);
        }
    }

    private boolean hasActiveChains() {
        synchronized (activators) {
            for (var a : activators) {
                if (a.isOn()) return true;
            }
        }
        return false;
    }

    private void ensureSnapshotSized(int width, int height) {
        if (worldDepthSnapshot == null) {
            worldDepthSnapshot = new TextureTarget("Polytone World Depth Snapshot", width, height, true);
        } else if (worldDepthSnapshot.width != width || worldDepthSnapshot.height != height) {
            worldDepthSnapshot.resize(width, height);
        }
    }

    private void combineHandDepthIntoWorld(RenderTarget main) {
        GpuTextureView worldDepth = worldDepthSnapshot.getDepthTextureView();
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone depth combine",
                main.getColorTextureView(), OptionalInt.empty(),
                main.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(PolytoneRenderTypes.DEPTH_COMBINE_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("InSampler", worldDepth, sampler);
            pass.draw(0, 3);
        }
    }
}
