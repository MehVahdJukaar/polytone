package net.mehvahdjukaar.polytone.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class ClientFrameTicker {

    private static float time;
    private static float dayTime;
    private static float rainAndThunder;

    public static void onRenderTick(Minecraft mc) {
        Level level = mc.level;
        if (level == null) return;
        float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(false);

        time = level.getGameTime() + partialTicks;
        dayTime = level.getDayTime() + partialTicks;
        rainAndThunder = level.getRainLevel(partialTicks) * 0.5f + level.getThunderLevel(partialTicks) * 0.5f;
    }

    public static float getRainAndThunder() {
        return rainAndThunder;
    }

    public static float getDayTime() {
        return dayTime;
    }

    public static float getGameTime() {
        return time;
    }
}
