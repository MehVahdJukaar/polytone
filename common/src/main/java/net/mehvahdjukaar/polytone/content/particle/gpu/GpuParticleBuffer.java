package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

// Ring buffer of particle records. There is no per-frame update: a spawn writes the four corners of
// its quad once and the vertex shader derives everything else from the record and the current time.
// All vertices of a record carry identical data; the shader tells corners (and, with an area, the
// quads of one spawn) apart by gl_VertexID.
// Positions are relative to origin and spawn ticks to timeBase; both are rebased (buffer cleared) when
// the camera or the clock drift far enough that float precision would suffer.
public final class GpuParticleBuffer implements AutoCloseable {

    public static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Velocity", GpuFormat.RGB32_FLOAT)
            .addAttribute("SpawnLife", GpuFormat.RG32_FLOAT)
            .addAttribute("Params", GpuFormat.RGBA32_FLOAT) // size, roll, custom, seed
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("UV2", GpuFormat.RG16_SINT)
            .build();

    private static final int VERTEX_BYTES = FORMAT.getVertexSize();
    private static final double REBASE_DISTANCE = 1024;
    private static final long REBASE_TICKS = 1L << 20;

    // absolute position and tick; made relative at upload so a rebase in between can't skew them
    private record Spawn(double x, double y, double z, float vx, float vy, float vz, long tick,
                         float seed, int packedLight, GpuParticleInitializer.SpawnValues values) {}

    private final int capacity;
    private final int quadsPerRecord;
    private final int recordBytes;
    private final List<Spawn> pendingSpawns = new ArrayList<>();
    private @Nullable GpuBuffer buffer;
    private int cursor = 0;
    private Vec3 origin = null;
    private long timeBase = 0;

    public GpuParticleBuffer(int capacity, int quadsPerRecord) {
        this.capacity = capacity;
        this.quadsPerRecord = quadsPerRecord;
        this.recordBytes = VERTEX_BYTES * 4 * quadsPerRecord;
    }

    // spawns arrive from the async particle tick threads, uploads happen on the render thread
    public void add(double x, double y, double z, float vx, float vy, float vz,
                    long tick, float seed, int packedLight, GpuParticleInitializer.SpawnValues values) {
        synchronized (pendingSpawns) {
            pendingSpawns.add(new Spawn(x, y, z, vx, vy, vz, tick, seed, packedLight, values));
        }
    }

    public Vec3 origin() {
        return origin;
    }

    public long timeBase() {
        return timeBase;
    }

    public GpuBuffer vertexBuffer() {
        return buffer;
    }

    public int vertexCount() {
        return capacity * quadsPerRecord * 4;
    }

    public void prepareForFrame(Vec3 cameraPos, long gameTime) {
        if (buffer == null) createStorage();
        boolean rebase = origin == null
                || cameraPos.distanceToSqr(origin) > REBASE_DISTANCE * REBASE_DISTANCE
                || gameTime - timeBase > REBASE_TICKS
                || gameTime < timeBase;
        if (rebase) {
            origin = new Vec3(Math.floor(cameraPos.x), Math.floor(cameraPos.y), Math.floor(cameraPos.z));
            timeBase = gameTime;
            cursor = 0;
            clearStorage();
        }
        uploadPending();
    }

    private void createStorage() {
        buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone gpu particle records",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, (long) capacity * recordBytes);
        origin = null;
    }

    private void clearStorage() {
        ByteBuffer zeros = MemoryUtil.memCalloc(capacity * recordBytes);
        try {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), zeros);
        } finally {
            MemoryUtil.memFree(zeros);
        }
    }

    private void uploadPending() {
        List<Spawn> batch;
        synchronized (pendingSpawns) {
            if (pendingSpawns.isEmpty()) return;
            // more spawns than slots: only the newest capacity ones can survive anyway
            int start = Math.max(0, pendingSpawns.size() - capacity);
            batch = new ArrayList<>(pendingSpawns.subList(start, pendingSpawns.size()));
            pendingSpawns.clear();
        }

        int count = batch.size();
        ByteBuffer bytes = MemoryUtil.memAlloc(count * recordBytes);
        try {
            for (Spawn s : batch) writeRecord(bytes, s);

            var encoder = RenderSystem.getDevice().createCommandEncoder();
            int untilEnd = Math.min(count, capacity - cursor);
            bytes.limit(untilEnd * recordBytes).position(0);
            encoder.writeToBuffer(buffer.slice((long) cursor * recordBytes, (long) untilEnd * recordBytes), bytes);
            if (untilEnd < count) {
                int rest = count - untilEnd;
                bytes.limit(count * recordBytes).position(untilEnd * recordBytes);
                encoder.writeToBuffer(buffer.slice(0, (long) rest * recordBytes), bytes);
            }
            cursor = (cursor + count) % capacity;
        } finally {
            MemoryUtil.memFree(bytes);
        }
    }

    private void writeRecord(ByteBuffer out, Spawn s) {
        GpuParticleInitializer.SpawnValues v = s.values;
        float x = (float) (s.x - origin.x);
        float y = (float) (s.y - origin.y);
        float z = (float) (s.z - origin.z);
        float spawn = s.tick - timeBase;
        for (int vertex = 0; vertex < 4 * quadsPerRecord; vertex++) {
            out.putFloat(x).putFloat(y).putFloat(z);
            out.putFloat(s.vx).putFloat(s.vy).putFloat(s.vz);
            out.putFloat(spawn).putFloat(v.lifetime);
            out.putFloat(v.size).putFloat(v.roll).putFloat(v.custom).putFloat(s.seed);
            out.put(toByte(v.red)).put(toByte(v.green)).put(toByte(v.blue)).put(toByte(v.alpha));
            out.putShort((short) (s.packedLight & 0xFFFF)).putShort((short) ((s.packedLight >> 16) & 0xFFFF));
        }
    }

    private static byte toByte(float unorm) {
        return (byte) Math.round(Math.max(0f, Math.min(1f, unorm)) * 255f);
    }

    @Override
    public void close() {
        if (buffer != null) buffer.close();
        buffer = null;
        synchronized (pendingSpawns) {
            pendingSpawns.clear();
        }
    }
}
