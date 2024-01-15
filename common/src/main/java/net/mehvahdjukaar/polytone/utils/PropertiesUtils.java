package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
            } else {
                properties.setProperty(newPath, value.toString());
            }
        }
    }
}
