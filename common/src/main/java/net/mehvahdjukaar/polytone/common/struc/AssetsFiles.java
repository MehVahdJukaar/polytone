package net.mehvahdjukaar.polytone.common.struc;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Map;

/**
 * A manager's folder(s), scanned and grouped by file type: {@code .json} → {@link #jsons()},
 * {@code .png} → {@link #textures()}. The uniform hand-off from {@code ContentManager#prepare} to
 * {@code parseWithLevel}/{@code applyNormal}.
 *
 * <p>A pure data bundle: both maps are handed out as read-only (unmodifiable) views. Steps that
 * merely look, iterate, or convert use them directly; the colormap associate/stray flow wraps the
 * textures in a fresh {@code TrackedTextures} itself when it needs consumption tracking.
 */
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
