package net.mehvahdjukaar.polytone.content.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.content.model.WornModel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Applies an item modifier's client data and forwards everything else to the extension already
// registered for the item (vanilla DEFAULT or another mod's), so we only take over the hooks we
// actually drive. Hooked in from IClientItemExtensionsMixin#of, one wrapper cached per delegate.
public class PolytoneClientItemExtensions implements IClientItemExtensions {

    private static final Map<IClientItemExtensions, PolytoneClientItemExtensions> CACHE = new ConcurrentHashMap<>();

    private final IClientItemExtensions delegate;

    private PolytoneClientItemExtensions(IClientItemExtensions delegate) {
        this.delegate = delegate;
    }

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

    @Override
    public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        WornModel worn = wornModelFor(stack);
        if (worn != null && worn.appliesTo(layerType)) {
            Model baked = worn.getOrBake(Minecraft.getInstance().getEntityModels());
            if (baked != null) return baked;
        }
        return delegate.getHumanoidArmorModel(stack, layerType, original);
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        WornModel worn = wornModelFor(stack);
        if (worn != null && worn.appliesTo(layerType)) {
            // run the interface default so it calls our getHumanoidArmorModel and copies the humanoid pose properties
            return IClientItemExtensions.super.getGenericArmorModel(stack, layerType, original);
        }
        return delegate.getGenericArmorModel(stack, layerType, original);
    }

    @Override
    public @Nullable Font getFont(ItemStack stack, FontContext context) {
        return delegate.getFont(stack, context);
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getArmPose() != null) return mod.getArmPose();
        return delegate.getArmPose(entity, hand, stack);
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        return delegate.applyForgeHandTransform(poseStack, player, arm, stack, partialTick, equipProcess, swingProcess);
    }

    @Override
    public void setupModelAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot, Model model,
                                     float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                     float netHeadYaw, float headPitch) {
        delegate.setupModelAnimations(entity, stack, slot, model, limbSwing, limbSwingAmount, partialTick, ageInTicks,
                netHeadYaw, headPitch);
    }

    @Override
    public void renderFirstPersonOverlay(ItemStack stack, EquipmentSlot slot, Player player, GuiGraphicsExtractor graphics,
                                         DeltaTracker deltaTracker) {
        delegate.renderFirstPersonOverlay(stack, slot, player, graphics, deltaTracker);
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
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getArmorTint() != null) {
            return mod.getArmorTint().getItemColor(stack, layerIdx);
        }
        return delegate.getArmorLayerTintColor(stack, layer, layerIdx, fallbackColor);
    }

    @Override
    public int getDefaultDyeColor(ItemStack stack) {
        return delegate.getDefaultDyeColor(stack);
    }

    @Override
    public @Nullable Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type,
                                                EquipmentClientInfo.Layer layer, Identifier fallback) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getArmorTexture() != null) return mod.getArmorTexture();
        return delegate.getArmorTexture(stack, type, layer, fallback);
    }

    @Override
    public Identifier getScopeOverlayTexture(ItemStack stack) {
        ItemModifier mod = modFor(stack);
        if (mod != null && mod.getScopeOverlay() != null) return mod.getScopeOverlay();
        return delegate.getScopeOverlayTexture(stack);
    }
}
