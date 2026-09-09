package net.mehvahdjukaar.polytone.common.struc;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Map;

public record AssetsFiles(Map<Identifier, JsonElement> jsons, Map<Identifier, ArrayImage> textures) {
    public AssetsFiles(Map<Identifier, JsonElement> jsons, Map<Identifier, ArrayImage> textures) {
        this.jsons = Collections.unmodifiableMap(jsons);
        this.textures = Collections.unmodifiableMap(textures);
    }
}
