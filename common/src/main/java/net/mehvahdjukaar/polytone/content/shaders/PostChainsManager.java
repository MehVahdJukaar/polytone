package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
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
import net.mehvahdjukaar.polytone.mixins.accessor.PostChainAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

public class PostChainsManager extends ContentManager<PostChainActivator> {

    public static final String GLOBALS_NAME = "PolyGlobals";
    public static final String SHADOW_UBO_NAME = "PolyShadow";
    public static final String SHADOW_SAMPLER_NAME = "InShadow";

    // bound at runtime, not by a pipeline. read from the raw jsons so they exist before a level does
    public static List<String> dynamicSamplers() {
        List<String> viewpoints = Polytone.VIEWPOINTS.declaredSamplerNames();
        List<String> surfaceMap = Polytone.SURFACE_MAP.samplerNames();
        if (viewpoints.isEmpty() && surfaceMap.isEmpty()) return List.of(SHADOW_SAMPLER_NAME);
        List<String> all = new ArrayList<>(viewpoints.size() + surfaceMap.size() + 1);
        all.add(SHADOW_SAMPLER_NAME);
        all.addAll(viewpoints);
        all.addAll(surfaceMap);
        return all;
    }

    private static volatile boolean globalsDeclared = false;
    private static volatile boolean shadowUboDeclared = false;
    private static volatile boolean shadowSamplerDeclared = false;

    private PolytoneGlobalUniforms globalUniforms = null;
    private GpuBuffer emptyShadowUbo = null;

    private final List<PostChainActivator> activators = new ArrayList<>();
    private final Map<Identifier, List<Map<String, Identifier>>> samplersByPassPipeline = new HashMap<>();
    // logged once per chain
    private final Set<PostChain> warnedChains = Collections.newSetFromMap(new IdentityHashMap<>());

    private TextureTarget worldDepthSnapshot;
    private boolean worldDepthCaptured = false;
    private boolean levelRenderedThisFrame = false;

    // this frame's after hand suffix, render thread only
    private final List<ActiveChain> deferredAfterHand = new ArrayList<>();
    private List<Identifier> lastStagingIds = List.of();
    private int lastStagingSplit = -1;

