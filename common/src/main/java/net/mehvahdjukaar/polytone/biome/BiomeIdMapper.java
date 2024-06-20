package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.BiomeKeysCache;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;

public interface BiomeIdMapper {


    Codec<BiomeIdMapper> CODEC = new ReferenceOrDirectCodec<>(
            Polytone.BIOME_ID_MAPPERS.byNameCodec(), Custom.CUSTOM_CODEC, false);

    BiomeIdMapper BY_INDEX = (biome) -> {
        int id = LegacyHelper.getBiomeId(biome);
        return (1 + id) / 255f;
    };

    float getIndex(Biome biome);

    record Custom(Map<ResourceKey<Biome>, Float> map, float textureSize) implements BiomeIdMapper {

        public Custom(Map<ResourceKey<Biome>, Float> map) {
            this(map, map.getOrDefault(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("texture_size")), 1f));
        }

        public static final Codec<Custom> CUSTOM_CODEC = Codec.unboundedMap(ResourceLocation.CODEC
                                .xmap(r -> ResourceKey.create(Registries.BIOME, r), ResourceKey::location),
                        Codec.FLOAT)
                .xmap(Custom::new, Custom::map);

        @Override
        public float getIndex(Biome biome) {
            // no clue why 1 is needed
            return (1 + map.getOrDefault(BiomeKeysCache.get(biome), 0f)) / (textureSize - 1);
        }
    }


}
