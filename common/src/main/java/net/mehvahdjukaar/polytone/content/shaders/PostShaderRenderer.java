package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.function.IntSupplier;

import net.mehvahdjukaar.polytone.content.shaders.PostShadersManager.ActivePostPassFrame;

public class PostShaderRenderer {

    private boolean depthCapturedThisFrame = false;

    private final Matrix4f projMat = new Matrix4f();
    private final Matrix4f modelViewMat = new Matrix4f();

    private TextureTarget depthSnapshot = null;
    private ShaderInstance depthCombineShader = null;
    private boolean depthCombineFailed = false;

    public void captureLevelMatrices(Matrix4f projection, Matrix4f modelView) {
        this.projMat.set(projection);
        this.modelViewMat.set(modelView);
    }

    public void captureLevelDepthSnapshot() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        ensureDepthSnapshot(main);
        depthSnapshot.copyDepthFrom(main);
        main.bindWrite(false);
        depthCapturedThisFrame = true;
    }

    public void resize(int width, int height) {
        if (depthSnapshot != null) {
            depthSnapshot.resize(width, height, Minecraft.ON_OSX);
        }
    }

    public void render(LinkedHashMap<PostChainEffect, PostChain> activeChains, boolean anyUsesDepth, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        Polytone.POST_TARGETS.ensureAllocated(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
        float sunAngle = 0f;
        float dayTime = 0f;
        float deltaTime = mc.getTimer().getGameTimeDeltaTicks();
        ClientLevel level = mc.level;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (level != null) {
            sunAngle = level.getSunAngle(partial) - Mth.HALF_PI;
            dayTime = (float) (level.getDayTime() % 24000L);
        }

        Vec3 playerPos = mc.player == null ? Vec3.ZERO : mc.player.getPosition(partial);
        BlockPos playerBlockPos = BlockPos.containing(playerPos);
        Vec3 playerOffset = new Vec3(
                playerBlockPos.getX() - playerPos.x,
                playerBlockPos.getY() - playerPos.y,
                playerBlockPos.getZ() - playerPos.z);

        IntSupplier depthTexture = prepareDepthSnapshot(mc, anyUsesDepth);

        if (depthTexture != null && depthCapturedThisFrame
                && Polytone.CONFIGS.postShadersOccludeHeldItems.get()) {
            foldHeldItemDepthIntoSnapshot(mc);
        }

        for (var entry : activeChains.entrySet()) {
            PostChainEffect effect = entry.getKey();
            PostChain chain = entry.getValue();
            PostShadersManager.ACTIVE_POST_PASS.set(new ActivePostPassFrame(
                    effect, projMat, modelViewMat, sunAngle, dayTime,
                    deltaTime, playerBlockPos, playerOffset, depthTexture));
            try {
                chain.process(partialTicks);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error processing polytone post chain '{}'", chain.getName(), e);
            } finally {
                PostShadersManager.ACTIVE_POST_PASS.remove();
            }
        }

        mc.getMainRenderTarget().bindWrite(true);

        depthCapturedThisFrame = false;
    }

    private IntSupplier prepareDepthSnapshot(Minecraft mc, boolean anyUsesDepth) {
        if (!anyUsesDepth) return null;

        RenderTarget main = mc.getMainRenderTarget();
        ensureDepthSnapshot(main);
        if (!depthCapturedThisFrame) {
            depthSnapshot.copyDepthFrom(main);
            main.bindWrite(false);
        }
        return depthSnapshot::getDepthTextureId;
    }

    private void foldHeldItemDepthIntoSnapshot(Minecraft mc) {
        ShaderInstance shader = getDepthCombineShader(mc);
        if (shader == null) return;

        RenderTarget main = mc.getMainRenderTarget();

        depthSnapshot.bindWrite(true);

        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);

        shader.setSampler("InSampler", main.getDepthTextureId());
        RenderSystem.setShader(() -> shader);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        bb.addVertex(1f, -1f, 0f).setUv(1f, 0f);
        bb.addVertex(1f, 1f, 0f).setUv(1f, 1f);
        bb.addVertex(-1f, 1f, 0f).setUv(0f, 1f);
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        main.bindWrite(false);
    }

    private ShaderInstance getDepthCombineShader(Minecraft mc) {
        if (depthCombineShader == null && !depthCombineFailed) {
            try {
                depthCombineShader = new ShaderInstance(mc.getResourceManager(),
                        "polytone_depth_combine", DefaultVertexFormat.POSITION_TEX);
            } catch (Exception e) {
                depthCombineFailed = true;
                Polytone.LOGGER.error("Failed to load polytone_depth_combine shader; " +
                        "held items will not occlude depth-driven post shaders", e);
            }
        }
        return depthCombineShader;
    }

    private void ensureDepthSnapshot(RenderTarget main) {
        if (depthSnapshot == null) {
            depthSnapshot = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            depthSnapshot.setClearColor(0f, 0f, 0f, 0f);
        } else if (depthSnapshot.width != main.width || depthSnapshot.height != main.height) {
            depthSnapshot.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        //for neoforge
        PlatStuff.matchStencil(main, depthSnapshot);
    }

    public void close() {
        if (depthSnapshot != null) {
            depthSnapshot.destroyBuffers();
            depthSnapshot = null;
        }
        if (depthCombineShader != null) {
            depthCombineShader.close();
            depthCombineShader = null;
        }
        depthCombineFailed = false;
    }
}
