package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.GpuFormat;
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
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

// Post chains toggled by an expression, plus the PolyGlobals / PolyShadow blocks and the InShadow sampler
// that get bound to any pass whose program declares them.
public class PostChainsManager extends ContentManager<PostChainActivator> {

    public static final String GLOBALS_NAME = "PolyGlobals";
    public static final String SHADOW_UBO_NAME = "PolyShadow";
    public static final String SHADOW_SAMPLER_NAME = "InShadow";
    // Samplers we bind by name that no pipeline declares. GlProgramMixin gives them a texture unit on
    // programs that use them, otherwise they'd sit on unit 0 and read the scene texture.
    public static final List<String> DYNAMIC_SAMPLERS = List.of(SHADOW_SAMPLER_NAME);

    // Latched at program link time and never cleared: with no pack using our blocks we skip the per-frame
    // upload and per-draw binds entirely, and un-latching would need every program re-linked.
    private static volatile boolean globalsDeclared = false;
    private static volatile boolean shadowUboDeclared = false;
    private static volatile boolean shadowSamplerDeclared = false;

    private PolytoneGlobalUniforms globalUniforms = null;

    private final List<PostChainActivator> activators = new ArrayList<>();
    private final Map<Identifier, List<Map<String, Identifier>>> samplersByPassShader = new HashMap<>();

    // World depth saved right before vanilla clears it for the first-person hand, so chains that run after
    // the hand still see terrain depth (see snapshotWorldDepth / runChainsAfterHand)
    private TextureTarget worldDepthSnapshot;
    private boolean worldDepthCaptured = false;

