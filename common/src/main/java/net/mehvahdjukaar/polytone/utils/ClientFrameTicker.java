package net.mehvahdjukaar.polytone.utils;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.content.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.content.light.ColoredLightsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

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

        ColoredLightsTracker.onFrame(partialTicks);

        deltaTime = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        playerSpeed =  mc.player.getDeltaMovement().lengthSqr();

      if ( mc.screen != lastScreen) {
            lastScreen = mc.screen;
            screenTime = 0;
        }
    }

    public static void onTick(Level level) {
        // Client-only ticker (drives GL post-shader loading). Guard against being called with a
        // non-client level on a non-render thread (NeoForge's LevelTickEvent fires for the integrated
        // server level too) - otherwise off-thread GL calls poison the post shader chains.
        if (level != Minecraft.getInstance().level) return;
        // keep the async player-stats cache in step with the tick (cleared to null when no player)
        ExpTicker.refreshPlayerSnapshot();
        if (cameraPos != null) {
            skyLight = level.getBrightness(LightLayer.SKY, cameraPos);
            blockLight = level.getBrightness(LightLayer.BLOCK, cameraPos);
            var biome = level.getBiome(cameraPos);
            temperature = ColorUtils.getClimateSettings(biome.value()).temperature();
            downfall = ColorUtils.getClimateSettings(biome.value()).downfall();
        }
        screenTime++;
        Polytone.GLOBAL_EXPRESSION.tick(level);
        if (level instanceof net.minecraft.client.multiplayer.ClientLevel c) ColoredLightsTracker.onTick(c, cameraPos);
        Polytone.POST_SHADERS.tick();
        TokenBucketTracker.tick();
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
