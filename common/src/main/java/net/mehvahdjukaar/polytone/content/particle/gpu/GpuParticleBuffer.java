package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public final class GpuParticleBuffer implements AutoCloseable {

    public static final int FLOATS_PER_RECORD = 16;
    private static final int BYTES_PER_RECORD = FLOATS_PER_RECORD * Float.BYTES;
    private static final double REBASE_DISTANCE = 1024;
    private static final long REBASE_TICKS = 1L << 20;

    private record Spawn(double x, double y, double z, float vx, float vy, float vz, long tick,
                         float seed, int packedLight, GpuParticleInitializer.SpawnValues values) {}

    private final int capacity;
    private final List<Spawn> pendingSpawns = new ArrayList<>();
    private int glBuffer = -1;
    private int glTexture = -1;
    private int cursor = 0;
    private Vec3 origin = null;
    private long timeBase = 0;

    public GpuParticleBuffer(int capacity) {
        this.capacity = capacity;
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

    // render thread: creates the GL objects on first use, rebases if needed, uploads the queued spawns
    public void prepareForFrame(Vec3 cameraPos, long gameTime) {
        if (glBuffer == -1) createStorage();
        boolean rebase = origin == null
                || cameraPos.distanceToSqr(origin) > REBASE_DISTANCE * REBASE_DISTANCE
                || gameTime - timeBase > REBASE_TICKS
                || gameTime < timeBase;
        if (rebase) {
            origin = new Vec3(Math.floor(cameraPos.x), Math.floor(cameraPos.y), Math.floor(cameraPos.z));
            timeBase = gameTime;
            cursor = 0;
            GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, glBuffer);
            clearStorage();
            GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        }
        uploadPending();
    }

    private void createStorage() {
        glBuffer = GlStateManager._glGenBuffers();
        glTexture = GlStateManager._genTexture();
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, glTexture);
        GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, glBuffer);
        clearStorage();
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, glBuffer);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        origin = null;
    }

    private void clearStorage() {
        ByteBuffer zeros = MemoryUtil.memCalloc(capacity * BYTES_PER_RECORD);
        try {
            GlStateManager._glBufferData(GL31.GL_TEXTURE_BUFFER, zeros, GL15.GL_DYNAMIC_DRAW);
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
        ByteBuffer bytes = MemoryUtil.memAlloc(count * BYTES_PER_RECORD);
        try {
            FloatBuffer floats = bytes.asFloatBuffer();
            for (Spawn s : batch) writeRecord(floats, s);

            GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, glBuffer);
            int untilEnd = Math.min(count, capacity - cursor);
            bytes.limit(untilEnd * BYTES_PER_RECORD).position(0);
            GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, (long) cursor * BYTES_PER_RECORD, bytes);
            if (untilEnd < count) {
                bytes.limit(count * BYTES_PER_RECORD).position(untilEnd * BYTES_PER_RECORD);
                GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, bytes);
            }
            GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
            cursor = (cursor + count) % capacity;
        } finally {
            MemoryUtil.memFree(bytes);
        }
    }

    private void writeRecord(FloatBuffer out, Spawn s) {
        GpuParticleInitializer.SpawnValues v = s.values;
        float light = (s.packedLight & 0xFFFF) + ((s.packedLight >> 16) & 0xFFFF) * 256f;
        out.put((float) (s.x - origin.x)).put((float) (s.y - origin.y)).put((float) (s.z - origin.z)).put(s.vx);
        out.put(s.vy).put(s.vz).put((float) (s.tick - timeBase)).put(v.lifetime);
        out.put(s.seed).put(light + toByte(v.alpha) * 65536f)
                .put(toByte(v.red) + toByte(v.green) * 256f + toByte(v.blue) * 65536f).put(v.size);
        out.put(v.roll).put(v.custom).put(0f).put(0f);
    }

    private static float toByte(float unorm) {
        return Math.round(Math.clamp(unorm, 0f, 1f) * 255f);
    }

    public void bind(int textureUnit) {
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + textureUnit);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, glTexture);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    public void unbind(int textureUnit) {
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + textureUnit);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    @Override
    public void close() {
        if (glTexture != -1) GlStateManager._deleteTexture(glTexture);
        if (glBuffer != -1) GlStateManager._glDeleteBuffers(glBuffer);
        glTexture = -1;
        glBuffer = -1;
        synchronized (pendingSpawns) {
            pendingSpawns.clear();
        }
    }
}
