package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.ItemToTabEvent;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record CreativeTabModifier(
        Optional<ItemStack> icon,
        List<ItemPredicate> removals,
        List<ItemAddition> additions,
        List<ResourceLocation> targets) {

    public static final Codec<CreativeTabModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(ItemStack.CODEC, "icon").forGetter(CreativeTabModifier::icon),
            StrOpt.of(ItemPredicate.CODEC.listOf(), "removals", List.of()).forGetter(CreativeTabModifier::removals),
            StrOpt.of(ItemAddition.CODEC.listOf(), "additions", List.of()).forGetter(CreativeTabModifier::additions),
            StrOpt.of(ResourceLocation.CODEC.listOf(), "targets", List.of()).forGetter(CreativeTabModifier::targets)
    ).apply(i, CreativeTabModifier::new));

    public void apply(ResourceKey<CreativeModeTab> tab, ItemToTabEvent event) {
        for(var v : additions){
            if(v.before()){
                event.addBefore(tab, v.predicate(), v.stack().toArray(ItemStack[]::new));
            }else{
                event.addAfter(tab, v.predicate(), v.stack().toArray(ItemStack[]::new));
            }
        }
    }
}
