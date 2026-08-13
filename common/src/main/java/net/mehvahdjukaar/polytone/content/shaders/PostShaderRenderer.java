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

// The per-frame GPU half of the post-shader system: captures the level matrices and depth snapshot,
// folds the held-item depth in, and runs the active chains. PostShadersManager drives it under its lock.
public class PostShaderRenderer {

    private boolean depthCapturedThisFrame = false;

    // Level projection / camera matrices captured during GameRenderer.renderLevel, exposed to pass
    // shaders as the PolyProjMat / PolyModelViewMat built-in uniforms.
    private final Matrix4f projMat = new Matrix4f();
    private final Matrix4f modelViewMat = new Matrix4f();

    // Standalone depth target for effects that declare use_depth_buffer. Can't sample the main
    // framebuffer's own depth attachment while the post quad writes to it (read/write feedback loop),
    // so we blit the level depth here once per frame and sample this instead.
    private TextureTarget depthSnapshot = null;

    // Fullscreen depth-only shader that folds the held-item depth into depthSnapshot.
    private ShaderInstance depthCombineShader = null;
    private boolean depthCombineFailed = false;

    public void captureLevelMatrices(Matrix4f projection, Matrix4f modelView) {
        this.projMat.set(projection);
        this.modelViewMat.set(modelView);
    }

    // must run while level geometry is still intact, before GameRenderer clears depth for the
    // first-person hand
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

    // each chain reads from and writes back to the main render target, so later chains see the
    // previous chain's output
    public void render(LinkedHashMap<PostChainEffect, PostChain> activeChains, boolean anyUsesDepth, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        // Keep persistent post targets allocated/sized to the frame so target_samplers resolve.
        Polytone.POST_TARGETS.ensureAllocated(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
        float sunAngle = 0f;
        float dayTime = 0f;
        // frame delta time in ticks (matches 1.21.11 PolyDeltaTime = deltaTracker.getGameTimeDeltaTicks())
        float deltaTime = mc.getTimer().getGameTimeDeltaTicks();
        ClientLevel level = mc.level;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (level != null) {
            // match 1.21.11: 0 = noon (sun straight up), measured from the horizon
            sunAngle = level.getSunAngle(partial) - Mth.HALF_PI;
            dayTime = (float) (level.getDayTime() % 24000L);
        }

        // lerped player (feet) position, split like vanilla's CameraBlockPos/CameraOffset so shaders
        // keep float precision at large coordinates: exact = vec3(PolyPlayerBlockPos) - PolyPlayerOffset
        Vec3 playerPos = mc.player == null ? Vec3.ZERO : mc.player.getPosition(partial);
        BlockPos playerBlockPos = BlockPos.containing(playerPos);
        Vec3 playerOffset = new Vec3(
                playerBlockPos.getX() - playerPos.x,
                playerBlockPos.getY() - playerPos.y,
                playerBlockPos.getZ() - playerPos.z);

        IntSupplier depthTexture = prepareDepthSnapshot(mc, anyUsesDepth);

        // The depth snapshot is taken at the end of level rendering, before GameRenderer clears the
        // depth buffer to draw the first-person hand. So held items (a raised shield) aren't in the
        // depth that effects like godrays sample, and they leak straight through. Fold the hand depth
        // back into the snapshot here (we run after the hand) so held items occlude depth effects.
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

        // Every PostChain.process() ends by unbinding its final pass's output target, which leaves
        // framebuffer 0 (the default backbuffer) bound - NOT the main render target. Vanilla restores
        // the main target right after its own gameRenderer.postEffect.process() via bindWrite(true);
        // because we run our chains AFTER that restore, we must re-bind it ourselves. Otherwise the
        // entire HUD (hotbar, inventory, F3, toasts, screens) is rendered into the backbuffer and then
        // overwritten by the end-of-frame blit of the main target - i.e. the GUI vanishes.
        mc.getMainRenderTarget().bindWrite(true);

        depthCapturedThisFrame = false;
    }

    // If any active effect samples the depth buffer, return a supplier of the snapshot depth texture id.
    // Prefers the copy taken at the end of LevelRenderer.renderLevel; copies now as a fallback when level
    // rendering did not run this frame.
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

    // Draw the (hand-only) main depth into the world-depth snapshot with a LEQUAL test, leaving
    // min(worldDepth, handDepth) per pixel. Runs after the hand is drawn, so held items occlude
    // depth-driven post effects instead of leaking through them.
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

        // Restore neutral state; the chain loop and vanilla's later HUD pass set up their own.
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
        // On (Neo)Forge a mod can call RenderTarget.enableStencil() on the main framebuffer, flipping its depth
        // attachment to a combined GL_DEPTH32F_STENCIL8. copyDepthFrom() blits GL_DEPTH_BUFFER_BIT, which needs
        // matching formats, or GL raises INVALID_OPERATION and copies nothing - the snapshot stays cleared and
        // depth effects silently do nothing. Vanilla propagates stencil to its temp targets the same way.
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
