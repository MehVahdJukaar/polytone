package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.model.EntityModelAnimation;
import net.mehvahdjukaar.polytone.content.model.EntityModelGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EntityParticleModel implements IParticleModel {

    private static final Codec<EntityModelAnimation<IParticleExp>> ANIMATION_CODEC = EntityModelAnimation.codec(IParticleExp.CODEC);

    public static final Codec<EntityParticleModel> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityModelGeometry.CODEC.fieldOf("geometry").forGetter(m -> m.geometry),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(m -> m.texture),
            ANIMATION_CODEC.optionalFieldOf("animation").forGetter(m -> Optional.ofNullable(m.animation))
    ).apply(i, (g, t, a) -> new EntityParticleModel(g, t, a.orElse(null))));

    private final EntityModelGeometry geometry;
    private final ResourceLocation texture;
    private final @Nullable EntityModelAnimation<IParticleExp> animation;

    private ModelPart modelRoot;
    private ModelPart[] animatedBones;
    private boolean failed = false;

    public EntityParticleModel(EntityModelGeometry geometry, ResourceLocation texture, @Nullable EntityModelAnimation<IParticleExp> animation) {
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
    public void render(CustomParticleInstance particle, PoseStack poseStack, int light, float r, float g, float b, float a, float partialTicks) {
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
        if (animation != null){
            animation.apply(animatedBones, particle.modelPose, partialTicks);
        }

        //entity models are y down
        poseStack.scale(-1, -1, 1);
        VertexConsumer consumer = PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(particle.type.renderType.getEntity(texture));
        modelRoot.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.colorFromFloat(a, r, g, b));
    }
}
