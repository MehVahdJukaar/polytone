package net.mehvahdjukaar.polytone.common.reloader;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Map;

public abstract class JsonPartialReloader extends PartialReloader<Map<Identifier, JsonElement>> {

    protected JsonPartialReloader(String ...name) {
        super(name);
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);
        return ImmutableMap.copyOf(jsons);
    }
}
