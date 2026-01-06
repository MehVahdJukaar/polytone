package net.mehvahdjukaar.polytone.common;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class ClientFrameTicker {

    private static double time;
    private static double sunTime;
    private static double dayTime;
    private static BlockPos cameraPos = BlockPos.ZERO;
    public static Holder<Biome> cameraBiome;
    private static float temperature;
    private static float downfall;
    private static double playerSpeed = 0;

    public static void onRenderTick(Minecraft mc) {
        Level level = mc.level;
        if (level == null) return;
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        Camera camera = mc.gameRenderer.getMainCamera();
        var probe = camera.attributeProbe();

        time = level.getGameTime() + partialTicks;
        dayTime = level.dimensionType().hasFixedTime() ? level.getDayTime() : level.getDayTime() + partialTicks;
        sunTime = probe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks) / 360.0F;
        //TODO: oter param like moon pos

        cameraPos = camera.blockPosition();
        cameraBiome = level.getBiome(cameraPos);

        if (mc.player != null)
            playerSpeed = mc.player.getDeltaMovement().lengthSqr();

    }

    public static void onTick(Level level) {
        if (cameraPos != null) {
            var biome = level.getBiome(cameraPos);
            temperature = ColorUtils.getClimateSettings(biome.value()).temperature;
            downfall = ColorUtils.getClimateSettings(biome.value()).downfall;
        }
    }

    public static double getDayTime() {
        return dayTime;
    }

    public static double getGameTime() {
        return time;
    }

    public static BlockPos getCameraPos() {
        return cameraPos;
    }

    public static float getTemperature() {
        return temperature;
    }

    public static float getDownfall() {
        return downfall;
    }

    public static Holder<Biome> getCameraBiome() {
        if (cameraBiome == null) {
            //mega dumb. idk why this can be called before tick
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            cameraPos = mc.gameRenderer.getMainCamera().blockPosition();
            cameraBiome = level.getBiome(cameraPos);
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
