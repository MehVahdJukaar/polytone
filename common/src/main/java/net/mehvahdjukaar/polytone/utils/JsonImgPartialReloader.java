package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.Map;

public abstract class JsonImgPartialReloader extends PartialReloader<JsonImgPartialReloader.Resources> {

    protected JsonImgPartialReloader(String name) {
        super(name);
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);
        var textures = ArrayImage.gatherImages(resourceManager, path());

        return new Resources(jsons, textures);
    }

    public record Resources(Map<ResourceLocation, JsonElement> jsons,
                            Map<ResourceLocation, ArrayImage> textures) {
    }
}
