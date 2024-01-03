package net.mehvahdjukaar.visualprop.moonlight_configs.fabric.values;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.visualprop.VisualProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ListStringConfigValue<T extends String> extends ConfigValue<List<String>> {

    private final Predicate<Object> predicate;

    public ListStringConfigValue(String name, List<String> defaultValue, Predicate<Object> validator) {
        super(name, defaultValue);
        this.predicate = validator;
    }

    @Override
    public boolean isValid(List<String> value) {
        return true;
    }

    @Override
    public void loadFromJson(JsonObject element) {
        if (element.has(this.name)) {
            try {
                var array = element.get(this.name);
                if (array instanceof JsonArray ja) {
                    this.value = new ArrayList<>();
                    for (var v : ja) {
                        T s = (T) v.getAsString();
                        if (this.predicate.test(s)) this.value.add(s);
                    }
                }
                if (this.isValid(value)) return;
                //if not valid it defaults
                this.value = defaultValue;
            } catch (Exception ignored) {
            }
            VisualProperties.LOGGER.warn("Config file had incorrect entry {}, correcting", this.name);
        } else {
            VisualProperties.LOGGER.warn("Config file had missing entry {}", this.name);
        }
    }

    @Override
    public void saveToJson(JsonObject object) {
        if (this.value == null) this.value = defaultValue;
        JsonArray ja = new JsonArray();
        this.value.forEach(ja::add);
        object.add(this.name, ja);
    }


}
