package net.mehvahdjukaar.polytone.mixins.fabric;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.core.IdMapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemColors.class)
public interface ItemColorsAccessor {

    @Accessor
    IdMapper<ItemColor>  getItemColors();
}
