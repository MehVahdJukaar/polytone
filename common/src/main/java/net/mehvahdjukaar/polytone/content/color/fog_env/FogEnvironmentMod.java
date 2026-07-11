package net.mehvahdjukaar.polytone.content.color.fog_env;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaRecord;
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
            SchemaRecord.create(FogEnvironmentMod.class, i -> i.group(
                    i.field("enabled", Codec.BOOL, FogEnvironmentMod::isEnabled),
                    i.field("base_color", ColorUtils.COLOR, FogEnvironmentMod::baseColor)
            ).apply(i, FogEnvironmentMod::new)));




    public void overrideSetupFog(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker) {
    }
}
