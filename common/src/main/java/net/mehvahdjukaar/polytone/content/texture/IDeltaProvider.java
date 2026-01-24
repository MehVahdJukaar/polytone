package net.mehvahdjukaar.polytone.content.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface IDeltaProvider {
    Codec<IDeltaProvider> CODEC = CodecUtils.alternatives(PresetProvider.CODEC,
            ExpProvider.CODEC);

    @Nullable Float getDelta(float timeCycleDuration);



    record ExpProvider(ISimpleExp exp) implements IDeltaProvider {
        public static final Codec<ExpProvider> CODEC = ISimpleExp.CODEC
                .xmap(ExpProvider::new, ExpProvider::exp);

        @Override
        public Float getDelta(float timeCycleDuration) {
            //expression evaluation not implemented yet
            return (float) exp.evaluate();
        }
    }

    enum PresetProvider implements StringRepresentable, IDeltaProvider {
        VANILLA, GAME_TIME, DAY_TIME, WEATHER, SCREEN_TIME, MOON_PHASE, SEASON;

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
                    float rainAndThunder = ExpTicker.getRainAndThunder() * 2 / 3f;
                    yield rainAndThunder + 1 / 6;
                }
                //needs to fall in between those 2 so we dont get interpolation as this stuff doesnt loop back
                case GAME_TIME -> {
                    yield getTimeFract(timeCycleDuration, level);
                }
                case SEASON -> {
                    if (CompatHandler.SS || CompatHandler.FS) {
                        yield ExpTicker.getSeasonNumber();
                    } else {
                        yield getTimeFract(timeCycleDuration, level);
                    }
                }
                case MOON_PHASE -> {
                    int phase = level.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE)
                            .index();
                    yield phase / 8f;
                }
                case SCREEN_TIME -> Math.min(1, (ExpTicker.getGuiTime() / timeCycleDuration));
                default -> {
                    double dayTime = ClientFrameTicker.getDayTime() % timeCycleDuration;
                    yield (float) (dayTime / timeCycleDuration);
                }
            };
        }

        private static float getTimeFract(float timeCycleDuration, Level level) {
            double gameTime = level.getGameTime() % timeCycleDuration;
            return (float) (gameTime / timeCycleDuration);
        }
    }
}