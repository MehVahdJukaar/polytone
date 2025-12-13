package net.mehvahdjukaar.polytone.compat;

import io.github.lucaargolo.seasons.FabricSeasons;
import net.minecraft.world.level.Level;

public class FabricSeasonsCompat {

    public static float getSeason(Level level) {
        return FabricSeasons.getCurrentSeason(level).ordinal() / 3f;
    }
}
