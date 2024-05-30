package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public abstract class PartialReloader<T> {

    public static final Gson GSON = new Gson();

    protected String[] names;

    protected PartialReloader(String... name) {
        this.names = name;
    }

    protected Map<ResourceLocation, JsonElement> getJsonsInDirectories(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        for (String name : names) {
            scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, GSON, jsons);
        }
        return jsons;
    }


    protected Map<ResourceLocation, ArrayImage> getImagesInDirectories(ResourceManager resourceManager) {
        Map<ResourceLocation, ArrayImage> images = new HashMap<>();
        for (String name : names) {
            ArrayImage.scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, images);
        }
        return images;
    }

    protected Map<ResourceLocation, ArrayImage.Group> getGroupedImagesInDirectories(ResourceManager manager) {
        return ArrayImage.groupTextures(this.getImagesInDirectories(manager));
    }

    protected abstract T prepare(ResourceManager resourceManager);

    protected abstract void reset();

    protected abstract void process(T obj);

    protected void apply() {
    }

    protected void checkConditions(Map<ResourceLocation, JsonElement> object) {
        object.entrySet().removeIf(e -> {
            if (e.getValue() instanceof JsonObject jo) {
                JsonElement je = jo.get("require_mods");
                if (je != null) {
                    if (je.isJsonArray()) {
                        for (JsonElement el : je.getAsJsonArray()) {
                            if (!PlatStuff.isModLoaded(el.getAsString())) {
                                return true;
                            }
                        }
                    } else if (je.isJsonPrimitive()) {
                        return !PlatStuff.isModLoaded(je.getAsString());
                    }
                }
            }
            return false;
        });

    }
}
