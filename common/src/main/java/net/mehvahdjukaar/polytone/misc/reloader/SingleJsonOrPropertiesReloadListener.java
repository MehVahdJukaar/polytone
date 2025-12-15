package net.mehvahdjukaar.polytone.misc.reloader;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.misc.struc.PropertiesUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class SingleJsonOrPropertiesReloadListener extends PartialReloader<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new Gson();
    private final String[] folders;
    private final String propertiesName;
    private final String jsonName;

    // Instead of getting all files in a folder, it gets all files at certain locations
    protected SingleJsonOrPropertiesReloadListener(String myName,
                                                   String propertiesName, String jsonName,
                                                   String... possibleFolderLocations) {
        super(myName);
        this.folders = possibleFolderLocations;
        this.propertiesName = propertiesName;
        this.jsonName = jsonName;
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        Map<ResourceLocation, JsonElement> jsonObjects = new HashMap<>();
        for (String path : folders) {


            // .properties files
            var propertiesResources = resourceManager.listResourceStacks(path, id -> id.getPath().endsWith(propertiesName));

            for (var entry : propertiesResources.entrySet()) {
                var resourceStack = entry.getValue();

                for (var resource : resourceStack) {
                    try (Reader reader = resource.openAsReader()) {
                        Properties properties = new Properties();
                        properties.load(reader);
                        JsonObject json = PropertiesUtils.propertiesToJson(properties);
                        jsonObjects.put(entry.getKey(), json);
                    } catch (IOException | IllegalArgumentException ex) {
                        Polytone.LOGGER.error("Couldn't parse .properties file {}:", resource, ex);
                    }
                }
            }

            //json
          var  resources = resourceManager.listResourceStacks(path,
                  id -> id.getPath().endsWith(jsonName));

            for (var entrySet : resources.entrySet()) {
                var resourceStack = entrySet.getValue();
                ResourceLocation id = entrySet.getKey();
                //dont merge. too bad. jsons should have unique names here
                for (var resource : resourceStack) {
                    try (Reader reader = resource.openAsReader()) {
                        JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                        if (jsonObjects.containsKey(id)) {
                            Polytone.LOGGER.warn("Found duplicate color.json with path {}. Old one will be overwritten. Be sure to put this file in your own namespace, not minecraft one!", id);
                        }
                        jsonObjects.put(id, jsonElement);
                    } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                        Polytone.LOGGER.error("Couldn't parse data file {}:", resource, ex);
                    }
                }
            }
        }
        return ImmutableMap.copyOf(jsonObjects);
    }
}

