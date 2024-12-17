package net.mehvahdjukaar.polytone.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

//
public class BiomeIdMapperManager extends JsonPartialReloader {

    private final MapRegistry<BiomeIdMapper> biomeIdMappers = new MapRegistry<>("Biome ID Mappers");

    public BiomeIdMapperManager() {
        super("biome_id_mappers");
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        biomeIdMappers.clear();
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops,
                                  RegistryAccess access) {
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            var mapper = BiomeIdMapper.CODEC.decode(ops, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Biome ID mapper with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            try {
                biomeIdMappers.register(id, mapper);
            }catch (Exception e){
                Polytone.LOGGER.warn("Found duplicate biome in biome id mapper {}", id);
            }
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {

    }

    public Codec<BiomeIdMapper> byNameCodec() {
        return biomeIdMappers;
    }
}
