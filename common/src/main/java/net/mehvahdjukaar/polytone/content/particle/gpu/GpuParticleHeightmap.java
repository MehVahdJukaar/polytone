package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class GpuParticleHeightmap implements AutoCloseable {

    public static final int SIZE = 128;
    private static final int REBUILD_INTERVAL_TICKS = 10;
    private static final int RECENTER_DISTANCE = 24;

    private final DynamicTexture texture = new DynamicTexture(SIZE, SIZE, false);
    private int originX = Integer.MIN_VALUE;
    private int originZ = Integer.MIN_VALUE;
    private int minY;
    private long lastRebuildTick = Long.MIN_VALUE;

    public GpuParticleHeightmap() {
        // without this the default min filter wants mipmaps we never upload
        texture.setFilter(false, false);
    }

    public void update(ClientLevel level, Vec3 cameraPos) {
        long tick = level.getGameTime();
        int centerX = Mth.floor(cameraPos.x);
        int centerZ = Mth.floor(cameraPos.z);
        boolean farFromCenter = originX == Integer.MIN_VALUE
                || Math.abs(centerX - (originX + SIZE / 2)) > RECENTER_DISTANCE
                || Math.abs(centerZ - (originZ + SIZE / 2)) > RECENTER_DISTANCE;
        boolean stale = tick - lastRebuildTick >= REBUILD_INTERVAL_TICKS;
        if (!farFromCenter && !stale) return;

        if (farFromCenter) {
            originX = centerX - SIZE / 2;
            originZ = centerZ - SIZE / 2;
        }
        lastRebuildTick = tick;
        minY = level.getMinBuildHeight();
        NativeImage image = texture.getPixels();
        if (image == null) return;
        for (int dz = 0; dz < SIZE; dz++) {
            for (int dx = 0; dx < SIZE; dx++) {
                // unloaded chunks report minY, which never culls anything
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, originX + dx, originZ + dz) - minY;
                h = Mth.clamp(h, 0, 0xFFFF);
                // ABGR here, not ARGB: r is the low byte, g the high one
                image.setPixelRGBA(dx, dz, 0xFF000000 | ((h >> 8) << 8) | (h & 0xFF));
            }
        }
        texture.upload();
    }

    public int textureId() {
        return texture.getId();
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    public int minY() {
        return minY;
    }

    @Override
    public void close() {
        texture.close();
    }
}
