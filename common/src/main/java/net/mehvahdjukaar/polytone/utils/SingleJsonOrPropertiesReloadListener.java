package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class SingleJsonOrPropertiesReloadListener extends SimplePreparableReloadListener<List<Properties>> {
    private static final Gson GSON = new Gson();
    private final String[] locations;
    private final String propertiesName;
    private final String jsonName;

    // Instead of getting all files in a folder, it gets all files at certain locations
    protected SingleJsonOrPropertiesReloadListener(String propertiesName, String jsonName, String... possibleLocations) {
        this.locations = possibleLocations;
        this.propertiesName = propertiesName;
        this.jsonName = jsonName;
    }

    @Override
    protected List<Properties> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Properties> list = new ArrayList<>();
        for (String paths : locations) {
            var res = resourceManager.listResources(paths,
                    resourceLocation -> resourceLocation.endsWith(propertiesName));
            for (var r : res) {
                try {
                    List<Resource> resources = resourceManager.getResources(r);
                    for(var l : resources) {
                        try (Reader reader = new InputStreamReader(l.getInputStream())) {
                            Properties properties = new Properties();
                            properties.load(reader);
                            list.add(properties);
                        } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                            Polytone.LOGGER.error("Couldn't parse data file {}:", r, ex);
                        }
                    }
                } catch (IOException e) {
                    Polytone.LOGGER.error(e);
                   // throw new RuntimeException(e);
                }

            }

            res = resourceManager.listResources(paths,
                    resourceLocation -> resourceLocation.endsWith(jsonName));
            for (var l : res) {
                try {
                    List<Resource> resources = resourceManager.getResources(l);
                    for (var r : resources) {
                        try (Reader reader = new InputStreamReader(r.getInputStream())) {
                            JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                            Properties prop = PropertiesUtils.jsonToProperties(jsonElement);
                            list.add(prop);
                        } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                            Polytone.LOGGER.error("Couldn't parse data file {}:", l, ex);
                        }
                    }
                }catch (Exception e){
                    Polytone.LOGGER.error(e);
                }
            }
        }
        return list;
    }
}

