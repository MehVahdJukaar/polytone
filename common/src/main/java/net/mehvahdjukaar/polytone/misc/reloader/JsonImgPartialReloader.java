package net.mehvahdjukaar.polytone.misc.reloader;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.misc.data.ArrayImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Map;

public abstract class JsonImgPartialReloader extends PartialReloader<JsonImgPartialReloader.Resources> {

    protected JsonImgPartialReloader(String ...name) {
        super(name);
    }

    @Override
    protected Resources prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);
        var textures = this.getImagesInDirectories(resourceManager);

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures));
    }

    public record Resources(Map<ResourceLocation, JsonElement> jsons,
                            Map<ResourceLocation, ArrayImage> textures) {
    }


}
