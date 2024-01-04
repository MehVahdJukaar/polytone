package net.mehvahdjukaar.polytone;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientBlockPropertiesManager extends SimpleJsonResourceReloadListener {

    private final Map<Block, ClientBlockProperties> vanillaValues = new HashMap<>();

    public ClientBlockPropertiesManager() {
        super(new Gson(), VisualProperties.MOD_ID+"/blocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> list, ResourceManager resourceManager, ProfilerFiller profiler) {
        resetValues();

        List<ClientBlockProperties> propertiesList = new ArrayList<>();
        for(var j : list.entrySet()){
            var json = j.getValue();
            var id = j.getKey();

            ClientBlockProperties.CODEC.decode(JsonOps.INSTANCE, json)
                    // log error if parse fails
                    .resultOrPartial(errorMsg -> VisualProperties.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    // add loot modifier if parse succeeds
                    .ifPresent(modifier -> propertiesList.add(modifier.getFirst()));
        }

        for(var p : propertiesList) {
            if (p.hasBlock()) {
                var changed = p.apply();
                vanillaValues.merge(changed.block(), changed, ClientBlockProperties::merge);
            }
        }
    }

    private void resetValues() {
        for (var e : vanillaValues.entrySet()) {
            e.getValue().apply();
        }
        vanillaValues.clear();
    }
}
