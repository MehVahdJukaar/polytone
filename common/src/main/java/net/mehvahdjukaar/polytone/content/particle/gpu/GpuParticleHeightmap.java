package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

// MOTION_BLOCKING heights of the SIZE x SIZE block square around the camera as an RGBA8 texture
// (r = low byte, g = high byte of height - minY), so the vertex shader can drop quads under a roof.
// Rebuilt every few ticks or when the camera leaves the middle of the square.
public final class GpuParticleHeightmap implements AutoCloseable {

    public static final int SIZE = 128;
    private static final int REBUILD_INTERVAL_TICKS = 10;
    private static final int RECENTER_DISTANCE = 24;

    private final DynamicTexture texture = new DynamicTexture(() -> "Polytone gpu particle heightmap", SIZE, SIZE, false);
    private int originX = Integer.MIN_VALUE;
    private int originZ = Integer.MIN_VALUE;
    private int minY;
    private long lastRebuildTick = Long.MIN_VALUE;

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
        minY = level.getMinY();
        NativeImage image = texture.getPixels();
        if (image == null) return;
        for (int dz = 0; dz < SIZE; dz++) {
            for (int dx = 0; dx < SIZE; dx++) {
                // unloaded chunks report minY, which never culls anything
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, originX + dx, originZ + dz) - minY;
                h = Mth.clamp(h, 0, 0xFFFF);
                image.setPixel(dx, dz, 0xFF000000 | ((h & 0xFF) << 16) | ((h >> 8) << 8));
            }
        }
        texture.upload();
    }

    public GpuTextureView textureView() {
        return texture.getTextureView();
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
