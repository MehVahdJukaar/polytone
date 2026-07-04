package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.world.entity.Entity;

import java.util.List;

import static net.mehvahdjukaar.polytone.utils.Utils.mergeList;

public record EntityModifier(List<EntityParticleEmitter> emitters, Targets targets) {

    public static final Codec<EntityModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityParticleEmitter.CODEC.listOf().fieldOf("emitters").forGetter(EntityModifier::emitters),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(EntityModifier::targets)
    ).apply(i, EntityModifier::new));

    public void tick(Entity entity) {
        for (EntityParticleEmitter emitter : emitters) {
            emitter.tick(entity);
        }
    }

    public EntityModifier merge(EntityModifier other) {
        return new EntityModifier(
                mergeList(this.emitters, other.emitters),
                this.targets.merge(other.targets)
        );
    }
}
