package net.mehvahdjukaar.polytone.content.shaders.light;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;

public class PolyShaderPointLights implements PointLightStorage, AutoCloseable {

    //must match polytone_point_lights.glsl
    public static final int MAX_LIGHTS = 64;
    public static final String BLOCK_NAME = "PolyPointLights";

    private static final int UBO_SIZE;

    static {
        Std140SizeCalculator size = new Std140SizeCalculator().putIVec4();
        for (int i = 0; i < MAX_LIGHTS * 2; i++) size.putVec4();
        UBO_SIZE = size.get();
    }

    private final Map<Object, LightAt> lights = new HashMap<>();
    private int generation;
    private int uploadedGeneration = -1;
    //positions go in relative to this, the shader adds the camera offset from vanilla Globals
    private BlockPos uploadedAnchor = BlockPos.ZERO;

    @Nullable
    private GpuBuffer buffer = null;
    @Nullable
    private GpuBuffer emptyBuffer = null;

    @Override
    public void clear() {
        lights.clear();
        generation++;
    }

    //renderLevel HEAD, no render pass open
    public void upload(Vec3 camera) {
        if (lights.isEmpty() && buffer == null) return;
        BlockPos anchor = BlockPos.containing(camera);
        boolean nearestMayChange = lights.size() > MAX_LIGHTS && !anchor.equals(uploadedAnchor);
        if (uploadedGeneration == generation && !nearestMayChange) return;
        uploadedGeneration = generation;
        uploadedAnchor = anchor;

        List<LightAt> all = new ArrayList<>(lights.values());
        if (all.size() > MAX_LIGHTS) {
            all.sort(Comparator.comparingDouble(l -> camera.distanceToSqr(l.x, l.y, l.z)));
        }
        int count = Math.min(all.size(), MAX_LIGHTS);
        if (buffer == null) {
            buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone point lights UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, UBO_SIZE)
                    //count sits in the ivec3 padding on the glsl side
                    .putIVec4(anchor.getX(), anchor.getY(), anchor.getZ(), count);
            for (int i = 0; i < MAX_LIGHTS; i++) {
                if (i >= count) {
                    builder.putVec4(0, 0, 0, 0);
                    continue;
                }
                LightAt l = all.get(i);
                builder.putVec4((float) (l.x - anchor.getX()), (float) (l.y - anchor.getY()), (float) (l.z - anchor.getZ()), l.light.radius());
            }
            for (int i = 0; i < MAX_LIGHTS; i++) {
                if (i >= count) {
                    builder.putVec4(0, 0, 0, 0);
                    continue;
                }
                ResolvedPointLight light = all.get(i).light;
                int color = light.color();
                float scale = light.brightness() / 255f;
                builder.putVec4(ARGB.red(color) * scale, ARGB.green(color) * scale, ARGB.blue(color) * scale, 0);
            }
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), builder.get());
        }
    }

    public void bindTo(IShader shader) {
        //GUI items and the hand aren't in wotld space, and a portal view into another level isnt ours
        boolean inOurLevel = (LevelRenderPassTracker.isInLevelPass() || Polytone.POST_CHAINS.isProcessing())
                && LevelRenderPassTracker.currentLevel() == Minecraft.getInstance().level;
        boolean hasLights = buffer != null && !lights.isEmpty();
        shader.setUniform(BLOCK_NAME, inOurLevel && hasLights && !CompatHandler.irisShaderPackActive()
                ? buffer.slice() : emptyBuffer());
    }

    //inside a render pass here so no writeToBuffer, the zeros go in at creation
    private GpuBufferSlice emptyBuffer() {
        if (emptyBuffer == null) {
            ByteBuffer zeros = MemoryUtil.memCalloc(UBO_SIZE);
            emptyBuffer = RenderSystem.getDevice().createBuffer(() -> "Polytone empty point lights UBO", GpuBuffer.USAGE_UNIFORM, zeros);
            MemoryUtil.memFree(zeros);
        }
        return emptyBuffer.slice();
    }

    @Override
    public void close() {
        if (buffer != null) buffer.close();
        if (emptyBuffer != null) emptyBuffer.close();
        buffer = null;
        emptyBuffer = null;
        uploadedGeneration = -1;
    }

    @Override
    public void set(Object key, double x, double y, double z, ResolvedPointLight light) {
        LightAt l = lights.computeIfAbsent(key, k -> new LightAt());
        l.setPos(x,y,z);
        l.light = light;
        generation++;
    }

    @Override
    public void move(Object key, double x, double y, double z) {
        LightAt l = lights.get(key);
        if (l == null) return;
        l.setPos(x,y,z);
        generation++;
    }

    @Override
    public void remove(Object key) {
        if (lights.remove(key) != null) generation++;
    }

    private static class LightAt {
        double x, y, z;
        ResolvedPointLight light;

        private void setPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
