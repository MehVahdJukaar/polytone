package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ParticleInitializer(@Nullable BlockContextExpression size,
                                  @Nullable BlockContextExpression lifetime,
                                  @Nullable BlockContextExpression red,
                                  @Nullable BlockContextExpression green,
                                  @Nullable BlockContextExpression blue,
                                  @Nullable BlockContextExpression alpha,
                                  @Nullable BlockContextExpression roll,
                                  @Nullable BlockContextExpression friction,
                                  @Nullable BlockContextExpression custom) {

    public static final Codec<ParticleInitializer> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockContextExpression.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.size)),
            BlockContextExpression.CODEC.optionalFieldOf("lifetime").forGetter(p -> Optional.ofNullable(p.lifetime)),
            BlockContextExpression.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.red)),
            BlockContextExpression.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.green)),
            BlockContextExpression.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.blue)),
            BlockContextExpression.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.alpha)),
            BlockContextExpression.CODEC.optionalFieldOf("roll").forGetter(p -> Optional.ofNullable(p.roll)),
            BlockContextExpression.CODEC.optionalFieldOf("friction").forGetter(p -> Optional.ofNullable(p.friction)),
            BlockContextExpression.CODEC.optionalFieldOf("custom").forGetter(p -> Optional.ofNullable(p.custom))
    ).apply(i, ParticleInitializer::new));

    private ParticleInitializer(Optional<BlockContextExpression> size, Optional<BlockContextExpression> lifetime,
                                Optional<BlockContextExpression> red, Optional<BlockContextExpression> green,
                                Optional<BlockContextExpression> blue, Optional<BlockContextExpression> alpha,
                                Optional<BlockContextExpression> roll,
                                Optional<BlockContextExpression> friction,
                                Optional<BlockContextExpression> custom) {
        this(size.orElse(null), lifetime.orElse(null), red.orElse(null),
                green.orElse(null), blue.orElse(null), alpha.orElse(null),
                roll.orElse(null), friction.orElse(null),
                custom.orElse(null));
    }

    public void initialize(SingleQuadParticle particle, ClientLevel level, BlockState state, BlockPos pos) {
        if (this.roll != null) {
            particle.roll = (float) this.roll.getValue(level, pos, state);
        }
        if (this.size != null) {
            particle.quadSize = ((float) this.size.getValue(level, pos, state));
        }
        if (this.red != null) {
            particle.rCol = (float) this.red.getValue(level, pos, state);
        }
        if (this.green != null) {
            particle.gCol = (float) this.green.getValue(level, pos, state);
        }
        if (this.blue != null) {
            particle.bCol = (float) this.blue.getValue(level, pos, state);
        }
        if (this.alpha != null) {
            particle.alpha = (float) this.alpha.getValue(level, pos, state);
        }
        if (this.lifetime != null) {
            particle.setLifetime((int) Math.max(1, this.lifetime.getValue(level, pos, state)));
        }
        if (this.friction != null) {
            particle.friction = (float) this.friction.getValue(level, pos, state);
        }
        if (this.custom != null && particle instanceof CustomParticleType.Instance ci) {
            ci.custom = this.custom.getValue(level, pos, state);
        }
    }
}
