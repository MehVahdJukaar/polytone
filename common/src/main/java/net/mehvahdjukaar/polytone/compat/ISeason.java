package net.mehvahdjukaar.polytone.compat;

import net.minecraft.world.level.Level;

import java.util.Locale;

public enum ISeason {
    SUMMER,
    SPRING,
    WINTER,
    AUTUMN;

    private final String name;

    ISeason() {
        name = this.name().toLowerCase(Locale.ROOT);
    }

    public String lowercaseName() {
        return name;
    }

    public static ISeason get(Level level) {
        return CompatHandler.SS ? SereneSeasonsCompat.getSeason(level) :
                (CompatHandler.FS ? FabricSeasonsCompat.getSeason(level) : SUMMER);
    }

    public static float getNumber(Level level) {
        return CompatHandler.SS ? SereneSeasonsCompat.getSeasonNumber(level) :
                (CompatHandler.FS ? FabricSeasonsCompat.getSeasonNumber(level) : 0);
    }
}
