package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.Map;

public abstract class JsonPartialReloader extends PartialReloader<Map<ResourceLocation, JsonElement>> {

    protected JsonPartialReloader(String name) {
        super(name);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);
        return jsons;
    }
}
