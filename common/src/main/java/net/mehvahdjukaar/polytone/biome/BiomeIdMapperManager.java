package net.mehvahdjukaar.polytone.biome;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.Map;

//
public class BiomeIdMapperManager extends JsonPartialReloader {

    private static final BiMap<String, BiomeIdMapper> ID_MAPPERS = HashBiMap.create();


    public static final Codec<BiomeIdMapper> REFERENCE_CODEC = Codec.stringResolver(
            a -> ID_MAPPERS.inverse().get(a), ID_MAPPERS::get);

    public static final Codec<BiomeIdMapper> CODEC = new ReferenceOrDirectCodec<>(
            REFERENCE_CODEC, BiomeIdMapper.Custom.CODEC, false);


    public BiomeIdMapperManager() {
        super("biome_id_mappers");
    }

    @Override
    protected void reset() {
        ID_MAPPERS.clear();
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> obj) {
        for (var j : obj.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            var mapper = CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Biome ID mapper with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            ID_MAPPERS.put(id.toString(), mapper);
        }
    }

}
