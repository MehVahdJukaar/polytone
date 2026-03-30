package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.systems.RenderPass;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostShadersManager extends JsonPartialReloader {

    public static final String GLOBALS_NAME = "PolyGlobals";
    private PolytoneGlobalUniforms globalUniforms = null;

    private final List<PostChainEffect> effects = new ArrayList<>();

    public PostShadersManager() {
        super("post_shaders");
    }

    @Override
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var j : Parsed.batchParseOnlyEnabled(jsons, PostChainEffect.CODEC,
                ops, "Post Shader Effects")) {
            if (j != null) {
                effects.add(j.getValue());
            }
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        effects.clear();
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
        if (globalUniforms != null) {
            globalUniforms.close();
            globalUniforms = null;
        }
    }

    public void captureLevelRendererParams(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        float angle = Minecraft.getInstance().levelRenderer.levelRenderState.skyRenderState.sunAngle;
        getOrCreateUniforms().update(projectionMatrix, viewMatrix, angle);
    }

    public void addPostPass(int width, int height, LevelTargetBundle targets, FrameGraphBuilder frameGraphBuilder, GpuBufferSlice gpuBufferSlice, CameraRenderState cameraRenderState) {

        ShaderManager sm = Minecraft.getInstance().getShaderManager();
        for (var e : effects) {
            try {
                PostChain pc = sm.getPostChain(e.postChain(), LevelTargetBundle.MAIN_TARGETS);
                if (pc != null) pc.addToFrame(frameGraphBuilder, width, height, targets);
            }catch (Exception ex) {

            }
        }

    }
}
