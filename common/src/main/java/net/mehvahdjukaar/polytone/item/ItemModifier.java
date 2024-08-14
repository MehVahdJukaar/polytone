package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public record ItemModifier(Optional<? extends ItemColor> tintGetter,
                           Optional<IColorGetter> barColor,
                           Optional<Rarity> rarity,
                           List<Component> tooltips,
                           List<Pattern> removedTooltips,
                           Set<ResourceLocation> explicitTargets) implements ITargetProvider {

    public static final Codec<ItemModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IndexCompoundColorGetter.SINGLE_OR_MULTIPLE.optionalFieldOf("colormap").forGetter(b -> (Optional<IColorGetter>) b.tintGetter),
            Colormap.CODEC.optionalFieldOf("bar_color").forGetter(ItemModifier::barColor),
            Rarity.CODEC.optionalFieldOf("rarity").forGetter(ItemModifier::rarity),
            CreativeTabModifier.COMPONENT_CODEC.listOf().optionalFieldOf("tooltips", java.util.List.of()).forGetter(ItemModifier::tooltips),
            ExtraCodecs.PATTERN.listOf().optionalFieldOf("removed_tooltips", List.of()).forGetter(ItemModifier::removedTooltips),
            TARGET_CODEC.optionalFieldOf("targets", java.util.Set.of()).forGetter(ItemModifier::explicitTargets)
    ).apply(instance, ItemModifier::new));

    public static ItemModifier ofItemColor(Colormap colormap) {
        return new ItemModifier(Optional.of(colormap), Optional.empty(), Optional.empty(), List.of(), List.of(), java.util.Set.of());
    }

    public static ItemModifier ofBarColor(Colormap colormap) {
        return new ItemModifier(Optional.empty(), Optional.of(colormap),
                Optional.empty(), List.of(), List.of(), java.util.Set.of());
    }

    public ItemModifier merge(ItemModifier other) {
        return new ItemModifier(
                this.tintGetter.isPresent() ? this.tintGetter : other.tintGetter,
                this.barColor.isPresent() ? this.barColor : other.barColor,
                this.rarity.isPresent() ? this.rarity : other.rarity,
                mergeList(this.tooltips, other.tooltips),
                mergeList(this.removedTooltips, other.removedTooltips),
                mergeSet(this.explicitTargets, other.explicitTargets)
        );
    }

    public ItemModifier apply(Item item) {
        Rarity oldRarity = null;

        if (rarity.isPresent()) {
            oldRarity = item.components().get(DataComponents.RARITY);
            if (item.components() instanceof DataComponentMap.Builder.SimpleMap sm) {
                sm.map().put(DataComponents.RARITY, rarity.get());
            }
        }
        ItemColor oldColor = null;
        if (tintGetter.isPresent()) {
            ItemColors itemColors = Minecraft.getInstance().itemColors;
            oldColor = PlatStuff.getItemColor(itemColors, item);
            itemColors.register(tintGetter.get(), item);
        }

        // returns old properties
        return new ItemModifier(
                Optional.ofNullable(oldColor),
                Optional.empty(),
                Optional.ofNullable(oldRarity),
                List.of(), List.of(), Set.of());
    }

    @Nullable
    public Integer getBarColor(ItemStack itemStack) {
        return barColor.map(c -> c.getColor(itemStack, 0)).orElse(null);
    }

    public boolean hasTint() {
        return tintGetter.isPresent();
    }

    public ItemColor getTint() {
        return tintGetter.orElse(null);
    }

    public ItemColor getBarColor() {
        return barColor.orElse(null);
    }

    public boolean hasBarColor() {
        return barColor.isPresent();
    }

    public void modifyTooltips(List<Component> tooltips) {
        tooltips.removeIf(t -> removedTooltips.stream().anyMatch(p -> p.matcher(t.getString()).matches()));
        tooltips.addAll(this.tooltips);
    }

    public boolean shouldAttachToItem() {
        return !tooltips.isEmpty() || !removedTooltips.isEmpty() || barColor.isPresent();
    }
}
