package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;

public interface BiomeIdMapper {

    BiomeIdMapper BY_INDEX = (biomeRegistry, biome) -> {
        int id = LegacyHelper.getBiomeId(biome, biomeRegistry);
        return id/255f;
    };

    float getIndex(Registry<Biome> biomeRegistry, Biome biome);

    record Custom(Map<ResourceLocation, Float> map, float textureSize) implements BiomeIdMapper {

        public Custom(Map<ResourceLocation, Float> map) {
            this(map, map.getOrDefault(new ResourceLocation("texture_size"), 1f));
        }

        public static final Codec<Custom> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT)
                .xmap(Custom::new, Custom::map);

        @Override
        public float getIndex(Registry<Biome> biomeRegistry, Biome biome) {
            return map.getOrDefault(biomeRegistry.getKey(biome), 0f) / textureSize;
        }
    }


}
