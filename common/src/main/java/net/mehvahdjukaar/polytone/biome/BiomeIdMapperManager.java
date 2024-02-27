package net.mehvahdjukaar.polytone.biome;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

//
public class BiomeIdMapperManager {

    private final BiMap<ResourceLocation, BiomeIdMapper> biomeMappers = HashBiMap.create();

    public static final Codec<BiomeIdMapper> CODEC = null;
    public static final Codec<BiomeIdMapper> CODEC2 = null;

    public static final BiomeIdMapper BY_INDEX = (biomeRegistry, biome) -> biomeRegistry.getId(biome.value());

}
