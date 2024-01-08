package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public abstract class SinglePropertiesReloadListener extends SimplePreparableReloadListener<List<Properties>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String[] locations;

    // Instead of getting all files in a folder, it gets all files at certain locations
    protected SinglePropertiesReloadListener(String... possibleLocations) {
        this.locations = Arrays.stream(possibleLocations).map(s -> s.endsWith(".properties") ? s : s + ".properties")
                .toArray(String[]::new);
    }

    @Override
    protected List<Properties> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Properties> list = new ArrayList<>();
        for (String paths : locations) {
            var res = resourceManager.listResourceStacks(paths,
                    resourceLocation -> resourceLocation.getPath().endsWith(paths)).values();
            for (var l : res) {
                for (var r : l) {
                    try (Reader reader = r.openAsReader()) {
                        Properties properties = new Properties();
                        properties.load(reader);
                        list.add(properties);
                    } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                        LOGGER.error("Couldn't parse data file {}:", l, ex);
                    }
                }
            }
        }
        return list;
    }
}

