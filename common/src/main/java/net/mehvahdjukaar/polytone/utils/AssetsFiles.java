package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

public record AssetsFiles(Map<ResourceLocation, JsonElement> jsons, Map<ResourceLocation, ArrayImage> textures) {
    public AssetsFiles(Map<ResourceLocation, JsonElement> jsons, Map<ResourceLocation, ArrayImage> textures) {
        // zero-copy read-only views: guarantees the accessors below can't be mutated through
        this.jsons = Collections.unmodifiableMap(jsons);
        this.textures = Collections.unmodifiableMap(textures);
    }
}
