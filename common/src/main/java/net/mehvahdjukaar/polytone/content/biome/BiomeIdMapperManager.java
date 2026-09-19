package net.mehvahdjukaar.polytone.content.biome;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

//
public class BiomeIdMapperManager extends ContentManager<BiomeIdMapper> {

    private final MapRegistry<BiomeIdMapper> biomeIdMappers = new MapRegistry<>("Biome ID Mappers");

    public BiomeIdMapperManager() {
        super(Spec.of("Biome id mapper", () -> BiomeIdMapper.CODEC)
                .wikiPage("Colormaps")
                .folders("biome_id_mappers"));
    }


    @Override
    protected void resetWithLevel(boolean logOff) {
        biomeIdMappers.clear();
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops,
                                  RegistryAccess access) {
        var jsons = resources.jsons();
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            var mapper = BiomeIdMapper.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Biome ID mapper with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
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

    public BiomeIdMapper get(String name) {
        return biomeIdMappers.getValue(name);
    }
}
