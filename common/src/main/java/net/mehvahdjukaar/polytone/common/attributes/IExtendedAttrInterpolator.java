package net.mehvahdjukaar.polytone.common.attributes;

import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.biome.Biome;

public interface IExtendedAttrInterpolator {

    SpatialAttributeInterpolator polytone$getOrCreatePostInterpolator();

    void polytone$accumulateBiomeWeight(double weight, Holder<Biome> biome);

    Reference2DoubleMap<Holder<Biome>> polytone$getBiomeWeights();
}
