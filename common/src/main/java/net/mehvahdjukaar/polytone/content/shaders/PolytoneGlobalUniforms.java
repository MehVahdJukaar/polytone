package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public class PolytoneGlobalUniforms implements AutoCloseable {
    public static final int UBO_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .putFloat()
            .putFloat()
            .get();

    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone Global Settings UBO",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);

    public void update(Matrix4fc projectionMat, Matrix4fc viewMat, float sunAngle, float dayTime) {

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UBO_SIZE)
                    .putMat4f(projectionMat)
                    .putMat4f(viewMat)
                    .putFloat(sunAngle- Mth.HALF_PI)
                    .putFloat(dayTime)
                    .get();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(this.buffer.slice(), byteBuffer);
        }
    }

    public GpuBufferSlice getSlice() {
        return buffer.slice();
    }

    public void close() {
        this.buffer.close();
    }
}
