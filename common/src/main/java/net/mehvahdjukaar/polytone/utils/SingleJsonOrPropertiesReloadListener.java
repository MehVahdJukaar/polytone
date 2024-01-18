package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class SingleJsonOrPropertiesReloadListener extends PartialReloader<List<Properties>> {
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
    protected List<Properties> prepare(ResourceManager resourceManager) {
        List<Properties> list = new ArrayList<>();
        for (String paths : locations) {
            var res = resourceManager.listResourceStacks(paths,
                    resourceLocation -> resourceLocation.getPath().endsWith(propertiesName)).values();
            for (var l : res) {
                for (var r : l) {
                    try (Reader reader = r.openAsReader()) {
                        Properties properties = new Properties();
                        properties.load(reader);
                        list.add(properties);
                    } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                        Polytone.LOGGER.error("Couldn't parse data file {}:", l, ex);
                    }
                }
            }

            res = resourceManager.listResourceStacks(paths,
                    resourceLocation -> resourceLocation.getPath().endsWith(jsonName)).values();
            for (var l : res) {
                for (var r : l) {
                    try (Reader reader = r.openAsReader()) {
                        JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                        Properties prop = PropertiesUtils.jsonToProperties(jsonElement);
                        list.add(prop);
                    } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                        Polytone.LOGGER.error("Couldn't parse data file {}:", l, ex);
                    }
                }
            }
        }
        return list;
    }
}

