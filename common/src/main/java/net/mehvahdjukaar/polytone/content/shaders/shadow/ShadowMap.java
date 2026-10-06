package net.mehvahdjukaar.polytone.content.shaders.shadow;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.lang.ref.WeakReference;

public class ShadowMap implements AutoCloseable {

    private final WeakReference<ClientLevel> level;
    private final TextureTarget depthTarget;

    private final Matrix4f lightView = new Matrix4f();
    private final Matrix4f lightProj = new Matrix4f();
    private final Matrix4f shadowMatrix = new Matrix4f();
    private final Vector3f towardLight = new Vector3f(0, 1, 0);

    //what the last render used. while the map is reused shadowMatrix gets re-aligned from these
    private final Matrix4f renderedShadowMatrix = new Matrix4f();
    private Vec3 renderedCamPos = Vec3.ZERO;
    private long renderedAtMs = 0;
    private boolean rendered = false;

    public ShadowMap(ClientLevel level, int resolution) {
        this.level = new WeakReference<>(level);
        this.depthTarget = new TextureTarget(resolution, resolution, true, Minecraft.ON_OSX);
        GLHelper.disableColorAttachment(depthTarget);
    }

    public boolean isFor(@Nullable ClientLevel level) {
        return level != null && this.level.get() == level;
    }

    public int depthTextureId() {
        return depthTarget.getDepthTextureId();
    }

    public int resolution() {
        return depthTarget.width;
    }

    public Matrix4f lightView() {
        return lightView;
    }

    public Matrix4f lightProj() {
        return lightProj;
    }

    public Matrix4f shadowMatrix() {
        return shadowMatrix;
    }

    public Vector3f towardLight() {
        return towardLight;
    }

    //only while the camera is still well inside the box the map was drawn around. a teleport would slide the whole map away
    public boolean canBeReused(Vec3 camPos, long now, ShadowMapSettings settings) {
        if (!rendered || settings.updateInterval() <= 0f) return false;
        float maxDrift = settings.coverage() * 0.25f;
        if (camPos.distanceToSqr(renderedCamPos) > maxDrift * maxDrift) return false;
        return now - renderedAtMs < settings.updateInterval() * 50f;
    }

    //the projection is orthographic and the light basis is fixed between updates, so a plain translate by the camera delta re-aligns the map we already have
    public void realignTo(Vec3 camPos) {
        shadowMatrix.set(renderedShadowMatrix).translate(
                (float) (camPos.x - renderedCamPos.x),
                (float) (camPos.y - renderedCamPos.y),
                (float) (camPos.z - renderedCamPos.z));
    }

    public void updateLightMatrices(ClientLevel level, Vec3 camPos, float partialTick, ShadowMapSettings settings) {
        float coverage = settings.coverage();
        ShadowMapMath.directionTowardSunLight(level, partialTick, towardLight);
        ShadowMapMath.lightViewLookingAlong(towardLight, lightView);
        lightProj.setOrtho(-coverage, coverage, -coverage, coverage, -settings.depthRange(), settings.depthRange());
        ShadowMapMath.snapProjectionToTexelGrid(lightProj, lightView, camPos, coverage, resolution());
        shadowMatrix.set(lightProj).mul(lightView);
    }

    public void bindWriteAndClearDepth() {
        depthTarget.bindWrite(true);
        RenderSystem.depthMask(true);
        GlStateManager._clearDepth(1.0);
        GlStateManager._clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    public void markRendered(Vec3 camPos, long now) {
        renderedShadowMatrix.set(shadowMatrix);
        renderedCamPos = camPos;
        renderedAtMs = now;
        rendered = true;
    }

    public void markRenderFailed() {
        rendered = false;
    }

    @Override
    public void close() {
        depthTarget.destroyBuffers();
    }
}
