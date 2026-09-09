package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

public class PostChainsManager extends ContentManager<PostChainActivator> {

    public static final String GLOBALS_NAME = "PolyGlobals";
    public static final String SHADOW_UBO_NAME = "PolyShadow";
    public static final String SHADOW_SAMPLER_NAME = "InShadow";
    public static final List<String> DYNAMIC_SAMPLERS = List.of(SHADOW_SAMPLER_NAME);

    private static volatile boolean globalsDeclared = false;
    private static volatile boolean shadowUboDeclared = false;
    private static volatile boolean shadowSamplerDeclared = false;

    private PolytoneGlobalUniforms globalUniforms = null;
    private GpuBuffer emptyShadowUbo = null;

    private final List<PostChainActivator> activators = new ArrayList<>();
    private final Map<Identifier, List<Map<String, Identifier>>> samplersByShader = new HashMap<>();

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
            for (var e : activators) e.close();
            activators.clear();
        }
        samplersByShader.clear();
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

    private PolytoneGlobalUniforms getOrCreateUniforms() {
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

    public boolean hasAnyPassBindings() {
        return globalsDeclared || shadowUboDeclared || shadowSamplerDeclared || !samplersByShader.isEmpty();
    }

    public void setupExtraUniforms(RenderPass pass, Set<String> declaredUniforms) {
        if (declaredUniforms.contains(GLOBALS_NAME)) {
            globalsDeclared = true;
            pass.setUniform(GLOBALS_NAME, getOrCreateUniforms().getSlice());
        }
        if (declaredUniforms.contains(SHADOW_UBO_NAME)) {
            GpuBufferSlice shadowSlice = Polytone.SHADOWS.renderer().getUniformsSlice();
            pass.setUniform(SHADOW_UBO_NAME, shadowSlice != null ? shadowSlice : emptyShadowUbo());
        }
    }

    public boolean anyActiveEffectUsesShadowMap() {
        synchronized (activators) {
            for (var a : activators) {
                if (a.wantsShadowMap()) return true;
            }
        }
        return false;
    }

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

    public void bindExtraSamplers(RenderPass pass, RenderPipeline pipeline, Set<String> declaredUniforms) {
        if (declaredUniforms.contains(SHADOW_SAMPLER_NAME)) {
            GpuTextureView shadowMap = Polytone.SHADOWS.renderer().getShadowTexture();
            if (shadowMap == null) {
                shadowMap = Minecraft.getInstance().getTextureManager()
                        .getTexture(TextureManager.INTENTIONAL_MISSING_TEXTURE).getTextureView();
            }
            pass.bindTexture(SHADOW_SAMPLER_NAME, shadowMap,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
        if (samplersByShader.isEmpty()) return;
        List<Map<String, Identifier>> list = samplersByShader.get(pipeline.getFragmentShader());
        if (list == null) return;
        var texManager = Minecraft.getInstance().getTextureManager();
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
        if (emptyShadowUbo != null) {
            emptyShadowUbo.close();
            emptyShadowUbo = null;
        }
        if (worldDepthSnapshot != null) {
            worldDepthSnapshot.destroyBuffers();
            worldDepthSnapshot = null;
        }
        Polytone.POST_TARGETS.close();
    }

    public void captureLevelRendererParams(Matrix4fc projectionMatrix, Matrix4fc viewMatrix, float deltaTime) {
        if (!globalsDeclared && !Polytone.isDevEnv) return;
        Minecraft mc = Minecraft.getInstance();
        float angle = mc.levelRenderer.levelRenderState.skyRenderState.sunAngle;
        float dayTime = (float) ClientFrameTicker.getDayTime();
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

    private TextureTarget worldDepthSnapshot;
    private boolean worldDepthCaptured = false;

    public void snapshotWorldDepth(RenderTarget main) {
        worldDepthCaptured = false;
        if (!hasActiveChains()) return;
        ensureSnapshotSized(main.width, main.height);
        worldDepthSnapshot.copyDepthFrom(main);
        worldDepthCaptured = true;
    }

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
