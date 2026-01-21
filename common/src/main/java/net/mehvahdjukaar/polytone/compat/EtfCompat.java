package net.mehvahdjukaar.polytone.compat;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import traben.entity_texture_features.features.ETFManager;
import traben.entity_texture_features.features.state.ETFEntityRenderState;

import java.util.UUID;

public class EtfCompat {

    public static int getLastKnownTextureVariantIndex(LivingEntityRenderState renderState) {
        UUID uuid = ((ETFEntityRenderState) renderState).uuid();
        return ETFManager.getInstance().LAST_SUFFIX_OF_ENTITY.getInt(uuid);
    }
}
