package net.mehvahdjukaar.polytone.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

public class ClientFrameTicker {

    private static double time;
    private static double dayTime;
    private static float rainAndThunder;
    private static int skyLight;
    private static int blockLight;
    private static BlockPos cameraPos = BlockPos.ZERO;
    private static float temperature;
    private static float downfall;

    public static void onRenderTick(Minecraft mc) {
        Level level = mc.level;
        if (level == null) return;
        float partialTicks = mc.getFrameTime();

        time = level.getGameTime() + partialTicks;
        dayTime = level.getDayTime() + partialTicks;
        rainAndThunder = level.getRainLevel(partialTicks) * 0.5f + level.getThunderLevel(partialTicks) * 0.5f;

        cameraPos = mc.gameRenderer.getMainCamera().getBlockPosition();
    }

    public static void onTick(Level level){
        if(cameraPos != null) {
            skyLight = level.getBrightness(LightLayer.SKY, cameraPos);
            blockLight = level.getBrightness(LightLayer.BLOCK, cameraPos);
            var biome = level.getBiome(cameraPos);
            temperature = ColorUtils.getClimateSettings(biome.value()).temperature;
            downfall = ColorUtils.getClimateSettings(biome.value()).downfall;
        }
    }

    public static float getRainAndThunder() {
        return rainAndThunder;
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

    public static int getBlockLight() {
        return blockLight;
    }

    public static int getSkyLight() {
        return skyLight;
    }

    public static float getTemperature() {
        return temperature;
    }

    public static float getDownfall() {
        return downfall;
    }
}
