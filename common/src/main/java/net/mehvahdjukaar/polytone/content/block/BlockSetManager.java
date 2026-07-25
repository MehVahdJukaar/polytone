package net.mehvahdjukaar.polytone.content.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Map;

public class BlockSetManager extends ContentManager<BlockSetTypeProvider> {

    // we keep our own registry
    private final MapRegistry<BlockSetTypeProvider> blockSetTypes = new MapRegistry<>("Custom Block Set Types");
    private int counter = 0;

    public BlockSetManager() {
        super("Block set", () -> SchemaCodec.wrap(BlockSetTypeProvider.CODEC),
                "custom_block_sets", "block_sets");
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
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        //copy vanilla
        BlockSetType.values().forEach(type ->
                blockSetTypes.register(Identifier.parse(type.name()),
                        new BlockSetTypeProvider.Vanilla(type)));
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            BlockSetTypeProvider type = decodeStrict(json, id, ops);
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
