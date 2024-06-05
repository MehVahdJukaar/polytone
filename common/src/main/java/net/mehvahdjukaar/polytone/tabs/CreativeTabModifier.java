package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record CreativeTabModifier(
        Optional<ItemStack> icon,
        Optional<Boolean> search,
        Optional<Integer> searchWidth,
        Optional<Boolean> canScroll,
        Optional<Boolean> showTitle,
        Optional<Component> name,
        Optional<ResourceLocation> backGroundLocation,
        Optional<ResourceLocation> tabsImage,
        Optional<List<ResourceLocation>> beforeTabs,
        Optional<List<ResourceLocation>> afterTabs,
        List<ItemPredicate> removals,
        List<ItemAddition> additions,
        Set<ResourceLocation> explicitTargets) implements ITargetProvider {

    public static final Codec<CreativeTabModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(ExtraItemCodecs.ITEMSTACK, "icon").forGetter(CreativeTabModifier::icon),
            StrOpt.of(Codec.BOOL, "search_bar").forGetter(CreativeTabModifier::search), //unused
            StrOpt.of(Codec.INT, "search_bar_width").forGetter(CreativeTabModifier::searchWidth),
            StrOpt.of(Codec.BOOL, "can_scroll").forGetter(CreativeTabModifier::canScroll),
            StrOpt.of(Codec.BOOL, "show_title").forGetter(CreativeTabModifier::showTitle),
            StrOpt.of(ExtraCodecs.COMPONENT, "name").forGetter(CreativeTabModifier::name),
            StrOpt.of(ResourceLocation.CODEC, "background").forGetter(CreativeTabModifier::backGroundLocation),
            StrOpt.of(ResourceLocation.CODEC, "tabs_image").forGetter(CreativeTabModifier::tabsImage),
            StrOpt.of(ResourceLocation.CODEC.listOf(), "before_tabs").forGetter(CreativeTabModifier::beforeTabs),
            StrOpt.of(ResourceLocation.CODEC.listOf(), "after_tabs").forGetter(CreativeTabModifier::afterTabs),
            StrOpt.of(ItemPredicate.CODEC.listOf(), "removals", List.of()).forGetter(CreativeTabModifier::removals),
            StrOpt.of(ItemAddition.CODEC.listOf(), "additions", List.of()).forGetter(CreativeTabModifier::additions),
            StrOpt.of(TARGET_CODEC, "targets", Set.of()).forGetter(CreativeTabModifier::explicitTargets)
    ).apply(i, CreativeTabModifier::new));


    public CreativeTabModifier merge(CreativeTabModifier other) {
        return new CreativeTabModifier(
                icon.isPresent() ? icon : other.icon,
                search.isPresent() ? search : other.search,
                searchWidth.isPresent() ? searchWidth : other.searchWidth,
                canScroll.isPresent() ? canScroll : other.canScroll,
                showTitle.isPresent() ? showTitle : other.showTitle,
                name.isPresent() ? name : other.name,
                backGroundLocation.isPresent() ? backGroundLocation : other.backGroundLocation,
                tabsImage.isPresent() ? tabsImage : other.tabsImage,
                beforeTabs.isPresent() ? beforeTabs : other.beforeTabs,
                afterTabs.isPresent() ? afterTabs : other.afterTabs,
                mergeList(removals, other.removals),
                mergeList(additions, other.additions),
                mergeSet(explicitTargets, other.explicitTargets)
        );
    }

    public CreativeTabModifier applyItemsAndAttributes(ItemToTabEvent event) {
        for (var v : removals) {
            event.removeItems(v);
        }

        for (var v : additions) {
            if (v.before()) {
                event.addBefore(v.predicate(), v.stack().toArray(ItemStack[]::new));
            } else {
                event.addAfter(v.predicate(), v.stack().toArray(ItemStack[]::new));
            }
        }

        return applyAttributes(event.getTab());
    }

    public CreativeTabModifier applyAttributes(ResourceKey<CreativeModeTab> key) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);

        return PlatStuff.modifyTab(this, tab);
    }


}
