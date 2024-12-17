package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;

public abstract class JsonPartialReloader extends PartialReloader<Map<ResourceLocation, JsonElement>> {

    protected JsonPartialReloader(String ...name) {
        super(name);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        this.checkConditions(jsons);
        return ImmutableMap.copyOf(jsons);
    }
}
