package net.mehvahdjukaar.polytone.content.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.content.model.WornModel;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static net.mehvahdjukaar.polytone.utils.Utils.mergeList;

public record ItemModifier(Optional<? extends ItemColor> tintGetter,
                           Optional<IColorGetter> barColor,
                           Optional<Rarity> rarity,
                           List<TooltipAddition> tooltips,
                           List<Pattern> removedTooltips,
                           List<ItemModelOverride> customModels,
                           Optional<WornModel> wornModel,
                           // NeoForge IClientItemExtensions backed (Fabric only reads wornModel)
                           Optional<IColorGetter> armorTint,
                           Optional<ResourceLocation> armorTexture,
                           Optional<HumanoidModel.ArmPose> armPose,
                           Optional<ResourceLocation> scopeOverlay,
                           Optional<Boolean> bobAsEntity,
                           Optional<Boolean> spreadAsEntity,
                           Targets targets) {

    private static final Codec<HumanoidModel.ArmPose> ARM_POSE_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(HumanoidModel.ArmPose.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown arm_pose: " + s);
                }
            },
            p -> p.name().toLowerCase(Locale.ROOT));

    public static final Codec<ItemModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IndexCompoundColorGetter.SINGLE_OR_MULTIPLE.optionalFieldOf("colormap").forGetter(b -> (Optional<IColorGetter>) b.tintGetter),
            Colormap.CODEC.optionalFieldOf("bar_color").forGetter(ItemModifier::barColor),
            Rarity.CODEC.optionalFieldOf("rarity").forGetter(ItemModifier::rarity),
            TooltipAddition.CODEC.listOf().optionalFieldOf("tooltips", java.util.List.of()).forGetter(ItemModifier::tooltips),
            ExtraCodecs.PATTERN.listOf().optionalFieldOf("removed_tooltips", List.of()).forGetter(ItemModifier::removedTooltips),
            ItemModelOverride.CODEC.listOf().optionalFieldOf("custom_models", List.of()).forGetter(ItemModifier::customModels),
            WornModel.CODEC.optionalFieldOf("worn_model").forGetter(ItemModifier::wornModel),
            Colormap.CODEC.optionalFieldOf("armor_tint").forGetter(ItemModifier::armorTint),
            ResourceLocation.CODEC.optionalFieldOf("armor_texture").forGetter(ItemModifier::armorTexture),
            ARM_POSE_CODEC.optionalFieldOf("arm_pose").forGetter(ItemModifier::armPose),
            ResourceLocation.CODEC.optionalFieldOf("scope_overlay").forGetter(ItemModifier::scopeOverlay),
            Codec.BOOL.optionalFieldOf("bob_as_entity").forGetter(ItemModifier::bobAsEntity),
            Codec.BOOL.optionalFieldOf("spread_as_entity").forGetter(ItemModifier::spreadAsEntity),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(ItemModifier::targets)
    ).apply(instance, ItemModifier::new));

    public record Partial(List<ItemModelOverride.Partial> customModels) {
    }

    public static final Codec<Partial> CODEC_ONLY_MODELS = RecordCodecBuilder.create(instance -> instance.group(
            ItemModelOverride.CODEC_MODEL_ONLY.listOf().optionalFieldOf("custom_models", List.of()).forGetter(Partial::customModels)
    ).apply(instance, Partial::new));

    public static ItemModifier ofItemColor(Colormap colormap) {
        return new ItemModifier(Optional.of(colormap), Optional.empty(), Optional.empty(), List.of(),
                List.of(), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    public static ItemModifier ofBarColor(Colormap colormap) {
        return new ItemModifier(Optional.empty(), Optional.of(colormap),
                Optional.empty(), List.of(), List.of(), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    public ItemModifier merge(ItemModifier newMod) {
        return new ItemModifier(
                newMod.tintGetter.isPresent() ? newMod.tintGetter : this.tintGetter,
                newMod.barColor.isPresent() ? newMod.barColor : this.barColor,
                newMod.rarity.isPresent() ? newMod.rarity : this.rarity,
                mergeList(newMod.tooltips, this.tooltips),
                mergeList(newMod.removedTooltips, this.removedTooltips),
                mergeList(newMod.customModels, this.customModels),
                newMod.wornModel.isPresent() ? newMod.wornModel : this.wornModel,
                newMod.armorTint.isPresent() ? newMod.armorTint : this.armorTint,
                newMod.armorTexture.isPresent() ? newMod.armorTexture : this.armorTexture,
                newMod.armPose.isPresent() ? newMod.armPose : this.armPose,
                newMod.scopeOverlay.isPresent() ? newMod.scopeOverlay : this.scopeOverlay,
                newMod.bobAsEntity.isPresent() ? newMod.bobAsEntity : this.bobAsEntity,
                newMod.spreadAsEntity.isPresent() ? newMod.spreadAsEntity : this.spreadAsEntity,
                newMod.targets.merge(this.targets)
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
            item.components = Item.Properties.COMPONENT_INTERNER.intern(builder.build());
        }
        ItemColor oldColor = null;
        if (tintGetter.isPresent()) {
            ItemColors itemColors = Minecraft.getInstance().itemColors;
            oldColor = PlatStuff.getItemColor(itemColors, item);
            itemColors.register(tintGetter.get(), item);
        }

        // returns old properties (only vanilla item properties need restoring; client extension data is read live)
        return new ItemModifier(
                Optional.ofNullable(oldColor),
                Optional.empty(),
                Optional.ofNullable(oldRarity),
                List.of(), List.of(), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Targets.EMPTY);
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
        for (TooltipAddition ta : this.tooltips) {
            int position = ta.position();
            Component tooltip = ta.component();
            //insert at position. if <=0 insert first, if >=size insert last
            if (position <= 0) {
                tooltips.addFirst(tooltip);
            } else if (position >= tooltips.size()) {
                tooltips.add(tooltip);
            } else {
                tooltips.add(position, tooltip);
            }
        }
    }

    public boolean shouldAttachToItem() {
        return !tooltips.isEmpty() || !removedTooltips.isEmpty() || barColor.isPresent() || hasClientItemExtensions();
    }

    /** Whether this modifier drives any NeoForge {@code IClientItemExtensions} hook (so it must be wrapped). */
    public boolean hasClientItemExtensions() {
        return wornModel.isPresent() || armorTint.isPresent() || armorTexture.isPresent()
                || armPose.isPresent() || scopeOverlay.isPresent() || bobAsEntity.isPresent() || spreadAsEntity.isPresent();
    }

    @Nullable
    public WornModel getWornModel() {
        return wornModel.orElse(null);
    }

    @Nullable
    public IColorGetter getArmorTint() {
        return armorTint.orElse(null);
    }

    @Nullable
    public ResourceLocation getArmorTexture() {
        return armorTexture.orElse(null);
    }

    @Nullable
    public HumanoidModel.ArmPose getArmPose() {
        return armPose.orElse(null);
    }

    @Nullable
    public ResourceLocation getScopeOverlay() {
        return scopeOverlay.orElse(null);
    }

    @Nullable
    public Boolean getBobAsEntity() {
        return bobAsEntity.orElse(null);
    }

    @Nullable
    public Boolean getSpreadAsEntity() {
        return spreadAsEntity.orElse(null);
    }
}
