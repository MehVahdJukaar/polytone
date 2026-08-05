package net.mehvahdjukaar.polytone.mixins;

import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedInterpolator;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpatialAttributeInterpolator.class)
public class SpatialAttributeInterpolatorMixin implements IExtendedInterpolator {

    @Unique
    private SpatialAttributeInterpolator poly$postInterpolator = null;

    @Unique
    private final Reference2DoubleArrayMap<Holder<Biome>> poly$biomeWeights = new Reference2DoubleArrayMap<>();


    @Override
    public SpatialAttributeInterpolator polytone$getOrCreatePostInterpolator() {
        if (poly$postInterpolator == null) {
            poly$postInterpolator = new SpatialAttributeInterpolator();
        }
        return poly$postInterpolator;
    }

    @Override
    public void polytone$accumulateBiome(double weight, Holder<Biome> biome) {
        poly$biomeWeights.mergeDouble(biome, weight, Double::sum);
    }

    @Override
    public Reference2DoubleMap<Holder<Biome>> polytone$getBiomeWeights() {
        return poly$biomeWeights;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void polytone$clearPostInterpolator(CallbackInfo ci) {
        if (poly$postInterpolator != null) {
            poly$postInterpolator.clear();
        }
        poly$biomeWeights.clear();
    }
}
