package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CustomCreativeTab(List<ItemStack> items, ItemStack icon, Component name, boolean search) {

    public static final Codec<CustomCreativeTab> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraItemCodecs.ITEMSTACK_OR_ITEMSTACK_LIST.fieldOf("items").forGetter(CustomCreativeTab::items),
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("icon").forGetter(CustomCreativeTab::icon),
            ComponentSerialization.CODEC.optionalFieldOf("name", Component.empty()).forGetter(CustomCreativeTab::name),
            Codec.BOOL.optionalFieldOf("search", false).forGetter(CustomCreativeTab::search)
    ).apply(i, CustomCreativeTab::new));
}
