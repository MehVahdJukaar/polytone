package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static net.mehvahdjukaar.polytone.common.struc.ListUtils.mergeList;

public record EntityModifier(List<EntityParticleEmitter> emitters,
                             Targets targets) {


    public static final Codec<EntityModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            CodecUtils.singleOrList(EntityParticleEmitter.CODEC).fieldOf("emitters").forGetter(em -> em.emitters),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(em -> em.targets)
    ).apply(i, EntityModifier::new));


    public <S extends LivingEntityRenderState> List<ParticleSpawnRecord> gatherParticleSpawns(
            LivingEntityRenderer<?, S, ?> renderer, PoseStack poseStack, S renderState, CameraRenderState cameraState) {

        List<ParticleSpawnRecord> records = new ArrayList<>();
        for (EntityParticleEmitter emitter : emitters) {
            PoseStack spawn = emitter.getSpawnPose(renderer);
            if (spawn != null) {
                Matrix4f cameraToEntityArm = new Matrix4f(poseStack.last().pose());
                cameraToEntityArm.mul(spawn.last().pose());

// Convert from camera space to world space
                Vector3f cameraWorldPos = cameraState.pos.toVector3f();
                Matrix4f worldTransform = new Matrix4f()
                        .translation(cameraWorldPos) // moves from camera space to world space
                        .mul(cameraToEntityArm);     // apply emitter transform in camera space


                records.add(new ParticleSpawnRecord(worldTransform, emitter));
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
