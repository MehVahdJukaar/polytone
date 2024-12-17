package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static net.mehvahdjukaar.polytone.utils.ListUtils.mergeList;

public record ItemModifier(Optional<? extends ItemColor> tintGetter,
                           Optional<IColorGetter> barColor,
                           Optional<Rarity> rarity,
                           List<Component> tooltips,
                           List<Pattern> removedTooltips,
                           Set<ResourceLocation> explicitTargets) implements ITargetProvider {

    public static final Codec<ItemModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StrOpt.of(IndexCompoundColorGetter.SINGLE_OR_MULTIPLE, "colormap").forGetter(b -> (Optional<IColorGetter>) b.tintGetter),
            StrOpt.of(Colormap.CODEC, "bar_color").forGetter(ItemModifier::barColor),
            StrOpt.of(StringRepresentable.fromEnum(Rarity::values), "rarity").forGetter(ItemModifier::rarity),
            StrOpt.of(ExtraCodecs.COMPONENT.listOf(), "tooltips", java.util.List.of()).forGetter(ItemModifier::tooltips),
            StrOpt.of(ExtraCodecs.PATTERN.listOf(), "removed_tooltips", List.of()).forGetter(ItemModifier::removedTooltips),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(ItemModifier::targets)
    ).apply(instance, ItemModifier::new));

    public record Partial(List<ItemModelOverride.Partial> customModels) {
    }

    public static final Codec<Partial> CODEC_ONLY_MODELS = RecordCodecBuilder.create(instance -> instance.group(
            ItemModelOverride.CODEC_MODEL_ONLY.listOf().optionalFieldOf("custom_models", List.of()).forGetter(Partial::customModels)
    ).apply(instance, Partial::new));

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
                this.explicitTargets.merge(other.explicitTargets)
        );
    }

    public ItemModifier apply(Item item) {
        net.minecraft.world.item.Rarity oldRarity = null;
        if (rarity.isPresent()) {
            oldRarity = item.getRarity(item.getDefaultInstance());
            item.rarity = rarity.get().toVanilla();
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
                Optional.ofNullable(Rarity.fromVanilla(oldRarity)),
                List.of(), List.of(), Targets.EMPTY);
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


    public enum Rarity implements StringRepresentable {
        COMMON, UNCOMMON, RARE, EPIC;

        public static Rarity fromVanilla(net.minecraft.world.item.Rarity oldRarity) {
            if (oldRarity == null) return null;
            return switch (oldRarity) {
                case COMMON -> COMMON;
                case UNCOMMON -> UNCOMMON;
                case RARE -> RARE;
                case EPIC -> EPIC;
            };
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public net.minecraft.world.item.Rarity toVanilla() {
            return switch (this) {
                case COMMON -> net.minecraft.world.item.Rarity.COMMON;
                case UNCOMMON -> net.minecraft.world.item.Rarity.UNCOMMON;
                case RARE -> net.minecraft.world.item.Rarity.RARE;
                case EPIC -> net.minecraft.world.item.Rarity.EPIC;
            };
        }
    }

    public void modifyTooltips(List<Component> tooltips) {
        tooltips.removeIf(t -> removedTooltips.stream().anyMatch(p -> p.matcher(t.getString()).matches()));
        tooltips.addAll(this.tooltips);
    }

    public boolean shouldAttachToItem() {
        return !tooltips.isEmpty() || !removedTooltips.isEmpty() || barColor.isPresent();
    }
}
