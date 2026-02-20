package net.mehvahdjukaar.polytone.compat;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import traben.entity_model_features.EMFManager;
import traben.entity_model_features.models.animation.state.EMFEntityRenderState;

import java.util.UUID;

public class EmfCompat {

    public static int getLastKnownTextureVariantIndex(EntityRenderState renderState) {
        UUID uuid = ((EMFEntityRenderState) renderState).uuid();
        return EMFManager.getInstance().lastModelSuffixOfEntity.getInt(uuid);
    }
}