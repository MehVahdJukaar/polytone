package net.mehvahdjukaar.polytone.content.particle.custom;


import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.ParticleExpEnv;
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

    public static final SchemaCodec<MultiExpressionParticleTicker> CODEC = SchemaRecord.create(MultiExpressionParticleTicker.class, i -> i.group(
            i.optional("x", IParticleExp.CODEC, p -> Optional.ofNullable(p.x)),
            i.optional("y", IParticleExp.CODEC, p -> Optional.ofNullable(p.y)),
            i.optional("z", IParticleExp.CODEC, p -> Optional.ofNullable(p.z)),
            i.optional("dx", IParticleExp.CODEC, p -> Optional.ofNullable(p.dx)),
            i.optional("dy", IParticleExp.CODEC, p -> Optional.ofNullable(p.dy)),
            i.optional("dz", IParticleExp.CODEC, p -> Optional.ofNullable(p.dz)),
            i.optional("size", IParticleExp.CODEC, p -> Optional.ofNullable(p.size)),
            i.optional("red", IParticleExp.CODEC, p -> Optional.ofNullable(p.red)),
            i.optional("green", IParticleExp.CODEC, p -> Optional.ofNullable(p.green)),
            i.optional("blue", IParticleExp.CODEC, p -> Optional.ofNullable(p.blue)),
            i.optional("alpha", IParticleExp.CODEC, p -> Optional.ofNullable(p.alpha)),
            i.optional("roll", IParticleExp.CODEC, p -> Optional.ofNullable(p.roll)),
            i.optional("custom", IParticleExp.CODEC, p -> Optional.ofNullable(p.custom)),
            i.optional("remove_condition", IParticleExp.CODEC, p -> Optional.ofNullable(p.removeIf))
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
        // one reusable per-thread var environment for all field expressions of this tick
        ParticleExpEnv env = ParticleExpEnv.get();
        if (this.roll != null) {
            particle.roll = (float) this.roll.evaluate(particle, level, env);
        }
        if (this.size != null) {
            particle.quadSize = (float) this.size.evaluate(particle, level, env);
        }
        if (this.red != null) {
            particle.rCol = (float) this.red.evaluate(particle, level, env);
        }
        if (this.green != null) {
            particle.gCol = (float) this.green.evaluate(particle, level, env);
        }
        if (this.blue != null) {
            particle.bCol = (float) this.blue.evaluate(particle, level, env);
        }
        if (this.alpha != null) {
            particle.alpha = (float) this.alpha.evaluate(particle, level, env);
        }
        if (this.x != null) {
            particle.x = this.x.evaluate(particle, level, env);
        }
        if (this.y != null) {
            particle.y = this.y.evaluate(particle, level, env);
        }
        if (this.z != null) {
            particle.z = this.z.evaluate(particle, level, env);
        }
        if (this.dx != null) {
            particle.xd = this.dx.evaluate(particle, level, env);
        }
        if (this.dy != null) {
            particle.yd = this.dy.evaluate(particle, level, env);
        }
        if (this.dz != null) {
            particle.zd = this.dz.evaluate(particle, level, env);
        }
        if (this.custom != null) {
            particle.custom = this.custom.evaluate(particle, level, env);
        }
        if (this.removeIf != null) {
            if (this.removeIf.evaluate(particle, level, env) > 0) {
                particle.remove();
            }
        }
    }

}
