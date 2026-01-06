package net.mehvahdjukaar.polytone.content.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

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
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            var mapper = BiomeIdMapper.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Biome ID mapper with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            try {
                biomeIdMappers.register(id, mapper);
            } catch (Exception e) {
                Polytone.LOGGER.warn("Found duplicate biome in biome id mapper {}", id);
            }
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    public Codec<BiomeIdMapper> byNameCodec() {
        return biomeIdMappers;
    }

    @Nullable
    public BiomeIdMapper get(String biomeMapper) {
        return biomeIdMappers.getValue(biomeMapper);
    }
}
