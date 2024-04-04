package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

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

    public static JsonObject propertiesToJson(Properties properties) {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            String[] keys = key.split("\\.");

            JsonObject currentObject = jsonObject;
            for (int i = 0; i < keys.length - 1; i++) {
                String currentKey = keys[i];
                if (!currentObject.has(currentKey)) {
                    JsonObject newObject = new JsonObject();
                    currentObject.add(currentKey, newObject);
                    currentObject = newObject;
                } else {
                    JsonElement element = currentObject.get(currentKey);
                    if (!element.isJsonObject()) {
                        // Key already exists with a non-object value, we can't overwrite it.
                        throw new IllegalArgumentException("Invalid properties file: Duplicate key found: " + currentKey);
                    }
                    currentObject = element.getAsJsonObject();
                }
            }

            String finalKey = keys[keys.length - 1];
            if (currentObject.has(finalKey)) {
                throw new IllegalArgumentException("Invalid properties file: Duplicate key found: " + finalKey);
            }

            if (value.contains(" ")) {
                String[] values = value.split(" ");
                jsonObject.getAsJsonObject(keys[0]).addProperty(finalKey, values[0]);
                for (int i = 1; i < values.length; i++) {
                    jsonObject.getAsJsonObject(keys[0]).getAsJsonArray(finalKey).add(values[i]);
                }
            } else {
                jsonObject.getAsJsonObject(keys[0]).addProperty(finalKey, value);
            }
        }
        return jsonObject;
    }

    public static Map<ResourceLocation, Properties> gatherProperties(ResourceManager resourceManager, String path) {

        FileToIdConverter converter = new FileToIdConverter(path, ".properties");
        Map<ResourceLocation, Properties> map = new HashMap<>();
        var res = converter.listMatchingResources(resourceManager);
        for (var e : res.entrySet()) {
            try (Reader reader = e.getValue().openAsReader()) {
                Properties properties = new Properties();
                properties.load(reader);
                ResourceLocation fileId = converter.fileToId(e.getKey());

                map.put(fileId, properties);
            } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                Polytone.LOGGER.error("Couldn't parse property file {}:", e, ex);
            }
        }
        return map;
    }
}
