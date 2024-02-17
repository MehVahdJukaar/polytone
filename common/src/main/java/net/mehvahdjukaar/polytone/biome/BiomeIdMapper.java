package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;

public interface BiomeIdMapper {

    float getIndex(Registry<Biome> biomeRegistry, Holder<Biome> biome);

    record Custom(Map<ResourceLocation, Float> map) implements BiomeIdMapper{

        public static final Codec<Custom> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT)
                .xmap(Custom::new, Custom::map);

        @Override
        public float getIndex(Registry<Biome> biomeRegistry, Holder<Biome> biome) {
            return map.getOrDefault(biome.unwrapKey().get().location(),0f);
        }
    }
}
