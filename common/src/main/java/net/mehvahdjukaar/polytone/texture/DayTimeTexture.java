package net.mehvahdjukaar.polytone.texture;

import com.google.gson.JsonElement;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface DayTimeTexture {

    Mode polytone$getMode();

    void polytone$setMode(Mode mode);

    int polytone$getDayDuration();

    void polytone$setDayDuration(int duration);

    enum Mode implements StringRepresentable {
        GAME_TIME, DAY_TIME, WEATHER;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static Mode byName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return GAME_TIME;
            }
        }

        public static Mode get(@Nullable JsonElement json) {
            if (json != null && json.isJsonPrimitive()) {
                return byName(json.getAsString());
            } else {
                return GAME_TIME;
            }
        }
    }
}
