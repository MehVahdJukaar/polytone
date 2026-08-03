package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public class PolytoneGlobalUniforms implements AutoCloseable {
    public static final int UBO_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .putFloat()
            .putFloat()
            .putFloat()
            .putIVec3()
            .putVec3()
            .get();

    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone Global Settings UBO",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);

    public void update(Matrix4fc projectionMat, Matrix4fc viewMat, float sunAngle, float dayTime, float deltaTime) {

        // lerped player (feet) position, split like vanilla's CameraBlockPos/CameraOffset so shaders
        // keep float precision at large coordinates: exact = vec3(PolyPlayerBlockPos) - PolyPlayerOffset
        Minecraft mc = Minecraft.getInstance();
        Vec3 playerPos = mc.player == null ? Vec3.ZERO
                : mc.player.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        BlockPos playerBlockPos = BlockPos.containing(playerPos);

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UBO_SIZE)
                    .putMat4f(projectionMat)
                    .putMat4f(viewMat)
                    .putFloat(sunAngle - Mth.HALF_PI)
                    .putFloat(dayTime)
                    .putFloat(deltaTime)
                    .putIVec3(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ())
                    .putVec3((float) (playerBlockPos.getX() - playerPos.x),
                            (float) (playerBlockPos.getY() - playerPos.y),
                            (float) (playerBlockPos.getZ() - playerPos.z))
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
