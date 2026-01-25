package net.mehvahdjukaar.polytone.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.colormap.ColormapExpressionProvider;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface IDeltaProvider {
    Codec<IDeltaProvider> CODEC = CodecUtils.alternatives(
            PresetProvider.CODEC, ExpProvider.CODEC);

    @Nullable Float getDelta(float timeCycleDuration);


    record ExpProvider(ColormapExpressionProvider exp) implements IDeltaProvider {
        public static final Codec<ExpProvider> CODEC = ColormapExpressionProvider.CODEC
                .xmap(ExpProvider::new, ExpProvider::exp);

        @Override
        public Float getDelta(float timeCycleDuration) {
            //expression evaluation not implemented yet
            return exp.getValue(null, null, null, null, null);
        }
    }


    enum PresetProvider implements StringRepresentable, IDeltaProvider {

        VANILLA, GAME_TIME, DAY_TIME, WEATHER, SCREEN_TIME, MOON_PHASE;

        public static final Codec<PresetProvider> CODEC = StringRepresentable.fromEnum(PresetProvider::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static PresetProvider byName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return VANILLA;
            }
        }

        public static PresetProvider get(@Nullable JsonElement json) {
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
                case MOON_PHASE -> {
                    int phase = level.getMoonPhase();
                    yield phase / 8f;
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
