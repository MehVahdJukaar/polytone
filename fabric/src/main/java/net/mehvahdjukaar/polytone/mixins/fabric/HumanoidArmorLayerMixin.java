package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.item.ItemModifier;
import net.mehvahdjukaar.polytone.content.model.WornModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//rip fabric api
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @ModifyVariable(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private HumanoidModel<?> polytone$swapWornModel(HumanoidModel<?> original,
                                                    @Local(argsOnly = true) LivingEntity livingEntity,
                                                    @Local(argsOnly = true) EquipmentSlot slot) {
        ItemStack stack = livingEntity.getItemBySlot(slot);
        ItemModifier mod = ((IPolytoneItem) stack.getItem()).polytone$getModifier();
        if (mod != null) {
            WornModel worn = mod.getWornModel();
            if (worn != null && worn.appliesTo(slot)) {
                HumanoidModel<?> baked = worn.getOrBake(Minecraft.getInstance().getEntityModels());
                if (baked != null) {
                    return baked;
                }
            }
        }
        return original;
    }
}
