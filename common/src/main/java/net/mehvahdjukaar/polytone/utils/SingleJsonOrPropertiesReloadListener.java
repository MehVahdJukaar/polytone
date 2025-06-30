package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class SingleJsonOrPropertiesReloadListener extends PartialReloader<Map<ResourceLocation, Properties>> {
    private static final Gson GSON = new Gson();
    private final String[] locations;
    private final String propertiesName;
    private final String jsonName;

    // Instead of getting all files in a folder, it gets all files at certain locations
    protected SingleJsonOrPropertiesReloadListener(String propertiesName, String jsonName, String... possibleLocations) {
        super("color_manager");
        this.locations = possibleLocations;
        this.propertiesName = propertiesName;
        this.jsonName = jsonName;
    }

    @Override
    protected Map<ResourceLocation, Properties> prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, Properties> list = new HashMap<>();
        for (String paths : locations) {

            //properties
            var resources = resourceManager.listResourceStacks(paths, id -> id.getPath().endsWith(propertiesName));

            for (var entrySet : resources.entrySet()) {
                var resourceStack = entrySet.getValue();
                ResourceLocation id = entrySet.getKey();
                for (var resource : resourceStack) {
                    try (Reader reader = resource.openAsReader()) {
                        Properties properties = new Properties();
                        properties.load(reader);
                        //merge all. only for .properties... optifine
                        list.merge(id, properties, (properties1, properties2) -> {
                            properties1.putAll(properties2);
                            return properties1;
                        });
                    } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                        Polytone.LOGGER.error("Couldn't parse data file {}:", resourceStack, ex);
                    }
                }
            }

            //json
            resources = resourceManager.listResourceStacks(paths, id -> id.getPath().endsWith(jsonName));

            for (var entrySet : resources.entrySet()) {
                var resourceStack = entrySet.getValue();
                ResourceLocation id = entrySet.getKey();
                //dont merge. too bad. jsons should have unique names here
                for (var resource : resourceStack) {
                    try (Reader reader = resource.openAsReader()) {
                        JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                        Properties prop = PropertiesUtils.jsonToProperties(jsonElement);
                        if (list.containsKey(id)) {
                            Polytone.LOGGER.warn("Found duplicate color.json with path {}. Old one will be overwritten. Be sure to put this file in your own namespace, not minecraft one!", id);
                        }
                        list.put(id, prop);
                    } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                        Polytone.LOGGER.error("Couldn't parse data file {}:", resource, ex);
                    }
                }
            }
        }
        return ImmutableMap.copyOf(list);
    }
}

