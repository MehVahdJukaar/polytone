package net.mehvahdjukaar.polytone.companion;

import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.PathsUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** The scanned texture store of one reload pass, tracking which files got consumed by a colormap. */
public final class TrackedTextures {

    private final Map<ResourceLocation, ArrayImage> textures;
    private final Set<ResourceLocation> used = new HashSet<>();

    public TrackedTextures(Map<ResourceLocation, ArrayImage> initial) {
        this.textures = new HashMap<>(initial);
    }

    public void putAll(Map<ResourceLocation, ArrayImage> extra) {
        textures.putAll(extra);
    }

    public boolean isUsed(ResourceLocation id) {
        return used.contains(id);
    }

    public Set<ResourceLocation> keySet() {
        return textures.keySet();
    }

    public @Nullable ResourceLocation find(ResourceLocation baseId, String fileName) {
        ResourceLocation candidate = baseId.withPath(
                PathsUtils.directoryOf(baseId.getPath()) + PathsUtils.stripExtension(fileName));
        return textures.containsKey(candidate) ? candidate : null;
    }

    public void fillColormap(ResourceLocation textureId, Colormap colormap) {
        ArrayImage texture = textures.get(textureId);
        if (texture == null || texture.pixels().length == 0) {
            throw new IllegalStateException("Colormap texture at location " + textureId + " had invalid 0 dimension");
        }
        colormap.acceptTexture(texture);
        used.add(textureId);
        colormap.debugID = textureId;
    }
}
