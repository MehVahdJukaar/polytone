package net.mehvahdjukaar.polytone.compat.platform;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.world.level.Level;


public class FabricSeasonsCompatImpl {

    public static ISeason getSeason(Level level) {
        Season currentSeason = FabricSeasons.getCurrentSeason(level);
        return switch (currentSeason) {
            case SPRING -> ISeason.SPRING;
            case SUMMER -> ISeason.SUMMER;
            case FALL -> ISeason.AUTUMN;
            case WINTER ->ISeason. WINTER;
        };
    }

    public static float getSeasonNumber(Level level){
        return FabricSeasons.getCurrentSeason(level).ordinal() / 3f;
    }

}
