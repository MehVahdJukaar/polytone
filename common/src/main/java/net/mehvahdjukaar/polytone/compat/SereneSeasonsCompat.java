package net.mehvahdjukaar.polytone.compat;

import net.minecraft.world.level.Level;
import sereneseasons.api.season.SeasonHelper;

public class SereneSeasonsCompat {

    public static float getSeason(Level level) {
        return SeasonHelper.getSeasonState(level).getSubSeason().ordinal() / 11f;
    }

}
