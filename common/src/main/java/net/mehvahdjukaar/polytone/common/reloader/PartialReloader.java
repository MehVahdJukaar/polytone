package net.mehvahdjukaar.polytone.common.reloader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.struc.ListUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
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

    protected Map<Identifier, JsonElement> getJsonsInDirectories(ResourceManager resourceManager) {
        // resources given by the resource manager won't be sorted by pack ordering so we at least sort them by name
        Map<Identifier, JsonElement> jsons = ListUtils.sortedMap();
        for (String name : names) {
            Map<Identifier, JsonElement> js = new HashMap<>();
            scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, GSON, js);
            greedyAddAll(js, jsons);
        }
        //sort by key
        return jsons;
    }

    public static void scanDirectory(ResourceManager resourceManager, String string, Gson gson, Map<Identifier, JsonElement> map) {
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(string);

        for (Map.Entry<Identifier, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            Identifier resourceLocation = entry.getKey();
            Identifier resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);

            try (Reader reader = entry.getValue().openAsReader()) {

                JsonElement jsonElement = GsonHelper.fromJson(gson, reader, JsonElement.class);
                JsonElement jsonElement2 = map.put(resourceLocation2, jsonElement);
                if (jsonElement2 != null) {
                    Polytone.maybeThrow(
                            new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2)
                    );
                }

            } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                Polytone.maybeThrow(
                        new IllegalStateException("Couldn't parse data file " + resourceLocation2 + " from " + resourceLocation, var14)
                );
                // Polytone.LOGGER.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, var14);
            }
        }
    }

    private static <T> void greedyAddAll(Map<Identifier, T> js, Map<Identifier, T> jsons) {
        for (var entry : js.entrySet()) {
            var r = entry.getKey();
            var j = entry.getValue();
            jsons.put(r, j);
        }
    }

    protected Map<Identifier, ArrayImage> getImagesInDirectories(ResourceManager resourceManager) {
        Map<Identifier, ArrayImage> images = new HashMap<>();
        for (String name : names) {
            Map<Identifier, ArrayImage> im = new HashMap<>();
            ArrayImage.scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, im);
            greedyAddAll(im, images);
        }
        return images;
    }

    protected Map<Identifier, ArrayImage.Group> getGroupedImagesInDirectories(ResourceManager manager) {
        return ArrayImage.groupTextures(this.getImagesInDirectories(manager));
    }

    protected void earlyProcess(PreparableReloadListener.SharedState sharedState) {
    }

    protected abstract T prepare(PreparableReloadListener.SharedState sharedState);

    protected void parseWithLevel(T obj, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
    }

    ;

    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
    }

    ;

    protected void resetWithLevel(boolean logOff) {
    }

    ;

    protected void applyNormal(T obj) {
    }


}
