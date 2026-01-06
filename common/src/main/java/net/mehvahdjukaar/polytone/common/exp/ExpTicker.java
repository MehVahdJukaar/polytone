package net.mehvahdjukaar.polytone.common.exp;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ExpTicker {

    private static double time;
    private static double sunTime;
    private static double dayTime;
    private static float rainAndThunder;
    private static float season;
    private static float deltaTime;

    private static DimensionType lastDImType;
    private static Screen lastScreen = null;
    private static float screenTime;

    public static void onTick(Level level) {
        screenTime++;
        Minecraft mc = Minecraft.getInstance();
        if (level == null) return;
        if (level.dimensionType() != lastDImType) {
            lastDImType = level.dimensionType();
            Polytone.onDimChanged(level);
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        var probe = camera.attributeProbe();

        time = level.getGameTime();
        dayTime = level.dimensionType().hasFixedTime() ? level.getDayTime() : level.getDayTime() + partialTicks;
        sunTime = probe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks) / 360.0F;
        //TODO: oter param like moon pos
        rainAndThunder = level.getRainLevel(partialTicks) * 0.5f + level.getThunderLevel(partialTicks) * 0.5f;
        season = ISeason.getNumber(level);

        deltaTime = Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
        if (mc.screen != lastScreen) {
            lastScreen = mc.screen;
            screenTime = 0;
        }
    }


}
