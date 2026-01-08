package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.Targets;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static net.mehvahdjukaar.polytone.common.struc.ListUtils.mergeList;

public record EntityModifier(List<EntityParticleEmitter> emitters,
                             Targets targets) {


    public static final Codec<EntityModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityParticleEmitter.CODEC.listOf().fieldOf("emitters").forGetter(em -> em.emitters),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(em -> em.targets)
    ).apply(i, EntityModifier::new));


    public <S extends LivingEntityRenderState> List<ParticleSpawnRecord> gatherParticleSpawns(
            LivingEntityRenderer<?, S, ?> renderer, PoseStack poseStack, S renderState) {

        List<ParticleSpawnRecord> records = new ArrayList<>();
        for (EntityParticleEmitter emitter : emitters) {
            PoseStack spawn = emitter.getSpawnPose(renderer);
            if (spawn != null) {
                Matrix4f mat = new Matrix4f();
                mat.translate((float) renderState.x, (float) renderState.y, (float) renderState.z);
                mat.mul(spawn.last().pose());
                mat.mul(poseStack.last().pose()); //TODO: optimize. add distance checks
                records.add(new ParticleSpawnRecord(new Vec3(mat.getTranslation(new Vector3f())), emitter));
            }
        }
        return records;
    }

    public EntityModifier merge(EntityModifier entityModifier) {
        return new EntityModifier(
                mergeList(this.emitters, entityModifier.emitters),
                this.targets.merge(entityModifier.targets)
        );
    }
}
