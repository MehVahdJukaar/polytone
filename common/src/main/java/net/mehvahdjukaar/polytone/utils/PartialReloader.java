package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
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

    public static void scanDirectory(ResourceManager resourceManager, String string, Gson gson, Map<ResourceLocation, JsonElement> map) {
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(string);

        for (Map.Entry<ResourceLocation, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);

            try {
                Reader reader = entry.getValue().openAsReader();

                try {
                    JsonElement jsonElement = GsonHelper.fromJson(gson, reader, JsonElement.class);
                    JsonElement jsonElement2 = map.put(resourceLocation2, jsonElement);
                    if (jsonElement2 != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2);
                    }
                } catch (Throwable var13) {
                    try {
                        reader.close();
                    } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                    }

                    throw var13;
                }

                reader.close();
            } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                Polytone.LOGGER.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, var14);
            }
        }
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

    protected void earlyProcess(ResourceManager resourceManager){

    }

    protected abstract T prepare(ResourceManager resourceManager);

    protected abstract void parseWithLevel(T obj, RegistryOps<JsonElement> ops, RegistryAccess access);

    protected abstract void applyWithLevel(RegistryAccess access, boolean isLogIn);

    protected abstract void resetWithLevel(boolean logOff);

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
