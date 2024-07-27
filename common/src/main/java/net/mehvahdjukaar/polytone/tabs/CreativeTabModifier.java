package net.mehvahdjukaar.polytone.tabs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static net.mehvahdjukaar.polytone.tabs.ExtraItemCodecs.ITEM_OR_STACK;

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

    public static final Codec<Component> COMPONENT_CODEC = Codec.either(ComponentSerialization.CODEC, ComponentSerialization.FLAT_CODEC).xmap(
            e -> e.map(Function.identity(), Function.identity()), Either::left
    );

    public static final Codec<CreativeTabModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            ITEM_OR_STACK.optionalFieldOf("icon").forGetter(CreativeTabModifier::icon),
            Codec.BOOL.optionalFieldOf("search_bar").forGetter(CreativeTabModifier::search), //unused
            Codec.INT.optionalFieldOf("search_bar_width").forGetter(CreativeTabModifier::searchWidth),
            Codec.BOOL.optionalFieldOf("can_scroll").forGetter(CreativeTabModifier::canScroll),
            Codec.BOOL.optionalFieldOf("show_title").forGetter(CreativeTabModifier::showTitle),
            COMPONENT_CODEC.optionalFieldOf("name").forGetter(CreativeTabModifier::name),
            ResourceLocation.CODEC.optionalFieldOf("background").forGetter(CreativeTabModifier::backGroundLocation),
            ResourceLocation.CODEC.optionalFieldOf("tabs_image").forGetter(CreativeTabModifier::tabsImage),
            ResourceLocation.CODEC.listOf().optionalFieldOf("before_tabs").forGetter(CreativeTabModifier::beforeTabs),
            ResourceLocation.CODEC.listOf().optionalFieldOf("after_tabs").forGetter(CreativeTabModifier::afterTabs),
            ItemPredicate.CODEC.listOf().optionalFieldOf("removals", List.of()).forGetter(CreativeTabModifier::removals),
            ItemAddition.CODEC.listOf().optionalFieldOf("additions", List.of()).forGetter(CreativeTabModifier::additions),
            TARGET_CODEC.optionalFieldOf("targets", Set.of()).forGetter(CreativeTabModifier::explicitTargets)
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

    public CreativeTabModifier applyItemsAndAttributes(ItemToTabEvent event, RegistryAccess access) {
        for (var v : removals) {
            event.removeItems(v);
        }

        for (var v : additions) {
            List<ItemStack> stacks = v.items();
            if (stacks == null) continue;
            if (v.inverse()) {
                List<ItemStack> newList = new ArrayList<>();
                var not = stacks.stream().map(ItemStack::getItem).toList();
                for (var i : BuiltInRegistries.ITEM) {
                    if (!not.contains(i)) {
                        newList.add(i.getDefaultInstance());
                    }
                }
                stacks = newList;
            }
            if (v.before()) {
                event.addBefore(v.predicate(), stacks.toArray(ItemStack[]::new));
            } else {
                event.addAfter(v.predicate(), stacks.toArray(ItemStack[]::new));
            }
        }

        return applyAttributes(event.getTab());
    }

    public CreativeTabModifier applyAttributes(ResourceKey<CreativeModeTab> key) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
        return PlatStuff.modifyTab(this, tab);
    }


}
