package net.mehvahdjukaar.polytone.content.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ContentManager;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

//
public class BiomeIdMapperManager extends ContentManager<BiomeIdMapper, Map<ResourceLocation, JsonElement>> {

    private final MapRegistry<BiomeIdMapper> biomeIdMappers = new MapRegistry<>("Biome ID Mappers");

    public BiomeIdMapperManager() {
        super(Spec.of("Biome id mapper", () -> BiomeIdMapper.CODEC)
                .wikiPage("Colormaps")
                .folders("biome_id_mappers"));
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        return this.getJsonsInDirectories(resourceManager);
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
