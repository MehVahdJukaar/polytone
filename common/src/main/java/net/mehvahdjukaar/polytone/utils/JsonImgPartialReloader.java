package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;

public abstract class JsonImgPartialReloader extends PartialReloader<JsonImgPartialReloader.Resources> {

    protected JsonImgPartialReloader(String ...name) {
        super(name);
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        var textures = this.getImagesInDirectories(resourceManager);

        this.checkConditions(jsons);
        return new Resources(jsons, textures);
    }

    public record Resources(Map<ResourceLocation, JsonElement> jsons,
                            Map<ResourceLocation, ArrayImage> textures) {
    }


}
