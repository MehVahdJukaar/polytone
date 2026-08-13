package net.mehvahdjukaar.polytone.common.struc;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Map;

// A manager's folder(s), scanned and grouped by file type: .json → jsons(), .png → textures(). The uniform
// hand-off from ContentManager#prepare to parseWithLevel/applyNormal. A pure data bundle: both maps are handed
// out as read-only (unmodifiable) views.
public final class AssetsFiles {
    private final Map<Identifier, JsonElement> jsons;
    private final Map<Identifier, ArrayImage> textures;

    public AssetsFiles(Map<Identifier, JsonElement> jsons, Map<Identifier, ArrayImage> textures) {
        // zero-copy read-only views: guarantees the accessors below can't be mutated through
        this.jsons = Collections.unmodifiableMap(jsons);
        this.textures = Collections.unmodifiableMap(textures);
    }

    public Map<Identifier, JsonElement> jsons() {
        return jsons;
    }

    public Map<Identifier, ArrayImage> textures() {
        return textures;
    }
}
