package net.mehvahdjukaar.polytone.moonlight_configs.fabric.values;

import com.google.gson.JsonObject;
import net.mehvahdjukaar.polytone.Polytone;

import java.util.function.Predicate;

public class StringConfigValue extends ConfigValue<String> {

    private final Predicate<Object> validator;

    public StringConfigValue(String name, String defaultValue, Predicate<Object> validator) {
        super(name, defaultValue);
        this.validator = validator;
    }

    @Override
    public boolean isValid(String value) {
        return validator.test(value);
    }

    @Override
    public void loadFromJson(JsonObject element) {
        if (element.has(this.name)) {
            try {
                this.value = element.get(this.name).getAsString();
                if (this.isValid(value)) return;
                //if not valid it defaults
                this.value = defaultValue;
            } catch (Exception ignored) {
            }
            Polytone.LOGGER.warn("Config file had incorrect entry {}, correcting", this.name);
        } else {
            Polytone.LOGGER.warn("Config file had missing entry {}", this.name);
        }
    }

    @Override
    public void saveToJson(JsonObject object) {
        if (this.value == null) this.value = defaultValue;
        object.addProperty(this.name, this.value);
    }


}
