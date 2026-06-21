package net.mehvahdjukaar.polytone.content.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.content.model.WornModel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapping {@link IClientItemExtensions} that applies Polytone's {@link ItemModifier} client data and forwards every
 * other call to the extension that was already registered for the item (vanilla {@code DEFAULT} or another mod's).
 * This way we never clobber other mods' extensions: we only take over the specific hooks Polytone actually drives.
 * Hooked in via {@code IClientItemExtensionsMixin#of}, and cached one instance per delegate.
 */
public class PolytoneClientItemExtensions implements IClientItemExtensions {

    private static final Map<IClientItemExtensions, PolytoneClientItemExtensions> CACHE = new ConcurrentHashMap<>();

    private final IClientItemExtensions delegate;

    private PolytoneClientItemExtensions(IClientItemExtensions delegate) {
        this.delegate = delegate;
    }

    /** Returns a Polytone wrapper around the already resolved extension, reusing a single wrapper per delegate. */
    public static PolytoneClientItemExtensions wrap(IClientItemExtensions delegate) {
        return CACHE.computeIfAbsent(delegate, PolytoneClientItemExtensions::new);
    }

    @Nullable
    private static ItemModifier modFor(ItemStack stack) {
        return ((IPolytoneItem) stack.getItem()).polytone$getModifier();
    }

    @Nullable
    private static WornModel wornModelFor(ItemStack stack) {
        ItemModifier mod = modFor(stack);
        return mod == null ? null : mod.getWornModel();
    }

    // --- Polytone driven hooks ---

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                  EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        WornModel worn = wornModelFor(itemStack);
        if (worn != null && worn.appliesTo(equipmentSlot)) {
            HumanoidModel<?> baked = worn.getOrBake(Minecraft.getInstance().getEntityModels());
            if (baked != null) return baked;
        }
        return delegate.getHumanoidArmorModel(livingEntity, itemStack, equipmentSlot, original);
    }

    @Override
    public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                      EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        WornModel worn = wornModelFor(itemStack);
        if (worn != null && worn.appliesTo(equipmentSlot)) {
            // run the interface default so it calls our getHumanoidArmorModel and copies the humanoid pose properties
            return IClientItemExtensions.super.getGenericArmorModel(livingEntity, itemStack, equipmentSlot, original);
        }
        return delegate.getGenericArmorModel(livingEntity, itemStack, equipmentSlot, original);
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getArmPose() != null) return mod.getArmPose();
        return delegate.getArmPose(entity, hand, stack);
    }

    @Override
    public boolean shouldBobAsEntity(ItemStack stack) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getBobAsEntity() != null) return mod.getBobAsEntity();
        return delegate.shouldBobAsEntity(stack);
    }

    @Override
    public boolean shouldSpreadAsEntity(ItemStack stack) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getSpreadAsEntity() != null) return mod.getSpreadAsEntity();
        return delegate.shouldSpreadAsEntity(stack);
    }

    @Override
    public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer,
                                      int layerIdx, int fallbackColor) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getArmorTint() != null) {
            return mod.getArmorTint().getColor(stack, layerIdx);
        }
        return delegate.getArmorLayerTintColor(stack, entity, layer, layerIdx, fallbackColor);
    }

    @Override
    public ResourceLocation getScopeOverlayTexture(ItemStack stack) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getScopeOverlay() != null) return mod.getScopeOverlay();
        return delegate.getScopeOverlayTexture(stack);
    }

    // --- Pure delegation (Polytone logic gets added here as more client extension features land) ---

    @Override
    public @Nullable Font getFont(ItemStack stack, FontContext context) {
        return delegate.getFont(stack, context);
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        return delegate.applyForgeHandTransform(poseStack, player, arm, stack, partialTick, equipProcess, swingProcess);
    }

    @Override
    public void setupModelAnimations(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot,
                                     Model model, float limbSwing, float limbSwingAmount, float partialTick,
                                     float ageInTicks, float netHeadYaw, float headPitch) {
        delegate.setupModelAnimations(livingEntity, itemStack, equipmentSlot, model, limbSwing, limbSwingAmount,
                partialTick, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderHelmetOverlay(ItemStack stack, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        delegate.renderHelmetOverlay(stack, player, guiGraphics, deltaTracker);
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return delegate.getCustomRenderer();
    }

    @Override
    public int getDefaultDyeColor(ItemStack stack) {
        return delegate.getDefaultDyeColor(stack);
    }
}
