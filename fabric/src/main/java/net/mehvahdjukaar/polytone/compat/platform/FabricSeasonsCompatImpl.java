package net.mehvahdjukaar.polytone.compat.platform;

import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.world.level.Level;

// Stub - no Fabric Seasons 26.1 build available yet
public class FabricSeasonsCompatImpl {

    public static ISeason getSeason(Level level) {
        return ISeason.SUMMER;
    }

    public static float getSeasonNumber(Level level) {
        return 0;
    }

}
