package net.mehvahdjukaar.polytone.content.shaders.post;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
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
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.shaders.ShaderUniformsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class PostChainsManager extends ContentManager<PostChainActivator> implements IShaderModifier {

    private final List<PostChainActivator> activators = new ArrayList<>();
    private final Map<Identifier, List<Map<String, Identifier>>> samplersByPassPipeline = new HashMap<>();

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
        samplersByPassPipeline.clear();
        IShaderModifier.invalidateEnabledLists();
    }

    public void registerSamplers(Identifier pipelineLocation, Map<String, Identifier> samplers) {
        if (samplers.isEmpty()) return;
        samplersByPassPipeline.computeIfAbsent(pipelineLocation, k -> new ArrayList<>()).add(samplers);
        IShaderModifier.invalidateEnabledLists();
    }

    public void unregisterSamplers(Identifier pipelineLocation, Map<String, Identifier> samplers) {
        List<Map<String, Identifier>> list = samplersByPassPipeline.get(pipelineLocation);
        if (list != null) {
            list.remove(samplers);
            if (list.isEmpty()) samplersByPassPipeline.remove(pipelineLocation);
        }
        IShaderModifier.invalidateEnabledLists();
    }

    @Override
    public List<String> getEnablingUniforms() {
        return List.of();
    }

    @Override
    public boolean isUsedBy(IShader shader) {
        return samplersByPassPipeline.containsKey(shader.pipeline().getLocation());
    }

    @Override
    public void bindTo(IShader shader) {
        List<Map<String, Identifier>> list = samplersByPassPipeline.get(shader.pipeline().getLocation());
        if (list == null) return;
        var texManager = Minecraft.getInstance().getTextureManager();
        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
        for (Map<String, Identifier> samplers : list) {
            for (var e : samplers.entrySet()) {
                GpuTextureView view = texManager.getTexture(e.getValue()).getTextureView();
                shader.setSampler(e.getKey(), view, sampler);
            }
        }
    }

    public void onClose() {
        synchronized (activators) {
            for (var e : activators) e.close();
        }
        if (worldDepthSnapshot != null) {
            worldDepthSnapshot.destroyBuffers();
            worldDepthSnapshot = null;
        }
        Polytone.POST_TARGETS.close();
    }

    public void tick() {
        synchronized (activators) {
            for (var a : activators) {
                a.refreshEnabled();
            }
        }
    }

    private boolean anyActiveChainReadsMainDepth() {
        synchronized (activators) {
            for (var a : activators) {
                if (a.readsMainDepth()) return true;
            }
        }
        return false;
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
    //after the hand we are outside the level pass, still world space though
    private boolean processingAfterHand = false;
    private boolean worldDepthCaptured = false;
    private boolean levelRenderedThisFrame = false;

    public void snapshotWorldDepth(RenderTarget main) {
        levelRenderedThisFrame = true;
        worldDepthCaptured = false;
        if (!anyActiveChainReadsMainDepth()) return;
        ensureSnapshotSized(main.width, main.height);
        worldDepthSnapshot.copyDepthFrom(main);
        worldDepthCaptured = true;
    }

    public void runAfterHand(RenderTarget main, GraphicsResourceAllocator resourceAllocator) {
        if (!levelRenderedThisFrame) return;
        levelRenderedThisFrame = false;

        ShaderManager sm = Minecraft.getInstance().getShaderManager();
        List<PostChain> active = new ArrayList<>();
        synchronized (activators) {
            for (var a : activators) {
                PostChain pc = a.getPostChain(sm);
                if (pc != null) active.add(pc);
            }
        }
        if (active.isEmpty()) return;

        if (worldDepthCaptured) combineHandDepthIntoWorld(main);
        worldDepthCaptured = false;
        //not pc.process(): its bundle only holds main, so pack post_targets would be missing
        Polytone.POST_TARGETS.ensureAllocated(main.width, main.height);
        processingAfterHand = true;
        try {
            for (PostChain pc : active) {
                FrameGraphBuilder frameGraph = new FrameGraphBuilder();
                PostChain.TargetBundle mainOnly = PostChain.TargetBundle.of(PostChain.MAIN_TARGET_ID, frameGraph.importExternal("main", main));
                pc.addToFrame(frameGraph, main.width, main.height, Polytone.POST_TARGETS.wrap(mainOnly, frameGraph));
                frameGraph.execute(resourceAllocator);
            }
        } finally {
            processingAfterHand = false;
        }
    }

    public boolean isProcessing() {
        return processingAfterHand;
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
