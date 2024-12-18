package net.mehvahdjukaar.polytone.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
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
    protected void resetWithLevel(boolean logOff) {
        blockSetTypes.clear();
        //copy vanilla
        counter = 0;
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        //copy vanilla
        BlockSetType.values().forEach(type ->
                blockSetTypes.register(ResourceLocation.parse(type.name()),
                        new BlockSetTypeProvider.Vanilla(type)));
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            BlockSetTypeProvider type = BlockSetTypeProvider.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Block Set Type with json id " + id + " - error: " + errorMsg
                    )).getFirst();
            blockSetTypes.register(id, type);
        }

    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        if (!blockSetTypes.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom block set types", blockSetTypes.size());
        }
    }

    public Codec<BlockSetTypeProvider> byNameCodec() {
        return blockSetTypes;
    }

}
