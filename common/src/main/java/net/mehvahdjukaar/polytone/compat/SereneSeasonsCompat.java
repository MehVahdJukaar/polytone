package net.mehvahdjukaar.polytone.compat;

import net.minecraft.world.level.Level;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class SereneSeasonsCompat {

    public static ISeason getSeason(Level level) {
        Season.SubSeason subSeason = SeasonHelper.getSeasonState(level).getSubSeason();
        return switch (subSeason.getSeason()){
            case SPRING -> ISeason.SPRING;
            case SUMMER -> ISeason.SUMMER;
            case AUTUMN -> ISeason.AUTUMN;
            case WINTER -> ISeason.WINTER;
        };
    }

    public static float getSeasonNumber(Level level) {
        return SeasonHelper.getSeasonState(level).getSubSeason().ordinal() / 11f;
    }
}
