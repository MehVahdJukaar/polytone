package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.CodecUtil;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CustomCreativeTab(List<ItemStack> items, ItemStack icon, Component name, boolean search) {

    public static final Codec<CustomCreativeTab> CODEC = RecordCodecBuilder.create(i -> i.group(
            CodecUtil.ITEMSTACK_OR_ITEMSTACK_LIST.fieldOf("items").forGetter(CustomCreativeTab::items),
            CodecUtil.ITEMSTACK.fieldOf("icon").forGetter(CustomCreativeTab::icon),
            StrOpt.of(ExtraCodecs.COMPONENT, "name", Component.empty()).forGetter(CustomCreativeTab::name),
            StrOpt.of(Codec.BOOL, "search", false).forGetter(CustomCreativeTab::search)
    ).apply(i, CustomCreativeTab::new));
}
