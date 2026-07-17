package net.mehvahdjukaar.polytone.content.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.core.HolderLookup;
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
import java.util.function.Function;

import static net.mehvahdjukaar.polytone.utils.Utils.mergeList;

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
        boolean registerTab,
        Targets targets) {

    public static final Codec<Component> COMPONENT_CODEC = Codec.withAlternative(ComponentSerialization.CODEC, ComponentSerialization.FLAT_CODEC,
            Function.identity());

    public static final Codec<CreativeTabModifier> CODEC =
                    RecordCodecBuilder.<CreativeTabModifier>create(i -> i.group(
                            SchemaCodecs.ITEM_OR_STACK.optionalFieldOf("icon").forGetter(CreativeTabModifier::icon),
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
                    Codec.BOOL.optionalFieldOf("create_new", false).forGetter(CreativeTabModifier::registerTab),
                    Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(CreativeTabModifier::targets)
            ).apply(i, CreativeTabModifier::new))
            .validate(
                    m -> {
                        if (m.registerTab && (!m.removals.isEmpty() || m.targets != Targets.EMPTY)) {
                            return DataResult.error(() -> "Modifiers that register new creative tabs cannot have item removals or target existing tabs.");
                        }
                        return DataResult.success(m);
                    });


    public CreativeTabModifier merge(CreativeTabModifier newMod) {
        return new CreativeTabModifier(
                newMod.icon.isPresent() ? newMod.icon : this.icon,
                newMod.search.isPresent() ? newMod.search : this.search,
                newMod.searchWidth.isPresent() ? newMod.searchWidth : this.searchWidth,
                newMod.canScroll.isPresent() ? newMod.canScroll : this.canScroll,
                newMod.showTitle.isPresent() ? newMod.showTitle : this.showTitle,
                newMod.name.isPresent() ? newMod.name : this.name,
                newMod.backGroundLocation.isPresent() ? newMod.backGroundLocation : this.backGroundLocation,
                newMod.tabsImage.isPresent() ? newMod.tabsImage : this.tabsImage,
                newMod.beforeTabs.isPresent() ? newMod.beforeTabs : this.beforeTabs,
                newMod.afterTabs.isPresent() ? newMod.afterTabs : this.afterTabs,
                mergeList(newMod.removals, this.removals),
                mergeList(newMod.additions, this.additions),
                this.registerTab || newMod.registerTab,
                this.targets.merge(newMod.targets)
        );
    }

    public CreativeTabModifier applyItemsAndAttributes(ItemToTabEvent event, HolderLookup.Provider access) {
        for (var v : removals) {
            event.removeItems(v);
        }

        outer:
        for (var v : additions) {
            List<ItemStack> stacks = v.items().get();
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
            for (var s : stacks) {
                if (event.getAllItems().contains(s)) {
                    Polytone.LOGGER.error("Attempted to add item {} to creative tab {} but it already contains it! This likely means you didnt add an item remover for said item. Pack load failed.", s, event.getTab());
                    Polytone.displayLateReloadFailedToast();
                    break outer;
                }
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
        if (tab == null) {
            Polytone.LOGGER.error("Could not find creative mode tab with ID {}. What?", key);
        }
        return PlatStuff.modifyTab(this, tab);
    }

}
