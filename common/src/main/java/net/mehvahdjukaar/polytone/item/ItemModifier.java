package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static net.mehvahdjukaar.polytone.utils.ListUtils.mergeList;

public record ItemModifier(Optional<IColorGetter> barColor,
                           Optional<Rarity> rarity,
                           List<Component> tooltips,
                           List<Pattern> removedTooltips,
                           List<ItemModelOverride> customModels,
                           Targets targets) {

    public static final Codec<ItemModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            //TODO: register custom item model color sampler that takes in a colormap
            //IndexCompoundColorGetter.SINGLE_OR_MULTIPLE.optionalFieldOf("colormap").forGetter(b -> (Optional<IColorGetter>) b.tintGetter),
            Colormap.CODEC.optionalFieldOf("bar_color").forGetter(ItemModifier::barColor),
            Rarity.CODEC.optionalFieldOf("rarity").forGetter(ItemModifier::rarity),
            CreativeTabModifier.COMPONENT_CODEC.listOf().optionalFieldOf("tooltips", java.util.List.of()).forGetter(ItemModifier::tooltips),
            ExtraCodecs.PATTERN.listOf().optionalFieldOf("removed_tooltips", List.of()).forGetter(ItemModifier::removedTooltips),
            ItemModelOverride.CODEC.listOf().optionalFieldOf("custom_models", List.of()).forGetter(ItemModifier::customModels),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(ItemModifier::targets)
    ).apply(instance, ItemModifier::new));

    public record Partial(List<ItemModelOverride.Partial> customModels) {
    }

    public static final Codec<Partial> CODEC_ONLY_MODELS = RecordCodecBuilder.create(instance -> instance.group(
            ItemModelOverride.CODEC_MODEL_ONLY.listOf().optionalFieldOf("custom_models", List.of()).forGetter(Partial::customModels)
    ).apply(instance, Partial::new));

    public static ItemModifier ofBarColor(Colormap colormap) {
        return new ItemModifier( Optional.of(colormap),
                Optional.empty(), List.of(), List.of(), List.of(), Targets.EMPTY);
    }

    public ItemModifier merge(ItemModifier other) {
        return new ItemModifier(
                this.barColor.isPresent() ? this.barColor : other.barColor,
                this.rarity.isPresent() ? this.rarity : other.rarity,
                mergeList(this.tooltips, other.tooltips),
                mergeList(this.removedTooltips, other.removedTooltips),
                mergeList(this.customModels, other.customModels),
                this.targets.merge(other.targets)
        );
    }

    public ItemModifier apply(Item item) {
        Rarity oldRarity = null;

        if (rarity.isPresent()) {
            DataComponentMap components = item.components();
            oldRarity = components.get(DataComponents.RARITY);
            // we must create a new instance as these are immutable and could use one shared by other items
            DataComponentMap.Builder builder = DataComponentMap.builder();
            builder.addAll(components);
            builder.set(DataComponents.RARITY, rarity.get());
            item.components = builder.build();
        }

        // returns old properties
        return new ItemModifier(
                Optional.empty(),
                Optional.ofNullable(oldRarity),
                List.of(), List.of(), List.of(), Targets.EMPTY);
    }

    @Nullable
    public Integer getBarColor(ItemStack itemStack) {
        return barColor.map(c -> c.getItemColor(itemStack, 0)).orElse(null);
    }

    public IColorGetter getBarColor() {
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
