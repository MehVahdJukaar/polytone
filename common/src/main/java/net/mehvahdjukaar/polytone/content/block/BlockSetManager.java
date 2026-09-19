package net.mehvahdjukaar.polytone.content.block;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Map;

public class BlockSetManager extends ContentManager<BlockSetTypeProvider> {

    // we keep our own registry
    private final MapRegistry<BlockSetTypeProvider> blockSetTypes = new MapRegistry<>("Custom Block Set Types");
    private int counter = 0;

    public BlockSetManager() {
        super(Spec.of("Block set", () -> BlockSetTypeProvider.CODEC)
                .folders("custom_block_sets", "block_sets"));
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
                                  RegistryAccess access) {
        var jsons = resources.jsons();
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
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        if (!blockSetTypes.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom block set types", blockSetTypes.size());
        }
    }

    public Codec<BlockSetTypeProvider> byNameCodec() {
        return blockSetTypes;
    }

}
