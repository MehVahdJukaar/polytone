package net.mehvahdjukaar.polytone.block;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Map;

public class BlockSetManager extends JsonPartialReloader {

    // we keep our own registry
    private final BiMap<ResourceLocation, BlockSetTypeProvider> blockSetTypes = HashBiMap.create();
    private int counter = 0;

    public BlockSetManager() {
        super("custom_block_sets", "block_sets");
    }

    public String getNextName() {
        return "polytone:custom_" + counter++;
    }

    @Override
    protected void reset() {
        blockSetTypes.clear();
        //copy vanilla
        BlockSetType.values().forEach(type ->
                blockSetTypes.put(new ResourceLocation(type.name()),
                        new BlockSetTypeProvider.Vanilla(type)));
        counter = 0;
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> jsons) {
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            BlockSetTypeProvider type = BlockSetTypeProvider.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Block Set Type with json id " + id + " - error: " + errorMsg
                    )).getFirst();
            blockSetTypes.put(id, type);
        }

    }

    public BlockSetTypeProvider get(ResourceLocation id) {
        return blockSetTypes.get(id);
    }

    public ResourceLocation getKey(BlockSetTypeProvider object) {
        return blockSetTypes.inverse().get(object);
    }
}
