package net.mehvahdjukaar.polytone.misc.reloader;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Map;

public abstract class JsonPartialReloader extends PartialReloader<Map<ResourceLocation, JsonElement>> {

    protected JsonPartialReloader(String ...name) {
        super(name);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);
        return ImmutableMap.copyOf(jsons);
    }
}
