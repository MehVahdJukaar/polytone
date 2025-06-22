package net.mehvahdjukaar.polytone.utils;

import com.google.gson.*;
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
            }else if(value instanceof JsonArray ja){
                StringBuilder builder = new StringBuilder();
                for (JsonElement e : ja) {
                    if (e instanceof JsonPrimitive p && p.isString()) {
                        builder.append(p.getAsString()).append(" ");
                    }
                }
                properties.setProperty(newPath, builder.toString().trim());
            }
        }
    }

    public static JsonObject propertiesToJson(Properties props) {
        JsonObject root = new JsonObject();

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            insertNested(root, key, value);
        }

        return root;
    }

    private static void insertNested(JsonObject root, String dottedKey, String value) {
        String[] parts = dottedKey.split("\\.");
        JsonObject current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            // Avoid overwriting existing non-object keys
            if (!current.has(part) || !(current.get(part) instanceof JsonObject)) {
                current.add(part, new JsonObject());
            }

            current = current.getAsJsonObject(part);
        }

        current.addProperty(parts[parts.length - 1], value);
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
