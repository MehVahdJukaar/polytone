package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

//move to global?
public class ExpTicker {

    private static double dayTime;
    private static float rainAndThunder;
    private static float season;

    private static DimensionType lastDimType;
    private static Screen lastScreen = null;
    private static int screenTime;

    public static void onTick(Level level) {
        screenTime++;
        Minecraft mc = Minecraft.getInstance();
        if (level == null) return;
        if (level.dimensionType() != lastDimType) {
            lastDimType = level.dimensionType();
            Polytone.onDimChanged(level);
        }

        //TODO: oter param like moon pos
        rainAndThunder = level.getRainLevel(0) * 0.5f + level.getThunderLevel(0) * 0.5f;
        season = ISeason.getNumber(level);

        if (mc.gui.screen() != lastScreen) {
            lastScreen = mc.gui.screen();
            screenTime = 0;
        }
    }


    public static int getGuiTime() {
        return screenTime;
    }

    public static float getRainAndThunder() {
        return rainAndThunder;
    }

    public static float getSeasonNumber() {
        return season;
    }
}
