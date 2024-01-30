package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtils {

    public static Properties jsonToProperties(JsonElement element) {
        Properties properties = new Properties();
        if (element instanceof JsonObject jo) {
            iterateJsonObject(jo, properties, "");
        }
        return properties;

    }

    private static void iterateJsonObject(JsonObject jsonObject, Properties properties, String currentPath) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            String newPath = currentPath.isEmpty() ? key : currentPath + "." + key;

            if (value instanceof JsonObject jo) {
                iterateJsonObject(jo, properties, newPath);
            } else if (value instanceof JsonPrimitive s && s.isString()) {
                properties.setProperty(newPath, s.getAsString());
            }
        }
    }

    public static Map<ResourceLocation, Properties> gatherProperties(ResourceManager resourceManager, String path) {

        FileToIdConverter converter = new FileToIdConverter(path, ".properties");
        Map<ResourceLocation, Properties> map = new HashMap<>();
        var res = converter.listMatchingResources(resourceManager);
        for (var e : res.entrySet()) {
            try (Reader reader = e.getValue().openAsReader()) {
                Properties properties = new Properties();
                properties.load(reader);
                map.put(e.getKey(), properties);
            } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                Polytone.LOGGER.error("Couldn't parse property file {}:", e, ex);
            }
        }
        return map;
    }
}
