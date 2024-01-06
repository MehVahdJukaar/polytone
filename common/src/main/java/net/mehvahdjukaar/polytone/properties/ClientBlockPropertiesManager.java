package net.mehvahdjukaar.polytone.properties;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public class ClientBlockPropertiesManager extends SimpleJsonResourceReloadListener {

    private final Map<Block, ClientBlockProperties> vanillaValues = new HashMap<>();

    public ClientBlockPropertiesManager() {
        super(new Gson(), Polytone.MOD_ID + "/blocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> list, ResourceManager resourceManager, ProfilerFiller profiler) {
        resetValues();

        Map<String, ClientBlockProperties> propertiesMap = new HashMap<>();
        for (var j : list.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            ClientBlockProperties.CODEC.decode(JsonOps.INSTANCE, json)
                    // log error if parse fails
                    .resultOrPartial(errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    // add loot modifier if parse succeeds
                    .ifPresent(modifier -> propertiesMap.put(j.getKey().getPath(), modifier.getFirst()));
        }

        for (var p : propertiesMap.entrySet()) {
            String s = p.getKey();
            int first = s.lastIndexOf("/");
            int second = s.lastIndexOf("/", first);
            s = s.substring(second);
            s = s.replace("polytone/", "");
            ResourceLocation id = new ResourceLocation(s.replace("/", ":"));
            var block = BuiltInRegistries.BLOCK.getOptional(id);
            if (block.isPresent()) {
                Block b = block.get();
                vanillaValues.put(b, p.getValue().apply(b));
            }
        }
    }

    private void resetValues() {
        for (var e : vanillaValues.entrySet()) {
            e.getValue().apply(e.getKey());
        }
        vanillaValues.clear();
    }
}
