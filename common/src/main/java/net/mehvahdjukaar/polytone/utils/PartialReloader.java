package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public abstract class PartialReloader<T> {

    public static final Gson GSON = new Gson();

    protected String[] names;

    protected PartialReloader(String... name) {
        this.names = name;
    }

    @Override
    public String toString() {
        return StringUtils.capitalize(names[0].replace("_", " ") + " Reloader");
    }

    protected Map<ResourceLocation, JsonElement> getJsonsInDirectories(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        for (String name : names) {
            Map<ResourceLocation, JsonElement> js = new HashMap<>();
            scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, GSON, js);
            greedyAddAll(js, jsons);
        }
        return jsons;
    }

    private static <T> void greedyAddAll(Map<ResourceLocation, T> js, Map<ResourceLocation, T> jsons) {
        for (var entry : js.entrySet()) {
            var r = entry.getKey();
            var j = entry.getValue();
            if (jsons.containsKey(r)) {
                Polytone.LOGGER.warn("Duplicate data file ignored with ID {}", r);
            }
            jsons.put(r, j);
        }
    }

    protected Map<ResourceLocation, ArrayImage> getImagesInDirectories(ResourceManager resourceManager) {
        Map<ResourceLocation, ArrayImage> images = new HashMap<>();
        for (String name : names) {
            Map<ResourceLocation, ArrayImage> im = new HashMap<>();
            ArrayImage.scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, im);
            greedyAddAll(im, images);
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
