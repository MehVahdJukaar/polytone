package net.mehvahdjukaar.polytone.common.reloader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.PropertiesUtils;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class SingleFileContentManager<O> extends ContentManager<O> {
    private static final Gson GSON = new Gson();
    private final String propertiesFileName;
    private final String jsonFileName;

    // Instead of getting all files in a folder, it gets all files at certain locations
    protected SingleFileContentManager(String myName,
                                                   String propertiesName, String jsonName,
                                                   String... possibleFolderLocations) {
        super(myName, possibleFolderLocations);
        this.propertiesFileName = propertiesName;
        this.jsonFileName = jsonName;
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        Map<Identifier, JsonElement> jsonObjects = new HashMap<>();
        for (String path : folderNames()) {


            // .properties files
            var propertiesResources = resourceManager.listResourceStacks(path, id -> id.getPath().endsWith(propertiesFileName));

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
                  id -> id.getPath().endsWith(jsonFileName));

            for (var entrySet : resources.entrySet()) {
                var resourceStack = entrySet.getValue();
                Identifier id = entrySet.getKey();
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
        return new AssetsFiles(jsonObjects, Map.of());
    }
}

