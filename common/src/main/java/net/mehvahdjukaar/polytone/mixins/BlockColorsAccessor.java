package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(BlockColors.class)
public interface BlockColorsAccessor {

    @Accessor
    Map<Block, List<BlockTintSource>> getSources();
}