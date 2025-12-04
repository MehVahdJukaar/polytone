package net.mehvahdjukaar.polytone.mixins.neoforge;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handlers.ClientPayloadHandler;
import net.neoforged.neoforge.registries.RegistryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(BlockColors.class)
public interface BlockColorsAccessor {

    @Accessor
    Map<Block, BlockColor> getBlockColors();
}
