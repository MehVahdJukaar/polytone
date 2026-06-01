package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

// On 1.21.1 the per-tick climate/season state is owned by ClientFrameTicker.
// This class is a thin facade so MVEL expressions can read it under the same API as 1.21.11.
public class ExpTicker {

    private static Screen lastScreen = null;
    private static int screenTime;

    public static void onTick() {
        screenTime++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != lastScreen) {
            lastScreen = mc.screen;
            screenTime = 0;
        }
    }


    public static int getGuiTime() {
        return screenTime;
    }

    public static float getRainAndThunder() {
        return ClientFrameTicker.getRainAndThunder();
    }

    public static float getSeasonNumber() {
        return ClientFrameTicker.getSeason();
    }
}
