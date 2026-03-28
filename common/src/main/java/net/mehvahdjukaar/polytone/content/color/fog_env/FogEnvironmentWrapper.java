package net.mehvahdjukaar.polytone.content.color.fog_env;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;

public class FogEnvironmentWrapper extends FogEnvironment {

    private final FogEnvironment wrapped;
    private final FogEnvironmentMod fogMod;

    public FogEnvironmentWrapper(FogEnvironment original, FogEnvironmentMod mod) {
        this.wrapped = original;
        this.fogMod = mod;
    }

    public FogEnvironment getWrapped() {
        return wrapped;
    }

    @Override
    public void setupFog(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker) {
        wrapped.setupFog(fogData, camera, clientLevel, f, deltaTracker);
        fogMod.overrideSetupFog(fogData, camera, clientLevel, f, deltaTracker);
    }

    @Override
    public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
        return fogMod.isEnabled() && wrapped.isApplicable(fogType, entity);
    }
}
