package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

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
        // resources given by the resource manager won't be sorted by pack ordering so we at least sort them by name
        Map<ResourceLocation, JsonElement> jsons = Utils.sortedMap();
        for (String name : names) {
            Map<ResourceLocation, JsonElement> js = new HashMap<>();
            scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, GSON, js);
            greedyAddAll(js, jsons);
        }
        //sort by key
        return jsons;
    }

    private static <T> void greedyAddAll(Map<ResourceLocation, T> js, Map<ResourceLocation, T> jsons) {
        for (var entry : js.entrySet()) {
            var r = entry.getKey();
            var j = entry.getValue();
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

    protected void earlyProcess(ResourceManager resourceManager) {

    }

    protected abstract T prepare(ResourceManager resourceManager);

    protected abstract void parseWithLevel(T obj, RegistryOps<JsonElement> ops, RegistryAccess access);

    protected abstract void applyWithLevel(RegistryAccess access, boolean isLogIn);

    protected abstract void resetWithLevel(boolean logOff);

}
