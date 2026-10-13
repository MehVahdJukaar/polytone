package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;

public class PolyGlobalUniforms implements IShaderModifier, AutoCloseable {

    public static final String BLOCK_NAME = "PolyGlobals";

    private static final List<String> NAMES = List.of(BLOCK_NAME);

    public static final int UBO_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .putFloat()
            .putFloat()
            .putFloat()
            .putIVec3()
            .putVec3()
            .get();

    //lazy, no gpu device yet at static init
    @Nullable
    private GpuBuffer buffer = null;
    private boolean boundThisFrame = false;

    // once per frame from the main level pass, while no render pass is open
    public void capture(Matrix4fc projectionMat, Matrix4fc viewMat, float deltaTime) {
        boolean wanted = boundThisFrame;
        boundThisFrame = false;
        if (!wanted && !Polytone.isDevEnv) return;

        Minecraft mc = Minecraft.getInstance();
        float sunAngle = mc.levelRenderer.levelRenderState.skyRenderState.sunAngle;
        float dayTime = (float) ClientFrameTicker.getDayTime();
        //we lerp player feet position and split like vanilla's CameraBlockPos/CameraOffset for more precision
        Vec3 playerPos = mc.player == null ? Vec3.ZERO
                : mc.player.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        BlockPos playerBlockPos = BlockPos.containing(playerPos);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bytes = Std140Builder.onStack(stack, UBO_SIZE)
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
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer().slice(), bytes);
        }
    }

    @Override
    public List<String> getEnablingUniforms() {
        return NAMES;
    }

    @Override
    public void bindTo(IShader shader) {
        boundThisFrame = true;
        shader.setUniform(BLOCK_NAME, buffer().slice());
    }

    private GpuBuffer buffer() {
        if (buffer == null) {
            buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone Global Settings UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);
        }
        return buffer;
    }


    @Override
    public void close() {
        if (buffer == null) return;
        buffer.close();
        buffer = null;
    }
}