    public PostChainsManager() {
        super(Spec.of("Post chain", () -> PostChainActivator.CODEC)
                .wikiPage("Shaders")
                .folders("post_chains", "post_shaders"));
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
            for (var a : activators) a.close();
            activators.clear();
        }
        samplersByPassShader.clear();
    }

    private PolytoneGlobalUniforms globalUniforms() {
        if (globalUniforms == null) {
            globalUniforms = new PolytoneGlobalUniforms();
        }
        return globalUniforms;
    }

    public static void onProgramLinked(Set<String> declaredUniforms) {
        if (declaredUniforms.contains(GLOBALS_NAME)) globalsDeclared = true;
        if (declaredUniforms.contains(SHADOW_UBO_NAME)) shadowUboDeclared = true;
    }

    public static void onDynamicSamplerDeclared(String name) {
        if (SHADOW_SAMPLER_NAME.equals(name)) shadowSamplerDeclared = true;
    }

    // Cheap gate for the setPipeline hook, which runs on every draw in the game
    public boolean hasAnyPassBindings() {
        return globalsDeclared || shadowUboDeclared || shadowSamplerDeclared || !samplersByPassShader.isEmpty();
    }

    public void bindUniformBlocks(RenderPass pass, Set<String> declaredUniforms) {
        if (declaredUniforms.contains(GLOBALS_NAME)) {
            globalsDeclared = true;
            pass.setUniform(GLOBALS_NAME, globalUniforms().getSlice());
        }
        if (declaredUniforms.contains(SHADOW_UBO_NAME)) {
            GpuBufferSlice shadowSlice = Polytone.SHADOWS.renderer().getUniformsSlice();
            if (shadowSlice != null) {
                pass.setUniform(SHADOW_UBO_NAME, shadowSlice);
            }
        }
    }

    public boolean anyActiveChainWantsShadowMap() {
        synchronized (activators) {
            for (var a : activators) {
                if (a.wantsShadowMap()) return true;
            }
        }
        return false;
    }

    public void registerSamplers(Identifier passShaderId, Map<String, Identifier> samplers) {
        if (samplers.isEmpty()) return;
        samplersByPassShader.computeIfAbsent(passShaderId, k -> new ArrayList<>()).add(samplers);
    }

    public void unregisterSamplers(Identifier passShaderId, Map<String, Identifier> samplers) {
        List<Map<String, Identifier>> list = samplersByPassShader.get(passShaderId);
        if (list != null) {
            list.remove(samplers);
            if (list.isEmpty()) samplersByPassShader.remove(passShaderId);
        }
    }

    // Everything is gated on declaredUniforms: binding a sampler the program lacks makes Iris/Sodium log
    // errors every frame
    public void bindSamplers(RenderPass pass, RenderPipeline pipeline, Set<String> declaredUniforms) {
        if (declaredUniforms.contains(SHADOW_SAMPLER_NAME)) {
            GpuTextureView shadowMap = Polytone.SHADOWS.renderer().getShadowTexture();
            if (shadowMap != null) {
                pass.bindTexture(SHADOW_SAMPLER_NAME, shadowMap,
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            }
        }
        if (samplersByPassShader.isEmpty()) return;
        List<Map<String, Identifier>> list = samplersByPassShader.get(pipeline.getFragmentShader());
        if (list == null) return;
        var textureManager = Minecraft.getInstance().getTextureManager();
        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
        for (Map<String, Identifier> samplers : list) {
            for (var e : samplers.entrySet()) {
                if (!declaredUniforms.contains(e.getKey())) continue;
                GpuTextureView view = textureManager.getTexture(e.getValue()).getTextureView();
                pass.bindTexture(e.getKey(), view, sampler);
            }
        }
    }

    public void onClose() {
        synchronized (activators) {
            for (var a : activators) a.close();
        }
        if (globalUniforms != null) {
            globalUniforms.close();
            globalUniforms = null;
        }
        if (worldDepthSnapshot != null) {
            worldDepthSnapshot.destroyBuffers();
            worldDepthSnapshot = null;
        }
        Polytone.POST_TARGETS.close();
    }

    public void updateGlobalUniforms(Matrix4fc projectionMatrix, Matrix4fc viewMatrix, float deltaTime) {
        if (!globalsDeclared && !Polytone.isDevEnv) return;
        Minecraft mc = Minecraft.getInstance();
        float sunAngle = mc.levelRenderer.levelRenderState.skyRenderState.sunAngle;
        float dayTime = (float) ClientFrameTicker.getDayTime();
        globalUniforms().update(projectionMatrix, viewMatrix, sunAngle, dayTime, deltaTime);
    }

    public void tick() {
        for (var a : activators) {
            a.refreshActive();
        }
    }

    private List<PostChain> activeChains() {
        ShaderManager shaderManager = Minecraft.getInstance().getShaderManager();
        List<PostChain> active = new ArrayList<>();
        synchronized (activators) {
            for (var a : activators) {
                PostChain chain = a.getPostChain(shaderManager);
                if (chain != null) active.add(chain);
            }
        }
        return active;
    }

    private boolean hasActiveChains() {
        synchronized (activators) {
            for (var a : activators) {
                if (a.isActive()) return true;
            }
        }
        return false;
    }

    // Used when post_chains_after_hand is off: chains go into the level frame graph, before the hand
    public void addChainsToFrameGraph(int width, int height, LevelTargetBundle targets, FrameGraphBuilder frameGraphBuilder,
                                      GpuBufferSlice fog, CameraRenderState cameraRenderState) {
        Polytone.POST_TARGETS.ensureAllocated(width, height);
        PostChain.TargetBundle bundle = Polytone.POST_TARGETS.wrap(targets, frameGraphBuilder);
        for (PostChain chain : activeChains()) {
            chain.addToFrame(frameGraphBuilder, width, height, bundle);
        }
    }

    public void snapshotWorldDepth(RenderTarget main) {
        worldDepthCaptured = false;
        if (!hasActiveChains()) return;
        ensureSnapshotSized(main.width, main.height);
        worldDepthSnapshot.copyDepthFrom(main);
        worldDepthCaptured = true;
    }

    // Folds the saved world depth into the hand-only main depth (min of the two), then runs every active chain
    public void runChainsAfterHand(RenderTarget main, GraphicsResourceAllocator resourceAllocator) {
        if (!worldDepthCaptured) return;
        worldDepthCaptured = false;

        List<PostChain> active = activeChains();
        if (active.isEmpty()) return;

        combineWorldDepthIntoMain(main);
        for (PostChain chain : active) {
            chain.process(main, resourceAllocator);
        }
    }

    private void ensureSnapshotSized(int width, int height) {
        if (worldDepthSnapshot == null) {
            worldDepthSnapshot = new TextureTarget("Polytone World Depth Snapshot", width, height, true,
                    GpuFormat.RGBA8_UNORM);
        } else if (worldDepthSnapshot.width != width || worldDepthSnapshot.height != height) {
            worldDepthSnapshot.resize(width, height);
        }
    }

    private void combineWorldDepthIntoMain(RenderTarget main) {
        GpuTextureView worldDepth = worldDepthSnapshot.getDepthTextureView();
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone depth combine",
                main.getColorTextureView(), Optional.empty(),
                main.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(PolytoneRenderTypes.DEPTH_COMBINE_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("InSampler", worldDepth, sampler);
            pass.draw(3, 1, 0, 0);
        }
    }
}
