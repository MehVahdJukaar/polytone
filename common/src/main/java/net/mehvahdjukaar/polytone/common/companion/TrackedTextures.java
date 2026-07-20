package net.mehvahdjukaar.polytone.common.companion;

import net.mehvahdjukaar.polytone.common.PathsUtils;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** The scanned texture store of one reload pass, tracking which files got consumed by a colormap. */
public final class TrackedTextures {

    private final Map<Identifier, ArrayImage> textures;
    private final Set<Identifier> used = new HashSet<>();

    public TrackedTextures(Map<Identifier, ArrayImage> initial) {
        this.textures = new HashMap<>(initial);
    }

    public void putAll(Map<Identifier, ArrayImage> extra) {
        textures.putAll(extra);
    }

    public boolean isUsed(Identifier id) {
        return used.contains(id);
    }

    public Set<Identifier> keySet() {
        return textures.keySet();
    }

    public @Nullable Identifier find(Identifier baseId, String fileName) {
        Identifier candidate = baseId.withPath(
                PathsUtils.directoryOf(baseId.getPath()) + PathsUtils.stripExtension(fileName));
        return textures.containsKey(candidate) ? candidate : null;
    }

    public void fillColormap(Identifier textureId, Colormap colormap) {
        ArrayImage texture = textures.get(textureId);
        if (texture == null || texture.pixels().length == 0) {
            throw new IllegalStateException("Colormap texture at location " + textureId + " had invalid 0 dimension");
        }
        colormap.acceptTexture(texture);
        used.add(textureId);
        colormap.debugID = textureId;
    }
}
