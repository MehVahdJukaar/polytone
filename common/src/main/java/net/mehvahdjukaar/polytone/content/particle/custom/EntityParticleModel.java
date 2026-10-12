package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.model.EntityModelAnimation;
import net.mehvahdjukaar.polytone.content.model.EntityModelGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EntityParticleModel implements IParticleModel {

    private static final Codec<EntityModelAnimation<IParticleExp>> ANIMATION_CODEC = EntityModelAnimation.codec(IParticleExp.CODEC);

    public static final Codec<EntityParticleModel> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityModelGeometry.CODEC.fieldOf("geometry").forGetter(m -> m.geometry),
            Identifier.CODEC.fieldOf("texture").forGetter(m -> m.texture),
            ANIMATION_CODEC.optionalFieldOf("animation").forGetter(m -> Optional.ofNullable(m.animation))
    ).apply(i, (g, t, a) -> new EntityParticleModel(g, t, a.orElse(null))));

    private final EntityModelGeometry geometry;
    private final Identifier texture;
    private final @Nullable EntityModelAnimation<IParticleExp> animation;

    private ModelPart modelRoot;
    private ModelPart[] animatedBones;
    private boolean failed = false;

    public EntityParticleModel(EntityModelGeometry geometry, Identifier texture, @Nullable EntityModelAnimation<IParticleExp> animation) {
        this.geometry = geometry;
        this.texture = texture;
        this.animation = animation;
    }

    @Override
    public void tick(CustomParticleInstance particle, ClientLevel level) {
        if (animation != null) {
            particle.modelPose = animation.tick(particle.modelPose, e -> e.evaluate(particle, level));
        }
    }

    @Override
    public void submit(CustomParticleInstance particle, SubmitNodeCollector collector, PoseStack poseStack, int light, int color, float partialTicks) {
        if (failed) return;
        if (modelRoot == null) {
            try {
                //lazy computes
                modelRoot = geometry.bake(Minecraft.getInstance().getEntityModels());
            } catch (Exception e) {
                Polytone.LOGGER.error("Failed to bake custom particle entity model", e);
                failed = true;
                return;
            }
            if (animation != null) {
                animatedBones = animation.findBones(modelRoot);
            }
        }

        //entity models are y down
        poseStack.scale(-1, -1, 1);
        collector.submitCustomGeometry(poseStack, particle.type.renderType.getEntity(texture), (pose, consumer) -> {
            if (animation != null) {
                animation.apply(animatedBones, particle.modelPose, partialTicks);
            }
            PoseStack partStack = new PoseStack();
            partStack.last().set(pose);
            modelRoot.render(partStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        });
    }
}
