package net.mehvahdjukaar.polytone.compat.platform;

import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.world.level.Level;

public class FabricSeasonsCompatImpl {
    public static float getSeasonNumber(Level level) {
        return 0;
    }

    public static ISeason getSeason(Level arg0) {
        return ISeason.SPRING;
    }

}
