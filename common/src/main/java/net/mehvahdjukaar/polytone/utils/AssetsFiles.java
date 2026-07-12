package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

/**
 * A manager's folder(s), scanned and grouped by file type: {@code .json} → {@link #jsons()},
 * {@code .png} → {@link #textures()}. The uniform hand-off from {@code ContentManager#prepare} to
 * {@code parseWithLevel}.
 *
 * <p>A pure data bundle: both maps are handed out as read-only (unmodifiable) views.</p>
 */
public final class AssetsFiles {
    private final Map<ResourceLocation, JsonElement> jsons;
    private final Map<ResourceLocation, ArrayImage> textures;

    public AssetsFiles(Map<ResourceLocation, JsonElement> jsons, Map<ResourceLocation, ArrayImage> textures) {
        // zero-copy read-only views: guarantees the accessors below can't be mutated through
        this.jsons = Collections.unmodifiableMap(jsons);
        this.textures = Collections.unmodifiableMap(textures);
    }

    public Map<ResourceLocation, JsonElement> jsons() {
        return jsons;
    }

    public Map<ResourceLocation, ArrayImage> textures() {
        return textures;
    }
}
