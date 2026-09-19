package net.mehvahdjukaar.polytone.common.reloader;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.PropertiesUtils;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class SingleFileContentManager<O> extends ContentManager<O> {

    private final String propertiesName;
    private final String jsonName;

    protected SingleFileContentManager(String myName,
                                                   String propertiesName, String jsonName,
                                                   String... possibleFolderLocations) {
        super(myName, possibleFolderLocations);
        this.propertiesName = propertiesName;
        this.jsonName = jsonName;
    }

    @Override
    protected AssetsFiles prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        for (String folder : folderNames()) {
            collect(resourceManager, folder, propertiesName, jsons, this::readProperties);
            collect(resourceManager, folder, jsonName, jsons, this::readJson);
        }
        return new AssetsFiles(jsons, Map.of());
    }

    private void collect(ResourceManager resourceManager, String folder, String suffix,
                         Map<ResourceLocation, JsonElement> out, ResourceParser parser) {
        var stacks = resourceManager.listResourceStacks(folder, id -> id.getPath().endsWith(suffix));
        for (var entry : stacks.entrySet()) {
            ResourceLocation id = entry.getKey();
            for (Resource resource : entry.getValue()) {
                try {
                    if (out.containsKey(id)) {
                        Polytone.LOGGER.warn("Found duplicate {}. The previous one will be overwritten. " +
                                "Be sure to put this file in your own namespace, not the minecraft one!", id);
                    }
                    out.put(id, parser.parse(resource));
                } catch (Exception ex) {
                    Polytone.LOGGER.error("Couldn't parse file {}:", resource, ex);
                }
            }
        }
    }

    private JsonElement readProperties(Resource resource) throws Exception {
        try (Reader reader = resource.openAsReader()) {
            Properties properties = new Properties();
            properties.load(reader);
            return PropertiesUtils.propertiesToJson(properties);
        }
    }

    private JsonElement readJson(Resource resource) throws Exception {
        try (Reader reader = resource.openAsReader()) {
            return GsonHelper.fromJson(GSON, reader, JsonElement.class);
        }
    }

    @FunctionalInterface
    private interface ResourceParser {
        JsonElement parse(Resource resource) throws Exception;
    }
}
