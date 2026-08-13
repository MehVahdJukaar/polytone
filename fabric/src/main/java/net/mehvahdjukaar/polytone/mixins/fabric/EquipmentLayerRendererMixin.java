package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.item.ItemModifier;
import net.mehvahdjukaar.polytone.content.model.WornModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Fabric counterpart to NeoForge's IClientItemExtensions#getGenericArmorModel: swaps the worn armor model for
// the item's Polytone worn_model override.
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    @ModifyVariable(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("HEAD"), argsOnly = true)
    private Model<?> polytone$swapWornModel(Model<?> original,
                                            @Local(argsOnly = true) EquipmentClientInfo.LayerType layerType,
                                            @Local(argsOnly = true) ItemStack item) {
        ItemModifier mod = ((IPolytoneItem) item.getItem()).polytone$getModifier();
        if (mod != null) {
            WornModel worn = mod.getWornModel();
            if (worn != null && worn.appliesTo(layerType)) {
                Model<?> baked = worn.getOrBake(Minecraft.getInstance().getEntityModels());
                if (baked != null) {
                    return baked;
                }
            }
        }
        return original;
    }
}
