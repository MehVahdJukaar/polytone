package net.mehvahdjukaar.polytone.common.attributes;

import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.biome.Biome;

public interface IExtendedInterpolator {

    SpatialAttributeInterpolator polytone$getOrCreatePostInterpolator();

    // vanilla only keeps weights per attribute map, which we can't map back to a biome (many biomes share
    // EnvironmentAttributeMap.EMPTY). so we record the same kernel a second time, keyed by biome
    void polytone$accumulateBiome(double weight, Holder<Biome> biome);

    Reference2DoubleMap<Holder<Biome>> polytone$getBiomeWeights();
}
