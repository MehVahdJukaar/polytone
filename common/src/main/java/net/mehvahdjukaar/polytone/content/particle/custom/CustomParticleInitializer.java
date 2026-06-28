package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.exp.impl.BlockContextExpression;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.mixins.accessor.ParticleAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

//TODO: change exp type
public record CustomParticleInitializer(@Nullable IBlockExp size,
                                        @Nullable IBlockExp lifetime,
                                        @Nullable IBlockExp red,
                                        @Nullable IBlockExp green,
                                        @Nullable IBlockExp blue,
                                        @Nullable IBlockExp alpha,
                                        @Nullable IBlockExp roll,
                                        @Nullable IBlockExp friction,
                                        @Nullable IBlockExp hitboxSize,
                                        @Nullable IBlockExp custom) {

    public static final Codec<CustomParticleInitializer> CODEC = RecordCodecBuilder.create(i -> i.group(
            IBlockExp.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.size)),
            IBlockExp.CODEC.optionalFieldOf("lifetime").forGetter(p -> Optional.ofNullable(p.lifetime)),
            IBlockExp.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.red)),
            IBlockExp.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.green)),
            IBlockExp.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.blue)),
            IBlockExp.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.alpha)),
            IBlockExp.CODEC.optionalFieldOf("roll").forGetter(p -> Optional.ofNullable(p.roll)),
            IBlockExp.CODEC.optionalFieldOf("friction").forGetter(p -> Optional.ofNullable(p.friction)),
            IBlockExp.CODEC.optionalFieldOf("hitbox_size").forGetter(p -> Optional.ofNullable(p.hitboxSize)),
            IBlockExp.CODEC.optionalFieldOf("custom").forGetter(p -> Optional.ofNullable(p.custom))
    ).apply(i, CustomParticleInitializer::new));

    private CustomParticleInitializer(Optional<IBlockExp> size, Optional<IBlockExp> lifetime,
                                      Optional<IBlockExp> red, Optional<IBlockExp> green,
                                      Optional<IBlockExp> blue, Optional<IBlockExp> alpha,
                                      Optional<IBlockExp> roll,
                                      Optional<IBlockExp> friction,
                                      Optional<IBlockExp> hitboxSize,
                                      Optional<IBlockExp> custom) {
        this(size.orElse(null), lifetime.orElse(null), red.orElse(null),
                green.orElse(null), blue.orElse(null), alpha.orElse(null),
                roll.orElse(null), friction.orElse(null),
                hitboxSize.orElse(null),
                custom.orElse(null));
    }

    public void initialize(SingleQuadParticle particle, ClientLevel level, BlockState state, BlockPos pos) {
        Vec3 v = Vec3.atCenterOf(pos);
        if (this.roll != null) {
            particle.roll = (float) this.roll.evaluate(level, v, state);
        }
        if (this.size != null) {
            particle.quadSize = ((float) this.size.evaluate(level, v, state));
        }
        if (this.red != null) {
            particle.rCol = (float) this.red.evaluate(level, v, state);
        }
        if (this.green != null) {
            particle.gCol = (float) this.green.evaluate(level, v, state);
        }
        if (this.blue != null) {
            particle.bCol = (float) this.blue.evaluate(level, v, state);
        }
        if (this.alpha != null) {
            particle.alpha = (float) this.alpha.evaluate(level, v, state);
        }
        if (this.lifetime != null) {
            particle.setLifetime((int) Math.max(1, this.lifetime.evaluate(level, v, state)));
        }
        if (this.friction != null) {
            particle.friction = (float) this.friction.evaluate(level, v, state);
        }
        if (this.custom != null && particle instanceof CustomParticleInstance ci) {
            ci.custom = this.custom.evaluate(level, v, state);
        }
        if (this.hitboxSize != null) {
             float hitbox = (float) this.hitboxSize.evaluate(level, v, state);
            ((ParticleAccessor) particle).invokeSetSize(hitbox, hitbox);
        }
    }
}
