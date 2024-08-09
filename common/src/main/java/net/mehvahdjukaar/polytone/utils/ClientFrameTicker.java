package net.mehvahdjukaar.polytone.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class ClientFrameTicker {

    private static double time;
    private static double dayTime;
    private static float rainAndThunder;

    public static void onRenderTick(Minecraft mc) {
        Level level = mc.level;
        if (level == null) return;
        float partialTicks = mc.getFrameTime();

        time = level.getGameTime() + partialTicks;
        dayTime = level.getDayTime() + partialTicks;
        rainAndThunder = level.getRainLevel(partialTicks) * 0.5f + level.getThunderLevel(partialTicks) * 0.5f;
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
}
