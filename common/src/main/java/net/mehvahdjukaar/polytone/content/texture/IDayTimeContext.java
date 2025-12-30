package net.mehvahdjukaar.polytone.content.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.misc.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface IDayTimeContext {

    Mode polytone$getMode();

    void polytone$setMode(Mode mode);

    int polytone$getTimeCycleDuration();

    void polytone$setTimeCycleDuration(int duration);

    enum Mode implements StringRepresentable {
        VANILLA, GAME_TIME, DAY_TIME, WEATHER, SCREEN_TIME;

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

        public @Nullable Float getDelta(float timeCycleDuration) {
            Level level = Minecraft.getInstance().level;
            if (level == null) return null;

            return switch (this) {
                case WEATHER -> {
                    float rainAndThunder = ClientFrameTicker.getRainAndThunder() * 2 / 3f;
                    yield rainAndThunder + 1 / 6;
                }
                //needs to fall in between those 2 so we dont get interpolation as this stuff doesnt loop back
                case GAME_TIME -> {
                    double gameTime = level.getGameTime() % timeCycleDuration;
                    yield (float) (gameTime / timeCycleDuration);
                }
                case SCREEN_TIME -> Math.min(1, (ClientFrameTicker.getGuiTime() / timeCycleDuration));
                default -> {
                    double dayTime = ClientFrameTicker.getDayTime() % timeCycleDuration;
                    yield (float) (dayTime / timeCycleDuration);
                }
            };
        }
    }
}
