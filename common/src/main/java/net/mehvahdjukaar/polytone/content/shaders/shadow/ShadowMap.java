package net.mehvahdjukaar.polytone.content.shaders.shadow;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class ShadowMap implements AutoCloseable {

    private final WeakReference<ClientLevel> level;
    private final GpuTexture depthTexture;
    private final GpuTextureView depthTextureView;
    private final GpuTexture colorTexture;
    private final GpuTextureView colorTextureView;
    private final GpuBuffer lightProjectionBuffer;

    private final Matrix4f lightView = new Matrix4f();
    private final Matrix4f lightProj = new Matrix4f();
    private final Matrix4f shadowMatrix = new Matrix4f();
    private final Vector3f towardLight = new Vector3f(0, 1, 0);

    private final Matrix4f renderedShadowMatrix = new Matrix4f();
    private Vec3 renderedCamPos = Vec3.ZERO;
    private long renderedAtMs = 0;
    private boolean rendered = false;

    public ShadowMap(ClientLevel level, int resolution) {
        this.level = new WeakReference<>(level);
        GpuDevice device = RenderSystem.getDevice();
        this.depthTexture = device.createTexture(() -> "Polytone shadow map depth",
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                GpuFormat.D32_FLOAT, resolution, resolution, 1, 1);
        this.depthTextureView = device.createTextureView(depthTexture);
        //COPY_DST too or the clear is rejected
        this.colorTexture = device.createTexture(() -> "Polytone shadow map color",
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                GpuFormat.RGBA8_UNORM, resolution, resolution, 1, 1);
        this.colorTextureView = device.createTextureView(colorTexture);
        this.lightProjectionBuffer = device.createBuffer(() -> "Polytone shadow projection UBO",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    public boolean isFor(@Nullable ClientLevel level) {
        return level != null && this.level.get() == level;
    }

    public int resolution() {
        return depthTexture.getWidth(0);
    }

    public GpuTexture depthTexture() {
        return depthTexture;
    }

    public GpuTextureView depthTextureView() {
        return depthTextureView;
    }

    public GpuTexture colorTexture() {
        return colorTexture;
    }

    public GpuTextureView colorTextureView() {
        return colorTextureView;
    }

    public GpuBufferSlice lightProjection() {
        return lightProjectionBuffer.slice();
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

    //ortho and the light basis is fixed between updates, so a translate by the camera delta is enough
    public void realignTo(Vec3 camPos) {
        shadowMatrix.set(renderedShadowMatrix).translate(
                (float) (camPos.x - renderedCamPos.x),
                (float) (camPos.y - renderedCamPos.y),
                (float) (camPos.z - renderedCamPos.z));
    }

    public void updateLightMatrices(Camera camera, Vec3 camPos, float partialTick, ShadowMapSettings settings) {
        float coverage = settings.coverage();
        ShadowMapMath.directionTowardSunLight(camera, partialTick, towardLight);
        ShadowMapMath.lightViewLookingAlong(towardLight, lightView);
        //reversed-Z like vanilla Projection
        boolean zZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
        lightProj.setOrtho(-coverage, coverage, -coverage, coverage, settings.depthRange(), -settings.depthRange(), zZeroToOne);
        ShadowMapMath.snapProjectionToTexelGrid(lightProj, lightView, camPos, coverage, resolution());
        shadowMatrix.set(lightProj).mul(lightView);
    }

    public void uploadProjectionAndClearDepth() {
        GpuDevice device = RenderSystem.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bytes = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
                    .putMat4f(lightProj).get();
            device.createCommandEncoder().writeToBuffer(lightProjectionBuffer.slice(), bytes);
        }
        device.createCommandEncoder().clearColorAndDepthTextures(colorTexture, new Vector4f(0, 0, 0, 0), depthTexture, 0.0);
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
        depthTextureView.close();
        depthTexture.close();
        colorTextureView.close();
        colorTexture.close();
        lightProjectionBuffer.close();
    }
}
