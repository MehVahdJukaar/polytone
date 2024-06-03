package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public class CustomParticleType implements ParticleFactory {

    private final RenderType renderType;
    private final @Nullable Initializer initializer;
    private final @Nullable Ticker ticker;
    private transient SpriteSet spriteSet;

    public CustomParticleType(RenderType renderType, @Nullable Initializer initializer, @Nullable Ticker ticker) {
        this.renderType = renderType;
        this.initializer = initializer;
        this.ticker = ticker;
    }

    public static final Codec<CustomParticleType> CODEC = RecordCodecBuilder.create(i -> i.group(
            RenderType.CODEC.optionalFieldOf("render_type", RenderType.OPAQUE)
                    .forGetter(CustomParticleType::getRenderType),
            Initializer.CODEC.optionalFieldOf("initializer").forGetter(c -> Optional.ofNullable(c.initializer)),
            Ticker.CODEC.optionalFieldOf("ticker").forGetter(c -> Optional.ofNullable(c.ticker))
    ).apply(i, CustomParticleType::new));

    public CustomParticleType(RenderType renderType, Optional<Initializer> initializer,
                              Optional<Ticker> ticker) {
        this(renderType, initializer.orElse(null), ticker.orElse(null));
    }

    public RenderType getRenderType() {
        return renderType;
    }

    @Override
    public void createParticle(ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                               @Nullable BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera.getPosition().distanceToSqr(x, y, z) < 1024.0) {
            if (spriteSet != null) {
                Particle particle = new Instance(world, x, y, z, xSpeed, ySpeed, zSpeed, state, this);
                mc.particleEngine.add(particle);

            } else {
                throw new IllegalStateException("Sprite set not set for custom particle type");
            }
        }
    }

    public void setSpriteSet(ParticleEngine.MutableSpriteSet mutableSpriteSet) {
        this.spriteSet = mutableSpriteSet;
    }

    public static class Instance extends TextureSheetParticle {

        private final ParticleRenderType renderType;
        private final @Nullable Ticker ticker;
        private final SpriteSet spriteSet;
        private float oQuadSize;

        protected Instance(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                           @Nullable BlockState state, CustomParticleType type) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            this.renderType = type.renderType.get();
            this.ticker = type.ticker;
            this.spriteSet = type.spriteSet;
            Initializer initializer = type.initializer;
            if (initializer != null) {
                BlockPos pos = BlockPos.containing(x, y, z);
                if (initializer.roll != null) {
                    this.roll = (float) initializer.roll.getValue(level, pos, state);
                }
                if (initializer.size != null) {
                    this.scale((float) initializer.size.getValue(level, pos, state));
                }
                if (initializer.red != null) {
                    this.rCol = (float) initializer.red.getValue(level, pos, state);
                }
                if (initializer.green != null) {
                    this.gCol = (float) initializer.green.getValue(level, pos, state);
                }
                if (initializer.blue != null) {
                    this.bCol = (float) initializer.blue.getValue(level, pos, state);
                }
                if (initializer.alpha != null) {
                    this.alpha = (float) initializer.alpha.getValue(level, pos, state);
                }
                if (initializer.lifetime != null) {
                    this.lifetime = (int) initializer.lifetime.getValue(level, pos, state);
                }
                if (initializer.friction != null) {
                    this.friction = (float) initializer.friction.getValue(level, pos, state);
                }
                this.hasPhysics = initializer.hasPhysics;
            }
            this.setSpriteFromAge(spriteSet);
        }

        @Override
        public void tick() {
            this.setSpriteFromAge(spriteSet);
            super.tick();
            if (this.ticker != null) {
                if (this.ticker.roll != null) {
                    this.oRoll = this.roll;
                    this.roll = (float) this.ticker.roll.get(this, level);
                }
                if (this.ticker.size != null) {
                    this.oQuadSize = this.quadSize;
                    this.quadSize = (float) this.ticker.size.get(this, level);
                }
                if (this.ticker.red != null) {
                    this.rCol = (float) this.ticker.red.get(this, level);
                }
                if (this.ticker.green != null) {
                    this.gCol = (float) this.ticker.green.get(this, level);
                }
                if (this.ticker.blue != null) {
                    this.bCol = (float) this.ticker.blue.get(this, level);
                }
                if (this.ticker.alpha != null) {
                    this.alpha = (float) this.ticker.alpha.get(this, level);
                }
                if (this.ticker.x != null) {
                    this.x = this.ticker.x.get(this, level);
                }
                if (this.ticker.y != null) {
                    this.y = this.ticker.y.get(this, level);
                }
                if (this.ticker.z != null) {
                    this.z = this.ticker.z.get(this, level);
                }
                if (this.ticker.dx != null) {
                    this.xd = this.ticker.dx.get(this, level);
                }
                if (this.ticker.dy != null) {
                    this.yd = this.ticker.dy.get(this, level);
                }
                if (this.ticker.dz != null) {
                    this.zd = this.ticker.dz.get(this, level);
                }
            }
        }

        @Override
        public float getQuadSize(float scaleFactor) {
            return Mth.lerp(scaleFactor, this.oQuadSize, this.quadSize);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return renderType;
        }
    }

    private enum RenderType {
        TERRAIN,
        OPAQUE,
        TRANSLUCENT,
        CUSTOM;

        public static final Codec<RenderType> CODEC = Codec.STRING.xmap(
                a -> valueOf(a.toUpperCase()), e -> e.name().toLowerCase(Locale.ROOT)
        );

        public ParticleRenderType get() {
            return switch (this) {
                case TERRAIN -> ParticleRenderType.TERRAIN_SHEET;
                default -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
                case TRANSLUCENT -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
                case CUSTOM -> ParticleRenderType.CUSTOM;
            };
        }
    }

    private record Ticker(@Nullable ParticleExpression x, @Nullable ParticleExpression y,
                          @Nullable ParticleExpression z,
                          @Nullable ParticleExpression dx, @Nullable ParticleExpression dy,
                          @Nullable ParticleExpression dz,
                          @Nullable ParticleExpression size,
                          @Nullable ParticleExpression red, @Nullable ParticleExpression green,
                          @Nullable ParticleExpression blue, @Nullable ParticleExpression alpha,
                          @Nullable ParticleExpression roll) {

        private static final Codec<Ticker> CODEC = RecordCodecBuilder.create(i -> i.group(
                ParticleExpression.CODEC.optionalFieldOf("x").forGetter(p -> Optional.ofNullable(p.x)),
                ParticleExpression.CODEC.optionalFieldOf("y").forGetter(p -> Optional.ofNullable(p.y)),
                ParticleExpression.CODEC.optionalFieldOf("z").forGetter(p -> Optional.ofNullable(p.z)),
                ParticleExpression.CODEC.optionalFieldOf("dx").forGetter(p -> Optional.ofNullable(p.dx)),
                ParticleExpression.CODEC.optionalFieldOf("dy").forGetter(p -> Optional.ofNullable(p.dy)),
                ParticleExpression.CODEC.optionalFieldOf("dz").forGetter(p -> Optional.ofNullable(p.dz)),
                ParticleExpression.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.size)),
                ParticleExpression.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.red)),
                ParticleExpression.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.green)),
                ParticleExpression.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.blue)),
                ParticleExpression.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.alpha)),
                ParticleExpression.CODEC.optionalFieldOf("roll").forGetter(p -> Optional.ofNullable(p.alpha))
        ).apply(i, Ticker::new));

        private Ticker(Optional<ParticleExpression> x, Optional<ParticleExpression> y,
                       Optional<ParticleExpression> z, Optional<ParticleExpression> dx,
                       Optional<ParticleExpression> dy, Optional<ParticleExpression> dz,
                       Optional<ParticleExpression> size, Optional<ParticleExpression> red,
                       Optional<ParticleExpression> green, Optional<ParticleExpression> blue,
                       Optional<ParticleExpression> alpha, Optional<ParticleExpression> roll) {
            this(x.orElse(null), y.orElse(null),
                    z.orElse(null), dx.orElse(null),
                    dy.orElse(null), dz.orElse(null),
                    size.orElse(null), red.orElse(null),
                    green.orElse(null), blue.orElse(null),
                    alpha.orElse(null), roll.orElse(null));
        }
    }

    private record Initializer(@Nullable BlockParticleExpression size,
                               @Nullable BlockParticleExpression lifetime,
                               @Nullable BlockParticleExpression red,
                               @Nullable BlockParticleExpression green,
                               @Nullable BlockParticleExpression blue,
                               @Nullable BlockParticleExpression alpha,
                               @Nullable BlockParticleExpression roll,
                               @Nullable BlockParticleExpression friction,
                               boolean hasPhysics) {

        private static final Codec<Initializer> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockParticleExpression.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.size)),
                BlockParticleExpression.CODEC.optionalFieldOf("lifetime").forGetter(p -> Optional.ofNullable(p.lifetime)),
                BlockParticleExpression.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.red)),
                BlockParticleExpression.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.green)),
                BlockParticleExpression.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.blue)),
                BlockParticleExpression.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.alpha)),
                BlockParticleExpression.CODEC.optionalFieldOf("roll").forGetter(p -> Optional.ofNullable(p.roll)),
                BlockParticleExpression.CODEC.optionalFieldOf("friction").forGetter(p -> Optional.ofNullable(p.friction)),
                Codec.BOOL.optionalFieldOf("has_physics", true).forGetter(p -> p.hasPhysics)
        ).apply(i, Initializer::new));

        private Initializer(Optional<BlockParticleExpression> size, Optional<BlockParticleExpression> lifetime,
                            Optional<BlockParticleExpression> red, Optional<BlockParticleExpression> green,
                            Optional<BlockParticleExpression> blue, Optional<BlockParticleExpression> alpha,
                            Optional<BlockParticleExpression> roll,
                            Optional<BlockParticleExpression> friction, boolean hasPhysics) {
            this(size.orElse(null), lifetime.orElse(null), red.orElse(null),
                    green.orElse(null), blue.orElse(null), alpha.orElse(null),
                    roll.orElse(null), friction.orElse(null), hasPhysics);
        }
    }
}

