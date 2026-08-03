package net.mehvahdjukaar.polytone.content.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

import java.util.Map;

//
public class BiomeIdMapperManager extends ContentManager<BiomeIdMapper> {

    private final MapRegistry<BiomeIdMapper> biomeIdMappers = new MapRegistry<>("Biome ID Mappers");

    public BiomeIdMapperManager() {
        super(Spec.of("Biome id mapper", () -> SchemaCodec.wrap(BiomeIdMapper.CODEC))
                .wikiPage("Colormaps")
                .folders("biome_id_mappers"));
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        biomeIdMappers.clear();
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            BiomeIdMapper mapper = decodeStrict(json, id, ops);
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
