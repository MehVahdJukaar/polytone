package net.mehvahdjukaar.polytone.common.struc;

import net.minecraft.resources.Identifier;
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
 * <p>Built fresh from {@link AssetsFiles#textures()} by whichever manager needs tracking, since the
 * bundle is re-parsed on every world join and consumption state must never leak back into the scan.
 */
public final class TrackedTextures {

    private final Map<Identifier, ArrayImage> textures;
    private final Set<Identifier> used = new HashSet<>();

    public TrackedTextures(Map<Identifier, ArrayImage> initial) {
        this.textures = new HashMap<>(initial);
    }

    /** Look up a texture without marking it used. */
    public @Nullable ArrayImage get(Identifier id) {
        return textures.get(id);
    }

    public boolean containsKey(Identifier id) {
        return textures.containsKey(id);
    }

    /** Add extra textures the folder scan didn't find (legacy/converted sources). */
    public void putAll(Map<Identifier, ArrayImage> extra) {
        textures.putAll(extra);
    }

    public void markUsed(Identifier id) {
        used.add(id);
    }

    public boolean isUsed(Identifier id) {
        return used.contains(id);
    }

    public Set<Identifier> keySet() {
        return textures.keySet();
    }

    /**
     * Textures not yet {@link #markUsed marked} — the orphan sweep. A snapshot, safe to iterate
     * while marking more used.
     */
    public Map<Identifier, ArrayImage> unused() {
        Map<Identifier, ArrayImage> out = new LinkedHashMap<>();
        for (var e : textures.entrySet()) {
            if (!used.contains(e.getKey())) out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}