    // the bobbed projection, see captureRenderedProjection
    private final Matrix4f renderedProjection = new Matrix4f();
    private boolean renderedProjectionValid = false;

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
        samplersByPassPipeline.clear();
        warnedChains.clear();
        deferredAfterHand.clear();
        lastStagingIds = List.of();
        lastStagingSplit = -1;
    }

    private GpuBufferSlice emptyShadowUbo() {
        if (emptyShadowUbo == null) {
            emptyShadowUbo = RenderSystem.getDevice().createBuffer(() -> "Polytone empty shadow UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, PolyShadowUniforms.UBO_SIZE);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                RenderSystem.getDevice().createCommandEncoder()
                        .writeToBuffer(emptyShadowUbo.slice(), stack.calloc(PolyShadowUniforms.UBO_SIZE));
            }
        }
        return emptyShadowUbo.slice();
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
        Polytone.SURFACE_MAP.onSamplerDeclared(name);
    }

    public boolean hasAnyPassBindings() {
        return globalsDeclared || shadowUboDeclared || shadowSamplerDeclared || !samplersByPassPipeline.isEmpty()
                || !Polytone.VIEWPOINTS.isEmpty() || Polytone.SURFACE_MAP.hasDeclaredSamplers();
    }

    public void bindUniformBlocks(RenderPass pass, Set<String> declaredUniforms) {
        if (declaredUniforms.contains(GLOBALS_NAME)) {
            globalsDeclared = true;
            pass.setUniform(GLOBALS_NAME, globalUniforms().getSlice());
        }
        if (declaredUniforms.contains(SHADOW_UBO_NAME)) {
            GpuBufferSlice shadowSlice = Polytone.SHADOWS.renderer().getUniformsSlice();
            pass.setUniform(SHADOW_UBO_NAME, shadowSlice != null ? shadowSlice : emptyShadowUbo());
        }
        Polytone.VIEWPOINTS.bindUniformBlocks(pass, declaredUniforms);
        Polytone.SURFACE_MAP.bindUniformBlocks(pass, declaredUniforms);
    }

    public boolean anyActiveChainWantsShadowMap() {
        synchronized (activators) {
            for (var a : activators) {
                if (a.wantsShadowMap()) return true;
            }
        }
        return false;
    }

    // union of the viewpoints every currently active chain asked for
    public Set<Identifier> wantedViewpoints() {
        Set<Identifier> wanted = new HashSet<>();
        synchronized (activators) {
            for (var a : activators) wanted.addAll(a.wantedViewpoints());
        }
        return wanted;
    }

    public void registerSamplers(Identifier pipelineLocation, Map<String, Identifier> samplers) {
        if (samplers.isEmpty()) return;
        samplersByPassPipeline.computeIfAbsent(pipelineLocation, k -> new ArrayList<>()).add(samplers);
    }

    public void unregisterSamplers(Identifier pipelineLocation, Map<String, Identifier> samplers) {
        List<Map<String, Identifier>> list = samplersByPassPipeline.get(pipelineLocation);
        if (list != null) {
            list.remove(samplers);
            if (list.isEmpty()) samplersByPassPipeline.remove(pipelineLocation);
        }
    }

    public void bindSamplers(RenderPass pass, RenderPipeline pipeline, Set<String> declaredUniforms) {
        if (declaredUniforms.contains(SHADOW_SAMPLER_NAME)) {
            GpuTextureView shadowMap = Polytone.SHADOWS.renderer().getShadowTexture();
            if (shadowMap == null) {
                shadowMap = Minecraft.getInstance().getTextureManager()
                        .getTexture(MissingTextureAtlasSprite.getLocation()).getTextureView();
            }
            pass.bindTexture(SHADOW_SAMPLER_NAME, shadowMap,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
        Polytone.VIEWPOINTS.bindSamplers(pass, declaredUniforms);
        Polytone.SURFACE_MAP.bindSamplers(pass, declaredUniforms);
        if (samplersByPassPipeline.isEmpty()) return;
        List<Map<String, Identifier>> list = samplersByPassPipeline.get(pipeline.getLocation());
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
        deferredAfterHand.clear();
        lastStagingIds = List.of();
        lastStagingSplit = -1;
        if (globalUniforms != null) {
            globalUniforms.close();
            globalUniforms = null;
        }
        if (emptyShadowUbo != null) {
            emptyShadowUbo.close();
            emptyShadowUbo = null;
        }
        DeclaredUniforms.clearCache();
        if (worldDepthSnapshot != null) {
            worldDepthSnapshot.destroyBuffers();
            worldDepthSnapshot = null;
        }
        Polytone.POST_TARGETS.close();
    }

    // vanilla renders with a bobbed copy of cameraState.projectionMatrix, depth reconstruction needs that one
    public void captureRenderedProjection(Matrix4fc projection) {
        renderedProjection.set(projection);
        renderedProjectionValid = true;
    }

    // consumed, so a frame without a capture falls back instead of reusing the last one
    private Matrix4fc renderedProjectionOr(Matrix4fc fallback) {
        if (!renderedProjectionValid) return fallback;
        renderedProjectionValid = false;
        return renderedProjection;
    }

    public void updateGlobalUniforms(Matrix4fc projectionMatrix, Matrix4fc viewMatrix, float deltaTime) {
        projectionMatrix = renderedProjectionOr(projectionMatrix);
        if (!globalsDeclared && !Polytone.isDevEnv) return;
        Minecraft mc = Minecraft.getInstance();
        float sunAngle = mc.levelRenderer.levelRenderState.skyRenderState.sunAngle;
        float dayTime = (float) ClientFrameTicker.getDayTime();
        globalUniforms().update(projectionMatrix, viewMatrix, sunAngle, dayTime, deltaTime);
    }

    public void tick() {
        synchronized (activators) {
            for (var a : activators) {
                a.refreshActive();
            }
        }
    }

    // PostChain has no id of its own
    private record ActiveChain(Identifier id, PostChain chain, boolean readsMainDepth) {
    }

    // priority order, which is data flow: every chain rewrites minecraft:main
    private List<ActiveChain> activeChains() {
        ShaderManager shaderManager = Minecraft.getInstance().getShaderManager();
        List<ActiveChain> active = new ArrayList<>();
        synchronized (activators) {
            for (var a : activators) {
                PostChain chain = a.getPostChain(shaderManager);
                if (chain != null) active.add(new ActiveChain(a.postChainId(), chain, a.readsMainDepth()));
            }
        }
        return active;
    }

    // after hand runs after the whole level graph, so only a suffix of the order can move there without reordering
    private static int firstDeferrable(List<ActiveChain> ordered) {
        int i = ordered.size();
        while (i > 0 && canRunAfterHand(ordered.get(i - 1).chain())) i--;
        return i;
    }

    private boolean anyDeferredChainReadsMainDepth() {
        for (ActiveChain c : deferredAfterHand) {
            if (c.readsMainDepth()) return true;
        }
        return false;
    }

    // logged when the split changes, naming the chain that held the rest back
    private void logStaging(List<ActiveChain> ordered, int split) {
        // every frame, don't allocate unless something changed
        if (split == lastStagingSplit && sameIds(ordered, lastStagingIds)) return;
        lastStagingSplit = split;
        List<Identifier> ids = new ArrayList<>(ordered.size());
        for (ActiveChain c : ordered) ids.add(c.id());
        lastStagingIds = ids;

        String level = ordered.subList(0, split).stream().map(c -> c.id().toString())
                .collect(Collectors.joining(", "));
        String afterHand = ordered.subList(split, ordered.size()).stream().map(c -> c.id().toString())
                .collect(Collectors.joining(", "));
        Polytone.LOGGER.info("Post chains: level=[{}] after_hand=[{}]{}", level, afterHand,
                split > 0 ? " (held back by " + ordered.get(split - 1).id() + ")" : "");
    }

    private static boolean sameIds(List<ActiveChain> ordered, List<Identifier> ids) {
        if (ordered.size() != ids.size()) return false;
        for (int i = 0; i < ids.size(); i++) {
            if (!ordered.get(i).id().equals(ids.get(i))) return false;
        }
        return true;
    }

    // level graph, before the hand. decides the frame's split and hosts everything before the deferred suffix
    public void addChainsToFrameGraph(int width, int height, LevelTargetBundle targets, FrameGraphBuilder frameGraphBuilder,
                                      GpuBufferSlice fog, CameraRenderState cameraRenderState) {
        Polytone.POST_TARGETS.ensureAllocated(width, height);
        PostChain.TargetBundle bundle = Polytone.POST_TARGETS.wrap(targets, frameGraphBuilder);

        List<ActiveChain> ordered = activeChains();
        int split = Polytone.CONFIGS.postChainsAfterHand.get() ? firstDeferrable(ordered) : ordered.size();
        logStaging(ordered, split);

        deferredAfterHand.clear();
        deferredAfterHand.addAll(ordered.subList(split, ordered.size()));

        for (ActiveChain active : ordered.subList(0, split)) {
            // skipped but still a barrier above, or a missing target would let the suffix swallow the rest
            if (!bundleSatisfies(bundle, active.chain(), "the level frame graph")) continue;
            active.chain().addToFrame(frameGraphBuilder, width, height, bundle);
        }
    }

    // only minecraft:main and our own post_targets outlive the level graph, vanilla's sorting targets don't
    private static boolean canRunAfterHand(PostChain chain) {
        for (Identifier id : ((PostChainAccessor) chain).polytone$getExternalTargets()) {
            if (id.equals(PostChain.MAIN_TARGET_ID)) continue;
            if (Polytone.POST_TARGETS.customTargetIds().contains(id)) continue;
            return false;
        }
        return true;
    }

    // getOrThrow would crash mid frame. sorting targets only exist with improved transparency on
    private boolean bundleSatisfies(PostChain.TargetBundle bundle, PostChain chain, String stage) {
        for (Identifier id : ((PostChainAccessor) chain).polytone$getExternalTargets()) {
            if (bundle.get(id) == null) {
                if (warnedChains.add(chain)) {
                    Polytone.LOGGER.error(
                            "Skipping a Polytone post chain: it reads target {}, which is not available in {}. " +
                            "Check that the target exists and, for vanilla sorting targets, that Improved " +
                            "Transparency is enabled.", id, stage);
                }
                return false;
            }
        }
        return true;
    }

    public void snapshotWorldDepth(RenderTarget main) {
        levelRenderedThisFrame = true;
        worldDepthCaptured = false;
        // only a deferred chain reading main's depth needs the world depth back
        if (!anyDeferredChainReadsMainDepth()) return;
        ensureSnapshotSized(main.width, main.height);
        worldDepthSnapshot.copyDepthFrom(main);
        worldDepthCaptured = true;
    }

    // folds the world depth back into main, then runs the deferred chains
    public void runChainsAfterHand(RenderTarget main, GraphicsResourceAllocator resourceAllocator) {
        if (!levelRenderedThisFrame) return;
        levelRenderedThisFrame = false;
        boolean depthCaptured = worldDepthCaptured;
        worldDepthCaptured = false;
        // exactly what addChainsToFrameGraph deferred, never re-derived
        if (deferredAfterHand.isEmpty()) return;

        if (depthCaptured) combineWorldDepthIntoMain(main);
        //not chain.process(): its bundle only holds main, so pack post_targets would be missing
        Polytone.POST_TARGETS.ensureAllocated(main.width, main.height);
        for (ActiveChain active : deferredAfterHand) {
            FrameGraphBuilder frameGraph = new FrameGraphBuilder();
            PostChain.TargetBundle mainOnly = PostChain.TargetBundle.of(PostChain.MAIN_TARGET_ID, frameGraph.importExternal("main", main));
            PostChain.TargetBundle bundle = Polytone.POST_TARGETS.wrap(mainOnly, frameGraph);
            if (!bundleSatisfies(bundle, active.chain(), "the after-hand stage")) continue;
            active.chain().addToFrame(frameGraph, main.width, main.height, bundle);
            frameGraph.execute(resourceAllocator);
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
