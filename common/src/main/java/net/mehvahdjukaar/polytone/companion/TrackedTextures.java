package net.mehvahdjukaar.polytone.companion;

import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A working copy of a scanned texture set that records which textures got claimed. The colormap
 * associate/stray flow looks textures up by id (without marking), {@link #markUsed marks} the ones
 * it binds, and finally sweeps {@link #unused()} for the "leftover texture becomes default content"
 * pass — the map + which-are-used pairing that each assigning manager used to hand-roll.
 *
 * <p>Built fresh from {@code AssetsFiles#textures()} by whichever manager needs tracking, since the
 * bundle is re-parsed on every world join and consumption state must never leak back into the scan.
 */
public final class TrackedTextures {

    private final Map<ResourceLocation, ArrayImage> textures;
    private final Set<ResourceLocation> used = new HashSet<>();

    public TrackedTextures(Map<ResourceLocation, ArrayImage> initial) {
        this.textures = new HashMap<>(initial);
    }

    /** Look up a texture without marking it used. */
    public @Nullable ArrayImage get(ResourceLocation id) {
        return textures.get(id);
    }

    public boolean containsKey(ResourceLocation id) {
        return textures.containsKey(id);
    }

    /** Add extra textures the folder scan didn't find (legacy/converted sources). */
    public void putAll(Map<ResourceLocation, ArrayImage> extra) {
        textures.putAll(extra);
    }

    public void markUsed(ResourceLocation id) {
        used.add(id);
    }

    public boolean isUsed(ResourceLocation id) {
        return used.contains(id);
    }

    public Set<ResourceLocation> keySet() {
        return textures.keySet();
    }

    /**
     * Textures not yet {@link #markUsed marked} — the orphan sweep. A snapshot, safe to iterate
     * while marking more used.
     */
    public Map<ResourceLocation, ArrayImage> unused() {
        Map<ResourceLocation, ArrayImage> out = new LinkedHashMap<>();
        for (var e : textures.entrySet()) {
            if (!used.contains(e.getKey())) out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}
