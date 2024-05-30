package net.mehvahdjukaar.polytone.block;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.particle.ParticleModifier;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Map;

public class BlockSetManager extends JsonPartialReloader {

    // we keep our own registry
    private final BiMap<ResourceLocation, BlockSetTypeProvider> blockSetTypes = HashBiMap.create();

    public BlockSetManager() {
        super("custom_block_sets", "block_sets");
    }

    private int counter = 0;

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
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> jsons) {
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            BlockSetTypeProvider type = BlockSetTypeProvider.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Custom Block Set Type with json id {} - error: {}",
                            id, errorMsg)).getFirst();
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
