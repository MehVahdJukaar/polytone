package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class BakedParticleModel implements IParticleModel {

    public static final Codec<BakedParticleModel> CODEC = ResourceLocation.CODEC.xmap(BakedParticleModel::new, m -> m.id);

    private final ResourceLocation id;
    private boolean missing = false;

    public BakedParticleModel(ResourceLocation id) {
        this.id = id;
    }

    public ModelResourceLocation getModelLocation() {
        return new ModelResourceLocation(id, "standalone");
    }

    @Override
    public void render(CustomParticleInstance particle, PoseStack poseStack, int light, float r, float g, float b, float a, float partialTicks) {
        if (missing) return;
        BakedModel model = PlatStuff.getBakedModel(getModelLocation());
        if (model == null) {
            Polytone.LOGGER.error("Failed to load custom particle model: {}. Maybe model was missing", id);
            missing = true;
            return;
        }
        VertexConsumer consumer = PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(particle.type.renderType.getBlock());
        poseStack.translate(-0.5, -0.5, -0.5);
        CustomParticleInstance.putModelBulkData(model, light, OverlayTexture.NO_OVERLAY, poseStack, consumer, r, g, b, a);
    }
}
