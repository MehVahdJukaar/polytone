package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns post-chain activators (turn a {@link PostChain} on/off based on a condition) and the
 * {@code PolyGlobals} UBO that gets bound to every render pass.
 */
public class PostChainsManager extends ContentManager<PostChainActivator> {

    public static final String GLOBALS_NAME = "PolyGlobals";
    private PolytoneGlobalUniforms globalUniforms = null;

    private final List<PostChainActivator> activators = new ArrayList<>();

    public PostChainsManager() {
        super("Post chain", () -> PostChainActivator.CODEC, "post_chains");
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
    }

    private PolytoneGlobalUniforms getOrCreateUniforms() {
        if (globalUniforms == null) {
            globalUniforms = new PolytoneGlobalUniforms();
        }
        return globalUniforms;
    }

    public void setupExtraUniforms(RenderPass pass) {
        pass.setUniform(GLOBALS_NAME, getOrCreateUniforms().getSlice());
    }

    public void onClose() {
        synchronized (activators) {
            for (var e : activators) e.close();
        }
        if (globalUniforms != null) {
            globalUniforms.close();
            globalUniforms = null;
        }
    }

    public void captureLevelRendererParams(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        Minecraft mc = Minecraft.getInstance();
        float angle = mc.levelRenderer.levelRenderState.skyRenderState.sunAngle;
        float dayTime = mc.level == null ? 0f : (float) (mc.level.getDayTime() % 24000L);
        getOrCreateUniforms().update(projectionMatrix, viewMatrix, angle, dayTime);
    }

    public void tick() {
        for (var a : activators) {
            a.refreshEnabled();
        }
    }

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
}
