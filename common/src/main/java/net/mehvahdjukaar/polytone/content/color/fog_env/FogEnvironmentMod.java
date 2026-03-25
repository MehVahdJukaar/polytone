package net.mehvahdjukaar.polytone.content.color.fog_env;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.renderer.fog.FogRenderer;

public record FogEnvironmentMod(boolean enabled, Integer baseColor) {

    //TODO: finish
    public static final Codec<FogEnvironmentMod> CODEC =
            Codec.withAlternative(
                    Codec.BOOL.xmap(aBoolean -> new FogEnvironmentMod(aBoolean, null),
                            powderSnowEnvMod -> powderSnowEnvMod.enabled),
            RecordCodecBuilder.create(i -> i.group(
                    Codec.BOOL.fieldOf("enabled").forGetter(FogEnvironmentMod::enabled),
                    ColorUtils.COLOR.fieldOf("base_color").forGetter(FogEnvironmentMod::baseColor)
            ).apply(i, FogEnvironmentMod::new)));

}
