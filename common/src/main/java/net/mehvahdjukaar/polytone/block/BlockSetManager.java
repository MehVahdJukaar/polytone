package net.mehvahdjukaar.polytone.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Map;

public class BlockSetManager extends JsonPartialReloader {

    // we keep our own registry
    private final MapRegistry<BlockSetTypeProvider> blockSetTypes = new MapRegistry<>("Custom Block Set Types");
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
                blockSetTypes.register(new ResourceLocation(type.name()),
                        new BlockSetTypeProvider.Vanilla(type)));
        counter = 0;
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            BlockSetTypeProvider type = BlockSetTypeProvider.CODEC.decode(ops, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Custom Block Set Type with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            blockSetTypes.register(id, type);
        }

    }

    public Codec<BlockSetTypeProvider> byNameCodec() {
        return blockSetTypes;
    }

}
