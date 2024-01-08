package net.mehvahdjukaar.polytone;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NewStuff {

    public static void scanDirectory(ResourceManager resourceManager, String name, Gson gson, Map<ResourceLocation, JsonElement> output) {
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(name);
        Iterator var5 = fileToIdConverter.listMatchingResources(resourceManager).entrySet().iterator();

        while(var5.hasNext()) {
            Map.Entry<ResourceLocation, Resource> entry = (Map.Entry)var5.next();
            ResourceLocation resourceLocation = (ResourceLocation)entry.getKey();
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);

            try {
                Reader reader = ((Resource)entry.getValue()).openAsReader();

                try {
                    JsonElement jsonElement = (JsonElement) GsonHelper.fromJson(gson, (Reader)reader, (Class)JsonElement.class);
                    JsonElement jsonElement2 = (JsonElement)output.put(resourceLocation2, jsonElement);
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
                Polytone.LOGGER.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, var14);
            }
        }

    }


    public static class FileToIdConverter {
        private final String prefix;
        private final String extension;

        public FileToIdConverter(String string, String string2) {
            this.prefix = string;
            this.extension = string2;
        }

        public static FileToIdConverter json(String name) {
            return new FileToIdConverter(name, ".json");
        }

        public ResourceLocation idToFile(ResourceLocation id) {
            String var10001 = this.prefix;
            return new ResourceLocation(id.getNamespace(),var10001 + "/" + id.getPath() + this.extension);
        }

        public ResourceLocation fileToId(ResourceLocation file) {
            String string = file.getPath();
            return new ResourceLocation(file.getNamespace(),string.substring(this.prefix.length() + 1, string.length() - this.extension.length()));
        }

        public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager resourceManager) {
            return resourceManager.listResources(this.prefix, (resourceLocation) -> {
                return resourceLocation.getPath().endsWith(this.extension);
            });
        }

        public Map<ResourceLocation, List<Resource>> listMatchingResourceStacks(ResourceManager resourceManager) {
            return resourceManager.listResourceStacks(this.prefix, (resourceLocation) -> {
                return resourceLocation.getPath().endsWith(this.extension);
            });
        }
    }

}
