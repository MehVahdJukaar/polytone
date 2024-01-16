package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
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

    protected void apply(){}

    public static void scanDirectory(ResourceManager resourceManager, String string, Gson gson, Map<ResourceLocation, JsonElement> map) {
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(string);
        Iterator var5 = fileToIdConverter.listMatchingResources(resourceManager).entrySet().iterator();

        while(var5.hasNext()) {
            Map.Entry<ResourceLocation, Resource> entry = (Map.Entry)var5.next();
            ResourceLocation resourceLocation = (ResourceLocation)entry.getKey();
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);

            try {
                Reader reader = ((Resource)entry.getValue()).openAsReader();

                try {
                    JsonElement jsonElement = (JsonElement) GsonHelper.fromJson(gson, reader, JsonElement.class);
                    JsonElement jsonElement2 = (JsonElement)map.put(resourceLocation2, jsonElement);
                    if (jsonElement2 != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2);
                    }
                } catch (Throwable var13) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Throwable var12) {
                            var13.addSuppressed(var12);
                        }
                    }

                    throw var13;
                }

                if (reader != null) {
                    reader.close();
                }
            } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                Polytone.LOGGER.error("Couldn't parse data file {} from {}", new Object[]{resourceLocation2, resourceLocation, var14});
            }
        }

    }

}
