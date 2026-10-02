package net.mehvahdjukaar.polytone.content.viewpoint;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

// std140, append only since shaders may declare a leading prefix:
// mat4 ViewProj (camera relative world -> viewpoint clip, reprojected every frame), vec4 ViewDir,
// vec4 CamFract (fract of camera pos), vec4 NearFarTexel (near, far, 1/w, 1/h),
// vec4 Update (rendered this frame, age s, interval s, phase). depth is reversed-Z like vanilla
public class ViewpointUniforms implements AutoCloseable {

    public static final int UBO_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();

    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone viewpoint UBO",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);

    public void update(Matrix4f viewProj, Vector3f viewDir, Vector3f camFract,
                       float near, float far, int width, int height,
                       boolean rendered, float ageSeconds, float intervalSeconds, float phase) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bb = Std140Builder.onStack(stack, UBO_SIZE)
                    .putMat4f(viewProj)
                    .putVec4(viewDir.x, viewDir.y, viewDir.z, 0f)
                    .putVec4(camFract.x, camFract.y, camFract.z, 0f)
                    .putVec4(near, far, 1f / Math.max(width, 1), 1f / Math.max(height, 1))
                    .putVec4(rendered ? 1f : 0f, ageSeconds, intervalSeconds, phase)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), bb);
        }
    }

    public GpuBufferSlice getSlice() {
        return buffer.slice();
    }

    @Override
    public void close() {
        buffer.close();
    }
}
