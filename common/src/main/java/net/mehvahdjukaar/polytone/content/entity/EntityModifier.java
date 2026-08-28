package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.content.light.ColoredLight;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

import static net.mehvahdjukaar.polytone.utils.Utils.mergeList;

public record EntityModifier(List<EntityParticleEmitter> emitters,
                             Optional<ColoredLight<IEntityExp>> coloredLight,
                             Targets targets) {

    public static final Codec<EntityModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            SchemaCodecs.singleOrList(EntityParticleEmitter.CODEC).optionalFieldOf("emitters", List.of()).forGetter(EntityModifier::emitters),
            ColoredLight.codec(IEntityExp.CODEC, c -> e -> c).optionalFieldOf("colored_light").forGetter(EntityModifier::coloredLight),
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
                other.coloredLight.isPresent() ? other.coloredLight : this.coloredLight,
                this.targets.merge(other.targets)
        );
    }
}
