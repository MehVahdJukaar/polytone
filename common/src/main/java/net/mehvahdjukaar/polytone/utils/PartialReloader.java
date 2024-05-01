package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;

public abstract class PartialReloader<T> {

    public static final Gson GSON = new Gson();

    protected String name;

    protected PartialReloader(String name) {
        this.name = name;
    }

    public String path() {
        return Polytone.MOD_ID + "/" + name;
    }

    protected abstract T prepare(ResourceManager resourceManager);

    protected abstract void reset();

    protected abstract void process(T obj);

    protected void apply() {
    }

    public static void checkConditions(Map<ResourceLocation, JsonElement> object) {
        object.entrySet().removeIf(e -> {
            if (e.getValue() instanceof JsonObject jo) {
                JsonElement je = jo.get("require_mods");
                if (je != null) {
                    if (je.isJsonArray()) {
                        for (JsonElement el : je.getAsJsonArray()) {
                            if (!PlatStuff.isModLoaded(el.getAsString())) {
                                return true;
                            }
                        }
                    } else if (je.isJsonPrimitive()) {
                        return !PlatStuff.isModLoaded(je.getAsString());
                    }
                }
            }
            return false;
        });

    }
}
