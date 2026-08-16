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

// The PolyShadow block:
//     mat4 PolyShadowMat;       light view-projection, camera-relative space
//     vec3 PolyShadowLightDir;  unit direction toward the light
//     vec3 PolyShadowCamFract;  fract(cameraPos), lets shaders snap to the world grid
public class PolyShadowUniforms implements AutoCloseable {

    public static final int UBO_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putVec3()
            .putVec3()
            .get();

    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone Shadow UBO",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);

    public void update(Matrix4f shadowMatrix, Vector3f towardLight, Vector3f cameraFract) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bytes = Std140Builder.onStack(stack, UBO_SIZE)
                    .putMat4f(shadowMatrix)
                    .putVec3(towardLight.x, towardLight.y, towardLight.z)
                    .putVec3(cameraFract.x, cameraFract.y, cameraFract.z)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytes);
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
