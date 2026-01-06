package net.mehvahdjukaar.polytone.common.exp;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

//move to global?
public class ExpTicker {

    private static double time;
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

        time = level.getGameTime();
        dayTime = level.dimensionType().hasFixedTime() ? level.getDayTime() : level.getDayTime();
        //TODO: oter param like moon pos
        rainAndThunder = level.getRainLevel(0) * 0.5f + level.getThunderLevel(0) * 0.5f;
        season = ISeason.getNumber(level);

        deltaTime = Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
        if (mc.screen != lastScreen) {
            lastScreen = mc.screen;
            screenTime = 0;
        }
    }


}
