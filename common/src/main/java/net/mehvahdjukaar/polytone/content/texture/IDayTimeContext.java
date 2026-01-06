package net.mehvahdjukaar.polytone.content.texture;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface IDayTimeContext {

    ITextureDeltaProvider polytone$getDeltaProvider();

    void polytone$setDeltaProvider(ITextureDeltaProvider mode);

    int polytone$getTimeCycleDuration();

    void polytone$setTimeCycleDuration(int duration);

    interface ITextureDeltaProvider{
        Codec<ITextureDeltaProvider> CODEC = CodecUtils.withAlternative(PolyDeltaProvider.CODEC,
                ExpressionDeltaProvider.CODEC);

        @Nullable Float getDelta(float timeCycleDuration);
    }

    record ExpressionDeltaProvider(ISimpleExp exp) implements ITextureDeltaProvider {
        public static final Codec<ExpressionDeltaProvider> CODEC = ISimpleExp.CODEC
                .xmap(ExpressionDeltaProvider::new, ExpressionDeltaProvider::exp);

        @Override
        public Float getDelta(float timeCycleDuration) {
            //expression evaluation not implemented yet
            return (float) exp.evaluate();
        }
    }

    enum PolyDeltaProvider implements StringRepresentable, ITextureDeltaProvider{
        VANILLA, GAME_TIME, DAY_TIME, WEATHER, SCREEN_TIME;

        public static final Codec<PolyDeltaProvider> CODEC = StringRepresentable.fromEnum(PolyDeltaProvider::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static PolyDeltaProvider byName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return VANILLA;
            }
        }

        public static PolyDeltaProvider get(@Nullable JsonElement json) {
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
                    double gameTime = level.getGameTime() % timeCycleDuration;
                    yield (float) (gameTime / timeCycleDuration);
                }
                case SCREEN_TIME -> Math.min(1, (ExpTicker.getGuiTime() / timeCycleDuration));
                default -> {
                    double dayTime = ClientFrameTicker.getDayTime() % timeCycleDuration;
                    yield (float) (dayTime / timeCycleDuration);
                }
            };
        }
    }
}
