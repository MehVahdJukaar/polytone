package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;

public abstract class JsonPartialReloader<O> extends ContentManager<O, Map<ResourceLocation, JsonElement>> {

    protected JsonPartialReloader(Spec<O> spec) {
        super(spec);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        return ImmutableMap.copyOf(jsons);
    }
}
