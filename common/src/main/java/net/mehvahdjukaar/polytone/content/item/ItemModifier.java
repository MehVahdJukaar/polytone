package net.mehvahdjukaar.polytone.content.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.model.WornModel;
import net.mehvahdjukaar.polytone.common.Targets;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static net.mehvahdjukaar.polytone.common.struc.ListUtils.mergeList;

public record ItemModifier(Optional<IColorGetter> barColor,
                           Optional<Rarity> rarity,
                           List<TooltipAddition> tooltips,
                           List<Pattern> removedTooltips,
                           //List<ItemModelOverride> customModels,
                           Optional<WornModel> wornModel,
                           // NeoForge IClientItemExtensions backed (no effect on Fabric, which only reads wornModel)
                           Optional<IColorGetter> armorTint,
                           Optional<Identifier> armorTexture,
                           Optional<HumanoidModel.ArmPose> armPose,
                           Optional<Identifier> scopeOverlay,
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

    public static final SchemaCodec<ItemModifier> CODEC = SchemaRecord.create(ItemModifier.class, i -> i.group(
            //TODO: register custom item model color sampler that takes in a colormap
            //IndexCompoundColorGetter.SINGLE_OR_MULTIPLE.optionalFieldOf("colormap").forGetter(b -> (Optional<IColorGetter>) b.tintGetter),
            i.optional("bar_color", Colormap.CODEC, ItemModifier::barColor),
            i.optional("rarity", Rarity.CODEC, ItemModifier::rarity),
            i.optional("tooltips", TooltipAddition.CODEC.listOf(), List.of(), ItemModifier::tooltips),
            i.optional("removed_tooltips", ExtraCodecs.PATTERN.listOf(), List.of(), ItemModifier::removedTooltips),
            //ItemModelOverride.CODEC.listOf().optionalFieldOf("custom_models", List.of()).forGetter(ItemModifier::customModels),
            i.optional("worn_model", WornModel.CODEC, ItemModifier::wornModel),
            i.optional("armor_tint", Colormap.CODEC, ItemModifier::armorTint),
            i.optional("armor_texture", Identifier.CODEC, ItemModifier::armorTexture),
            i.optional("arm_pose", ARM_POSE_CODEC, ItemModifier::armPose),
            i.optional("scope_overlay", Identifier.CODEC, ItemModifier::scopeOverlay),
            i.optional("bob_as_entity", Codec.BOOL, ItemModifier::bobAsEntity),
            i.optional("spread_as_entity", Codec.BOOL, ItemModifier::spreadAsEntity),
            i.optional("targets", Targets.CODEC, Targets.EMPTY, ItemModifier::targets)
    ).apply(i, ItemModifier::new));

    /*
    public record Partial(List<ItemModelOverride.Partial> customModels) {
    }

    public static final Codec<Partial> CODEC_ONLY_MODELS = RecordCodecBuilder.create(instance -> instance.group(
            ItemModelOverride.CODEC_MODEL_ONLY.listOf().optionalFieldOf("custom_models", List.of()).forGetter(Partial::customModels)
    ).apply(instance, Partial::new));
*/

    public static ItemModifier ofBarColor(Colormap colormap) {
        return new ItemModifier(Optional.of(colormap),
                Optional.empty(),
             //   List.of(),
                List.of(), List.of(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    public ItemModifier merge(ItemModifier newMod) {
        return new ItemModifier(
                newMod.barColor.isPresent() ? newMod.barColor : this.barColor,
                newMod.rarity.isPresent() ? newMod.rarity : this.rarity,
                mergeList(newMod.tooltips, this.tooltips),
                mergeList(newMod.removedTooltips, this.removedTooltips),
              //  mergeList(newMod.customModels, this.customModels),
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
            item.components = builder.build();
        }

        // returns old properties (only vanilla item properties need restoring; client extension data is read live)
        return new ItemModifier(
                Optional.empty(),
                Optional.ofNullable(oldRarity),
           //     List.of(),
                List.of(), List.of(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Targets.EMPTY);
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
    public Identifier getArmorTexture() {
        return armorTexture.orElse(null);
    }

    @Nullable
    public HumanoidModel.ArmPose getArmPose() {
        return armPose.orElse(null);
    }

    @Nullable
    public Identifier getScopeOverlay() {
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
