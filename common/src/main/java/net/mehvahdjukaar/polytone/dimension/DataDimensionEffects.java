package net.mehvahdjukaar.polytone.dimension;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DataDimensionEffects extends DimensionSpecialEffects {
    public DataDimensionEffects() {
        super(Float.NaN, false, DimensionSpecialEffects.SkyType.END, true, false);
    }

    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.scale(0.15000000596046448);
    }

    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Nullable
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }
}