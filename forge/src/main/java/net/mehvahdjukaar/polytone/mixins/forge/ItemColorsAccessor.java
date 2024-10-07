package net.mehvahdjukaar.polytone.mixins.forge;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemColors.class)
public interface ItemColorsAccessor {

    @Accessor
    Map<Holder.Reference<Item>, ItemColor> getItemColors();
}
