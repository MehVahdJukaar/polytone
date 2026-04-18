package net.mehvahdjukaar.polytone.compat;


import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.world.level.Level;

public class FabricSeasonsCompat {

    @PlatformImpl
    public static ISeason getSeason(Level level) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static float getSeasonNumber(Level level){
        throw new AssertionError();
    }
}
