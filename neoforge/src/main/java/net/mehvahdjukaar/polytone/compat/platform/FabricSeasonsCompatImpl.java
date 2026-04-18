package net.mehvahdjukaar.polytone.compat.platform;

import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.world.level.Level;

public class FabricSeasonsCompatImpl {
    public static ISeason ge2tSeason(Level level) {
        return ISeason.SPRING;
    }

    public static float getSeasonNumber(Level level) {
        return 0;
    }

}
