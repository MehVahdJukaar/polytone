package net.mehvahdjukaar.polytone.lightmap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public interface ILightmapNumberProvider {

    BiMap<String, ILightmapNumberProvider> CUSTOM_PROVIDERS = HashBiMap.create();

    Codec<ILightmapNumberProvider> REFERENCE_CODEC = Codec.stringResolver(
            a->CUSTOM_PROVIDERS.inverse().get(a), CUSTOM_PROVIDERS::get);

    Codec<ILightmapNumberProvider> CODEC = new ReferenceOrDirectCodec<>(REFERENCE_CODEC,
            LightmapExpressionProvider.CODEC, true);

    static <T extends ILightmapNumberProvider> T register(String name, T provider) {
        CUSTOM_PROVIDERS.put(name, provider);
        return provider;
    }


    float getValue(float time, float rain, float thunder);

    RandomSource RAND = RandomSource.create();

    // Sine
    ILightmapNumberProvider RANDOM = register("random",
            (time, rain, thunder) -> {
                RAND.setSeed(Float.floatToIntBits(time));
                return RAND.nextFloat();
            });

    // Same stuff that level.getSkyDarkens does
    // A slanted Step
    ILightmapNumberProvider DEFAULT = register("default",
            (time, rain, thunder) -> {
                float g = 1.0F - (Mth.cos(time * Mth.TWO_PI) * 2.0F + 0.2F);
                g = Mth.clamp(g, 0.0F, 1.0F);
                g = 1.0F - g;
                // g *= 1.0F - rain * 5.0F / 16.0F;
                // g *= 1.0F - thunder * 5.0F / 16.0F;
                return g;
            });

    // Sine
    ILightmapNumberProvider SMOOTH = register("smooth",
            (time, rain, thunder) -> 0.5f + (Mth.cos(time * Mth.TWO_PI) * 0.5f));

    // Triangle func
    ILightmapNumberProvider LINEAR = register("linear",
            (time, rain, thunder) -> Mth.abs(1 - 2 * time));

    ILightmapNumberProvider DEFAULT_2 = register("default",
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
    ILightmapNumberProvider SMOOTH_2 = register("smooth_2",
            (time, rain, thunder) -> {
                if (time > 0.5) {
                    return 0.25f - (Mth.cos(time * Mth.TWO_PI) * 0.25f);
                } else {
                    return 0.75f + (Mth.cos(time * Mth.TWO_PI) * 0.25f);
                }
            });

    // Line (Saw Tooth)
    ILightmapNumberProvider LINEAR_2 = register("linear_2",
            (time, rain, thunder) -> {
                float linear = Mth.abs(1 - 2 * time);
                if (time > 0.5) {
                    return linear * 0.5f;
                } else {
                    return 1 - linear * 0.5f;
                }
            });



}
