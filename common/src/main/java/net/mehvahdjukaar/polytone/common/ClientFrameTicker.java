package net.mehvahdjukaar.polytone.common;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class ClientFrameTicker {

    private static double time;
    private static double sunTime;
    private static double dayTime;
    public static Holder<Biome> cameraBiome;
    private static float temperature;
    private static float downfall;
    private static double playerSpeed = 0;

    private static WeakReference<Entity> lastEntity = new WeakReference<>(null);

    public static void setLastEntity(Entity entity) {
        lastEntity = new WeakReference<>(entity);
    }

    @Nullable
    public static Entity getLastEntity() {
        Entity e = lastEntity.get();
        if (e == null || e.isRemoved()) return null;
        return e;
    }

    public static void onRenderTick(Minecraft mc) {
        Level level = mc.level;
        if (level == null) return;
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        Camera camera = mc.gameRenderer.mainCamera();
        var probe = camera.attributeProbe();

        time = level.getGameTime() + partialTicks;
        sunTime = probe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks) / 360.0F;
        long clockTime = level.getDefaultClockTime();
        dayTime = level.dimensionType().hasFixedTime() ? clockTime : clockTime + partialTicks;
        //TODO: other param like moon pos

        cameraBiome = level.getBiome(getCameraBlockPos());

        if (mc.player != null) {
            playerSpeed = mc.player.getDeltaMovement().lengthSqr();
        }
    }

    public static void onTick(Level level) {
        BlockPos cameraPos = getCameraBlockPos();
        var biome = level.getBiome(cameraPos);
        temperature = ColorUtils.getClimateSettings(biome.value()).temperature();
        downfall = ColorUtils.getClimateSettings(biome.value()).downfall();
    }

    public static double getDayTime() {
        return dayTime;
    }

    public static double getGameTime() {
        return time;
    }

    public static BlockPos getCameraBlockPos() {
        return Minecraft.getInstance().gameRenderer.mainCamera().blockPosition();
    }

    public static float getTemperature() {
        return temperature;
    }

    public static float getDownfall() {
        return downfall;
    }

    public static Vec3 getCameraPos() {
        return Minecraft.getInstance().gameRenderer.mainCamera().position();
    }

    public static Holder<Biome> getCameraBiome() {
        if (cameraBiome == null) {
            //mega dumb. idk why this can be called before tick
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            cameraBiome = level.getBiome(getCameraBlockPos());
        }
        return cameraBiome;
    }

    public static double getSunTime() {
        return sunTime;
    }

    public static double getPlayerSpeed() {
        return playerSpeed;
    }

    public static double getRenderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }
}
