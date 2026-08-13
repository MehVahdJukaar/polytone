package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

// The PolyShadow UBO bound to post passes that declare it (see PostChainsManager), written once per frame
// by ShadowMapRenderer while no render pass is open:
//     mat4 PolyShadowMat;       light view-projection, camera-relative space
//     vec3 PolyShadowLightDir;  unit direction toward the light
//     vec3 PolyShadowCamFract;  fract(cameraPos), the world-grid anchor
public class PolyShadowUniforms implements AutoCloseable {

    public static final int UBO_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putVec3()
            .putVec3()
            .get();

    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone Shadow UBO",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);

    public void update(Matrix4f shadowMat, Vector3f lightDir, Vector3f camFract) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UBO_SIZE)
                    .putMat4f(shadowMat)
                    .putVec3(lightDir.x, lightDir.y, lightDir.z)
                    .putVec3(camFract.x, camFract.y, camFract.z)
                    .get();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(this.buffer.slice(), byteBuffer);
        }
    }

    public GpuBufferSlice getSlice() {
        return buffer.slice();
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}
