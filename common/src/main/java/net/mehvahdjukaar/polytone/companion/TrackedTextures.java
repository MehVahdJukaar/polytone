package net.mehvahdjukaar.polytone.companion;

import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class TrackedTextures {

    private final Map<ResourceLocation, ArrayImage> textures;
    private final Set<ResourceLocation> used = new HashSet<>();

    public TrackedTextures(Map<ResourceLocation, ArrayImage> initial) {
        this.textures = new HashMap<>(initial);
    }

    public @Nullable ArrayImage get(ResourceLocation id) {
        return textures.get(id);
    }

    public boolean containsKey(ResourceLocation id) {
        return textures.containsKey(id);
    }

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

    public Map<ResourceLocation, ArrayImage> unused() {
        Map<ResourceLocation, ArrayImage> out = new LinkedHashMap<>();
        for (var e : textures.entrySet()) {
            if (!used.contains(e.getKey())) out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}
