package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.SpecialModelsHandler;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;

public class BakedParticleModel implements IParticleModel {

    public static final Codec<BakedParticleModel> CODEC = Identifier.CODEC.xmap(BakedParticleModel::new, m -> m.id);

    private final Identifier id;
    private boolean missing = false;

    public BakedParticleModel(Identifier id) {
        this.id = id;
    }

    public Identifier getModelLocation() {
        return id;
    }

    @Override
    public void submit(CustomParticleInstance particle, SubmitNodeCollector collector, PoseStack poseStack, int light, int color, float partialTicks) {
        if (missing) return;
        QuadCollection model = SpecialModelsHandler.getSpecialModel(id);
        if (model == null) {
            Polytone.LOGGER.error("Failed to load custom particle model: {}. Maybe model was missing", id);
            missing = true;
            return;
        }
        poseStack.translate(-0.5, -0.5, -0.5);
        QuadInstance qi = new QuadInstance();
        qi.setColor(color);
        qi.setLightCoords(light);
        qi.setOverlayCoords(OverlayTexture.NO_OVERLAY);
        collector.submitCustomGeometry(poseStack, particle.type.renderType.getBlock(), (pose, consumer) -> {
            for (BakedQuad quad : model.getAll()) {
                consumer.putBakedQuad(pose, quad, qi);
            }
        });
    }
}
