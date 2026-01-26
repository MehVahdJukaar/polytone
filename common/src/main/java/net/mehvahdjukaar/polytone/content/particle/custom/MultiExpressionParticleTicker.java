package net.mehvahdjukaar.polytone.content.particle.custom;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

//TODO: merge this and particle modifier
public record MultiExpressionParticleTicker(@Nullable IParticleExp x,
                                            @Nullable IParticleExp y,
                                            @Nullable IParticleExp z,
                                            @Nullable IParticleExp dx,
                                            @Nullable IParticleExp dy,
                                            @Nullable IParticleExp dz,
                                            @Nullable IParticleExp size,
                                            @Nullable IParticleExp red, @Nullable IParticleExp green,
                                            @Nullable IParticleExp blue, @Nullable IParticleExp alpha,
                                            @Nullable IParticleExp roll,
                                            @Nullable IParticleExp custom,
                                            @Nullable IParticleExp removeIf) implements ICustomParticleTicker {

    public static final Codec<MultiExpressionParticleTicker> CODEC = RecordCodecBuilder.create(i -> i.group(
            IParticleExp.CODEC.optionalFieldOf("x").forGetter(p -> Optional.ofNullable(p.x)),
            IParticleExp.CODEC.optionalFieldOf("y").forGetter(p -> Optional.ofNullable(p.y)),
            IParticleExp.CODEC.optionalFieldOf("z").forGetter(p -> Optional.ofNullable(p.z)),
            IParticleExp.CODEC.optionalFieldOf("dx").forGetter(p -> Optional.ofNullable(p.dx)),
            IParticleExp.CODEC.optionalFieldOf("dy").forGetter(p -> Optional.ofNullable(p.dy)),
            IParticleExp.CODEC.optionalFieldOf("dz").forGetter(p -> Optional.ofNullable(p.dz)),
            IParticleExp.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.size)),
            IParticleExp.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.red)),
            IParticleExp.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.green)),
            IParticleExp.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.blue)),
            IParticleExp.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.alpha)),
            IParticleExp.CODEC.optionalFieldOf("roll").forGetter(p -> Optional.ofNullable(p.roll)),
            IParticleExp.CODEC.optionalFieldOf("custom").forGetter(p -> Optional.ofNullable(p.custom)),
            IParticleExp.CODEC.optionalFieldOf("remove_condition").forGetter(p -> Optional.ofNullable(p.removeIf))
    ).apply(i, MultiExpressionParticleTicker::new));

    private MultiExpressionParticleTicker(Optional<IParticleExp> x, Optional<IParticleExp> y,
                                          Optional<IParticleExp> z, Optional<IParticleExp> dx,
                                          Optional<IParticleExp> dy, Optional<IParticleExp> dz,
                                          Optional<IParticleExp> size, Optional<IParticleExp> red,
                                          Optional<IParticleExp> green, Optional<IParticleExp> blue,
                                          Optional<IParticleExp> alpha, Optional<IParticleExp> roll,
                                          Optional<IParticleExp> custom,
                                          Optional<IParticleExp> removeIf) {
        this(x.orElse(null), y.orElse(null),
                z.orElse(null), dx.orElse(null),
                dy.orElse(null), dz.orElse(null),
                size.orElse(null), red.orElse(null),
                green.orElse(null), blue.orElse(null),
                alpha.orElse(null), roll.orElse(null),
                custom.orElse(null), removeIf.orElse(null)
        );
    }

    public void tick(CustomParticleInstance particle, ClientLevel level) {
        if (this.roll != null) {
            particle.roll = (float) this.roll.evaluate(particle, level);
        }
        particle.roll = 45;
        if (this.size != null) {
            particle.quadSize = (float) this.size.evaluate(particle, level);
        }
        if (this.red != null) {
            particle.rCol = (float) this.red.evaluate(particle, level);
        }
        if (this.green != null) {
            particle.gCol = (float) this.green.evaluate(particle, level);
        }
        if (this.blue != null) {
            particle.bCol = (float) this.blue.evaluate(particle, level);
        }
        if (this.alpha != null) {
            particle.alpha = (float) this.alpha.evaluate(particle, level);
        }
        if (this.x != null) {
            particle.x = this.x.evaluate(particle, level);
        }
        if (this.y != null) {
            particle.y = this.y.evaluate(particle, level);
        }
        if (this.z != null) {
            particle.z = this.z.evaluate(particle, level);
        }
        if (this.dx != null) {
            particle.xd = this.dx.evaluate(particle, level);
        }
        if (this.dy != null) {
            particle.yd = this.dy.evaluate(particle, level);
        }
        if (this.dz != null) {
            particle.zd = this.dz.evaluate(particle, level);
        }
        if (this.custom != null) {
            particle.custom = this.custom.evaluate(particle, level);
        }
        if (this.removeIf != null) {
            if (this.removeIf.evaluate(particle, level) > 0) {
                particle.remove();
            }
        }
    }

}
