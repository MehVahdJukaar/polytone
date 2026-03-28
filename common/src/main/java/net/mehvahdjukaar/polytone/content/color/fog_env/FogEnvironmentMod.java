package net.mehvahdjukaar.polytone.content.color.fog_env;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;

public record FogEnvironmentMod(boolean isEnabled, Integer baseColor) {

    //TODO: finish
    public static final Codec<FogEnvironmentMod> CODEC =
            Codec.withAlternative(
                    Codec.BOOL.xmap(aBoolean -> new FogEnvironmentMod(aBoolean, null),
                            powderSnowEnvMod -> powderSnowEnvMod.isEnabled),
            RecordCodecBuilder.create(i -> i.group(
                    Codec.BOOL.fieldOf("enabled").forGetter(FogEnvironmentMod::isEnabled),
                    ColorUtils.COLOR.fieldOf("base_color").forGetter(FogEnvironmentMod::baseColor)
            ).apply(i, FogEnvironmentMod::new)));




    public void overrideSetupFog(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker) {
    }
}
