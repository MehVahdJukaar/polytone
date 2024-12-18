package net.mehvahdjukaar.polytone.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface DayTimeTexture {

    Mode polytone$getMode();

    void polytone$setMode(Mode mode);

    int polytone$getTimeCycleDuration();

    void polytone$setTimeCycleDuration(int duration);

    enum Mode implements StringRepresentable {
        VANILLA, GAME_TIME, DAY_TIME, WEATHER;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static Mode byName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return VANILLA;
            }
        }

        public static Mode get(@Nullable JsonElement json) {
            if (json != null && json.isJsonPrimitive()) {
                return byName(json.getAsString());
            } else {
                return VANILLA;
            }
        }
    }
}
