package net.mehvahdjukaar.polytone.lightmap;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public interface ILightmapNumberProvider {

    MapRegistry<ILightmapNumberProvider> BUILTIN_PROVIDERS = new MapRegistry<>("Lightmap Number Providers");

    Codec<ILightmapNumberProvider> CODEC = new ReferenceOrDirectCodec<>(BUILTIN_PROVIDERS,
            LightmapContextExpression.CODEC, true);


    double getValue(float time, float rain, float thunder);

    RandomSource RAND = RandomSource.create();

    // Sine
    ILightmapNumberProvider RANDOM = BUILTIN_PROVIDERS.register("random",
            (time, rain, thunder) -> {
                RAND.setSeed(Float.floatToIntBits(time));
                return RAND.nextFloat();
            });

    // Same stuff that level.getSkyDarkens does
    // A slanted Step
    ILightmapNumberProvider DEFAULT = BUILTIN_PROVIDERS.register("default",
            (time, rain, thunder) -> {
                double g = 1.0 - (Mth.cos(time * Mth.TWO_PI) * 2.0 + 0.2);
                g = Mth.clamp(g, 0.0, 1.0);
                g = 1.0 - g;
                // g *= 1.0F - rain * 5.0F / 16.0F;
                // g *= 1.0F - thunder * 5.0F / 16.0F;
                return g;
            });

    // Sine
    ILightmapNumberProvider SMOOTH = BUILTIN_PROVIDERS.register("smooth",
            (time, rain, thunder) -> 0.5 + (Mth.cos(time * Mth.TWO_PI) * 0.5));

    // Triangle func
    ILightmapNumberProvider LINEAR = BUILTIN_PROVIDERS.register("linear",
            (time, rain, thunder) -> Mth.abs(1 - 2 * time));

    ILightmapNumberProvider DEFAULT_2 = BUILTIN_PROVIDERS.register("default",
            (time, rain, thunder) -> {
                double g = 1.0 - (Mth.cos(time * Mth.TWO_PI) * 2.0 + 0.2);
                g = Mth.clamp(g, 0.0, 1.0);
                g = 1.0 - g;
                g *= 0.5;
                if (time > 0.5) {
                    return g;
                } else {
                    return 1 - g;
                }
            });

    // Sine Saw Tooth
    ILightmapNumberProvider SMOOTH_2 = BUILTIN_PROVIDERS.register("smooth_2",
            (time, rain, thunder) -> {
                if (time > 0.5) {
                    return 0.25 - (Mth.cos(time * Mth.TWO_PI) * 0.25);
                } else {
                    return 0.75 + (Mth.cos(time * Mth.TWO_PI) * 0.25);
                }
            });

    // Line (Saw Tooth)
    ILightmapNumberProvider LINEAR_2 = BUILTIN_PROVIDERS.register("linear_2",
            (time, rain, thunder) -> {
                float linear = Mth.abs(1 - 2 * time);
                if (time > 0.5) {
                    return linear * 0.5;
                } else {
                    return 1 - linear * 0.5;
                }
            });


}
