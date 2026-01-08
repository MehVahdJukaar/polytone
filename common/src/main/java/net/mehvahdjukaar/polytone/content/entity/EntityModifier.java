package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.Targets;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class EntityModifier {


    public static final Codec<EntityModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityParticleEmitter.CODEC.listOf().fieldOf("emitters").forGetter(em -> em.emitters),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(em -> em.targets)
    ).apply(i, EntityModifier::new));

    private final Targets targets;
    private final List<EntityParticleEmitter> emitters;

    public EntityModifier(List<EntityParticleEmitter> emitters, Targets targets) {
        this.emitters = emitters;
        this.targets = targets;
    }

    public void runTickers(Entity entity) {
        for (EntityParticleEmitter emitter : emitters) {
            emitter.trySpawnParticles(entity);
        }
    }
}
