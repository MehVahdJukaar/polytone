package net.mehvahdjukaar.polytone.mixins.neoforge;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemColors.class)
public interface ItemColorsAccessor {

    @Accessor
    Map<Item, ItemColor> getItemColors();
}
