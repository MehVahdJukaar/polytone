package net.mehvahdjukaar.polytone.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
public class FileToIdConverter {
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
        return new ResourceLocation(id.getNamespace(), var10001 + "/" + id.getPath() + this.extension);
    }

    public ResourceLocation fileToId(ResourceLocation file) {
        String string = file.getPath();
        return new ResourceLocation(file.getNamespace(),string.substring(this.prefix.length() + 1, string.length() - this.extension.length()));
    }

    public Collection<ResourceLocation> listMatchingResources(ResourceManager resourceManager) {
        return resourceManager.listResources(this.prefix, (resourceLocation) -> {
            return resourceLocation.endsWith(this.extension);
        });
    }
}
