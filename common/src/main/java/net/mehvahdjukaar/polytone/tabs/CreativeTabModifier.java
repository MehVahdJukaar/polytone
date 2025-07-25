package net.mehvahdjukaar.polytone.tabs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.CodecUtil;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
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
        Targets targets) {

    public static final Codec<Component> COMPONENT_CODEC = Codec.either(ExtraCodecs.COMPONENT, ExtraCodecs.FLAT_COMPONENT).xmap(
            e -> e.map(Function.identity(), Function.identity()), Either::left
    );

    public static final Codec<CreativeTabModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(CodecUtil.ITEM_OR_STACK, "icon").forGetter(CreativeTabModifier::icon),
            StrOpt.of(Codec.BOOL, "search_bar").forGetter(CreativeTabModifier::search), //unused
            StrOpt.of(Codec.INT, "search_bar_width").forGetter(CreativeTabModifier::searchWidth),
            StrOpt.of(Codec.BOOL, "can_scroll").forGetter(CreativeTabModifier::canScroll),
            StrOpt.of(Codec.BOOL, "show_title").forGetter(CreativeTabModifier::showTitle),
            StrOpt.of(COMPONENT_CODEC, "name").forGetter(CreativeTabModifier::name),
            StrOpt.of(ResourceLocation.CODEC, "background").forGetter(CreativeTabModifier::backGroundLocation),
            StrOpt.of(ResourceLocation.CODEC, "tabs_image").forGetter(CreativeTabModifier::tabsImage),
            StrOpt.of(ResourceLocation.CODEC.listOf(), "before_tabs").forGetter(CreativeTabModifier::beforeTabs),
            StrOpt.of(ResourceLocation.CODEC.listOf(), "after_tabs").forGetter(CreativeTabModifier::afterTabs),
            StrOpt.of(ItemPredicate.CODEC.listOf(), "removals", List.of()).forGetter(CreativeTabModifier::removals),
            StrOpt.of(ItemAddition.CODEC.listOf(), "additions", List.of()).forGetter(CreativeTabModifier::additions),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(CreativeTabModifier::targets)
    ).apply(i, CreativeTabModifier::new));


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
                this.targets.merge(newMod.targets)
        );
    }

    public CreativeTabModifier applyItemsAndAttributes(ItemToTabEvent event, RegistryAccess access) {
        for (var v : removals) {
            event.removeItems(v);
        }

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
