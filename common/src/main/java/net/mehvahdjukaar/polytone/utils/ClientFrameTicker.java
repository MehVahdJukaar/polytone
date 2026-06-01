package net.mehvahdjukaar.polytone.utils;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

public class ClientFrameTicker {

    private static double time;
    private static double timeOfDay;
    private static double dayTime;
    private static float rainAndThunder;
    private static float season;
    private static int skyLight;
    private static int blockLight;
    private static BlockPos cameraPos = BlockPos.ZERO;
    public static Holder<Biome> cameraBiome;
    private static float temperature;
    private static float downfall;
    private static float deltaTime;
    private static double playerSpeed;

    private static DimensionType lastDImType;
    private static Screen lastScreen;
    private static float screenTime;

    public static void onRenderTick(Minecraft mc) {
        Level level = mc.level;
        if (level == null) return;
        if (level.dimensionType() != lastDImType) {
            lastDImType = level.dimensionType();
            Polytone.onDimChanged(level);
        }
        float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(false);

        time = level.getGameTime() + partialTicks;
        dayTime = level.dimensionType().fixedTime().orElse(level.getDayTime()) + partialTicks;
        timeOfDay = level.getTimeOfDay(partialTicks);
        rainAndThunder = level.getRainLevel(partialTicks) * 0.5f + level.getThunderLevel(partialTicks) * 0.5f;
        season = PlatStuff.compatSSGetSeason(level);

        cameraPos = mc.gameRenderer.getMainCamera().getBlockPosition();
        cameraBiome = level.getBiome(cameraPos);

        deltaTime = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        playerSpeed =  mc.player.getDeltaMovement().lengthSqr();

      if ( mc.screen != lastScreen) {
            lastScreen = mc.screen;
            screenTime = 0;
        }
    }

    public static void onTick(Level level) {
        if (cameraPos != null) {
            skyLight = level.getBrightness(LightLayer.SKY, cameraPos);
            blockLight = level.getBrightness(LightLayer.BLOCK, cameraPos);
            var biome = level.getBiome(cameraPos);
            temperature = ColorUtils.getClimateSettings(biome.value()).temperature;
            downfall = ColorUtils.getClimateSettings(biome.value()).downfall;
        }
        screenTime++;
        Polytone.POST_SHADERS.tick();
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

    public static Holder<Biome> getCameraBiome() {
        return cameraBiome;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static double getSunTime() {
        return timeOfDay;
    }

    public static double getPlayerSpeed() {
        return playerSpeed;
    }

    public static double getRenderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    public static float getGuiTime() {
        return screenTime;
    }

    public static float getSeason() {
        return season;
    }
}
