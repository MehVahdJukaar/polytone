package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.TargetsHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record CreativeTabModifier(
        Optional<ItemStack> icon,
        List<ItemPredicate> removals,
        List<ItemAddition> additions,
        Optional<Set<ResourceLocation>> explicitTargets) implements ITargetProvider {

    public static final Codec<CreativeTabModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(ItemStack.CODEC, "icon").forGetter(CreativeTabModifier::icon),
            StrOpt.of(ItemPredicate.CODEC.listOf(), "removals", java.util.List.of()).forGetter(CreativeTabModifier::removals),
            StrOpt.of(ItemAddition.CODEC.listOf(), "additions", java.util.List.of()).forGetter(CreativeTabModifier::additions),
            StrOpt.of(TargetsHelper.CODEC, "targets").forGetter(CreativeTabModifier::explicitTargets)
    ).apply(i, CreativeTabModifier::new));

    public void apply(ItemToTabEvent event) {
        for (var v : additions) {
            if (v.before()) {
                event.addBefore(v.predicate(), v.stack().toArray(ItemStack[]::new));
            } else {
                event.addAfter(v.predicate(), v.stack().toArray(ItemStack[]::new));
            }
        }
        for (var v : removals) {
            event.removeItems(v);
        }
    }
}
