package net.mehvahdjukaar.polytone.content.particle.custom;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
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

    public static final SchemaCodec<CustomParticleInitializer> CODEC = SchemaRecord.create(CustomParticleInitializer.class, i -> i.group(
            i.optional("size", IBlockExp.CODEC, p -> Optional.ofNullable(p.size)),
            i.optional("lifetime", IBlockExp.CODEC, p -> Optional.ofNullable(p.lifetime)),
            i.optional("red", IBlockExp.CODEC, p -> Optional.ofNullable(p.red)),
            i.optional("green", IBlockExp.CODEC, p -> Optional.ofNullable(p.green)),
            i.optional("blue", IBlockExp.CODEC, p -> Optional.ofNullable(p.blue)),
            i.optional("alpha", IBlockExp.CODEC, p -> Optional.ofNullable(p.alpha)),
            i.optional("roll", IBlockExp.CODEC, p -> Optional.ofNullable(p.roll)),
            i.optional("friction", IBlockExp.CODEC, p -> Optional.ofNullable(p.friction)),
            i.optional("hitbox_size", IBlockExp.CODEC, p -> Optional.ofNullable(p.hitboxSize)),
            i.optional("custom", IBlockExp.CODEC, p -> Optional.ofNullable(p.custom))
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
        Vec3 v = pos.getCenter();
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
