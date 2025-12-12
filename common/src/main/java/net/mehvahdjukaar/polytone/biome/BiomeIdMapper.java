package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.BiomeKeysCache;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;

public interface BiomeIdMapper {


    Codec<BiomeIdMapper> CODEC = new ReferenceOrDirectCodec<>(
            Polytone.BIOME_ID_MAPPERS.byNameCodec(), Custom.CUSTOM_CODEC, false);

    BiomeIdMapper BY_INDEX = (biome) -> {
        int id = LegacyHelper.getBiomeId(biome);
        //don't ask questions here, I changed it too many times. This works
        return (id + 1) / 256f;
    };

    float getIndex(Biome biome);

    record Custom(Map<ResourceKey<Biome>, Float> map, float textureSize) implements BiomeIdMapper {

        public Custom(Map<ResourceKey<Biome>, Float> map) {
            this(map, map.getOrDefault(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("texture_size")), 1f));
        }

        public static final Codec<Custom> CUSTOM_CODEC = Codec.unboundedMap(Identifier.CODEC
                                .xmap(r -> ResourceKey.create(Registries.BIOME, r), ResourceKey::identifier),
                        Codec.FLOAT)
                .xmap(Custom::new, Custom::map);

        @Override
        public float getIndex(Biome biome) {
            return (map.getOrDefault(BiomeKeysCache.get(biome), 0f)) / (textureSize-1);
        }
    }


}
