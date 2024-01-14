package net.mehvahdjukaar.polytone.lightmap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.HashMap;
import java.util.Map;

public interface LightmapNumberProvider {

    Map<String, LightmapNumberProvider> CUSTOM_PROVIDERS = new HashMap<>();

    Codec<LightmapNumberProvider> CODEC = Codec.STRING.flatXmap(s -> {
        LightmapNumberProvider custom = CUSTOM_PROVIDERS.get(s);
        if (custom != null) return DataResult.success(custom);
        try {
            ExpressionNumberProvider compiled = ExpressionNumberProvider.create(s);
            return DataResult.success(compiled);
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.error(() -> "Encoding not supporteed"));

    static <T extends LightmapNumberProvider> T register(String name, T provider) {
        CUSTOM_PROVIDERS.put(name, provider);
        return provider;
    }


    float getValue(float time, float rain, float thunder);

    RandomSource RAND = RandomSource.create();

    // Sine
    LightmapNumberProvider RANDOM = register("random",
            (time, rain, thunder) -> {
                RAND.setSeed(Float.floatToIntBits(time));
                return RAND.nextFloat();
            });

    // Same stuff that level.getSkyDarkens does
    // A slanted Step
    LightmapNumberProvider DEFAULT = register("default",
            (time, rain, thunder) -> {
                float g = 1.0F - (Mth.cos(time * Mth.TWO_PI) * 2.0F + 0.2F);
                g = Mth.clamp(g, 0.0F, 1.0F);
                g = 1.0F - g;
                // g *= 1.0F - rain * 5.0F / 16.0F;
                // g *= 1.0F - thunder * 5.0F / 16.0F;
                return g;
            });

    // Sine
    LightmapNumberProvider SMOOTH = register("smooth",
            (time, rain, thunder) -> 0.5f + (Mth.cos(time * Mth.TWO_PI) * 0.5f));

    // Triangle func
    LightmapNumberProvider LINEAR = register("linear",
            (time, rain, thunder) -> Mth.abs(1 - 2 * time));

    LightmapNumberProvider DEFAULT_2 = register("default",
            (time, rain, thunder) -> {
                float g = 1.0F - (Mth.cos(time * Mth.TWO_PI) * 2.0F + 0.2F);
                g = Mth.clamp(g, 0.0F, 1.0F);
                g = 1.0F - g;
                g *= 0.5;
                if (time > 0.5) {
                    return g;
                } else {
                    return 1 - g;
                }
            });

    // Sine Saw Tooth
    LightmapNumberProvider SMOOTH_2 = register("smooth_2",
            (time, rain, thunder) -> {
                if (time > 0.5) {
                    return 0.25f - (Mth.cos(time * Mth.TWO_PI) * 0.25f);
                } else {
                    return 0.75f + (Mth.cos(time * Mth.TWO_PI) * 0.25f);
                }
            });

    // Line (Saw Tooth)
    LightmapNumberProvider LINEAR_2 = register("linear_2",
            (time, rain, thunder) -> {
                float linear = Mth.abs(1 - 2 * time);
                if (time > 0.5) {
                    return linear * 0.5f;
                } else {
                    return 1 - linear * 0.5f;
                }
            });


    record ExpressionNumberProvider(Expression expression) implements LightmapNumberProvider {
        private static final String TIME = "TIME";
        private static final String RAIN = "RAIN";
        private static final String THUNDER = "THUNDER";

        public static ExpressionNumberProvider create(String s) {
            return new ExpressionNumberProvider(new ExpressionBuilder(s)
                    .variables(TIME, RAIN, THUNDER)
                    .operator(ExpressionUtils.defOp())
                    .build());
        }

        @Override
        public float getValue(float time, float rain, float thunder) {
            expression.setVariable(TIME, time);
            expression.setVariable(RAIN, rain);
            expression.setVariable(THUNDER, thunder);
            return 0;
        }
    }

}
