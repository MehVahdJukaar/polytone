package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.SpecialModelsHandler;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.codec.BiggerCodecs;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.common.exp.impl.ParticleContextExpression;
import net.mehvahdjukaar.polytone.content.particle.ParticleParticleEmitter;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderState;
import net.mehvahdjukaar.polytone.content.sound.ParticleSoundEmitter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomParticleType implements CustomParticleFactory {

    private static BlockState STATE_HACK = Blocks.AIR.defaultBlockState();

    private final ParticleRenderMode renderType;
    private final @Nullable Identifier model;
    private final @Nullable ParticleInitializer initializer;
    private final @Nullable Ticker ticker;
    private final List<ParticleSoundEmitter> sounds;
    private final int tickRate;
    private final int exclusionRadius;
    protected final List<ParticleParticleEmitter> particles = new ArrayList<>();
    @Nullable
    protected List<Dynamic<?>> lazyParticles;

    private final int lightLevel;
    private final LiquidAffinity liquidAffinity;
    private final boolean hasPhysics;
    private final boolean killOnContact;
    private final boolean killWhenStill;
    private final @Nullable IColorGetter colormap;
    private final RotationProvider rotationProvider;
    private final Vec3 offset;
    private final Optional<ParticleLimit> particleGroupLimit;
    private final boolean forceSpawn;
    private final boolean randomSprite;

    private transient SpriteSet spriteSet;

    private boolean isValid = true;

    private CustomParticleType(ParticleRenderMode renderType, RotationProvider rotationProvider,
                               @Nullable Identifier model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact, boolean killWhenStill,
                               LiquidAffinity liquidAffinity, @Nullable IColorGetter colormap,
                               boolean randomSprite,
                               int particleGroupLimit, boolean forceSpawn,
                               @Nullable ParticleInitializer initializer, @Nullable Ticker ticker,
                               @Nullable List<ParticleSoundEmitter> sounds,
                               int tickRate, @Nullable List<Dynamic<?>> particles, int killSimilarInRadius) {
        this.renderType = renderType;
        this.randomSprite = randomSprite;
        this.model = model;
        this.initializer = initializer;
        this.ticker = ticker;
        this.sounds = sounds;
        this.lazyParticles = particles;
        this.lightLevel = light;
        this.hasPhysics = hasPhysics;
        this.killOnContact = killOnContact;
        this.killWhenStill = killWhenStill;
        this.liquidAffinity = liquidAffinity;
        this.forceSpawn = forceSpawn;
        this.colormap = colormap;
        this.offset = offset;
        this.rotationProvider = rotationProvider;
        this.tickRate = tickRate;
        this.exclusionRadius = killSimilarInRadius;
        this.particleGroupLimit = particleGroupLimit > 0 ? Optional.of(new ParticleLimit(particleGroupLimit)) : Optional.empty();
    }

    public static final Codec<CustomParticleType> CODEC = RecordCodecBuilder.create(i -> BiggerCodecs.group(i,
            ParticleRenderMode.CODEC.optionalFieldOf("render_type", ParticleRenderMode.OPAQUE)
                    .forGetter(CustomParticleType::getRenderType),
            RotationProvider.CODEC.optionalFieldOf("rotation_mode", RotationMode.LOOK_AT_XYZ).forGetter(c -> c.rotationProvider),
            Identifier.CODEC.optionalFieldOf("model").forGetter(c -> Optional.ofNullable(c.model)),
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(c -> c.offset),
            Codec.intRange(0, 15).optionalFieldOf("light_level", 0).forGetter(c -> c.lightLevel),
            Codec.BOOL.optionalFieldOf("has_physics", true).forGetter(c -> c.hasPhysics),
            Codec.BOOL.optionalFieldOf("kill_on_contact", false).forGetter(c -> c.killOnContact),
            Codec.BOOL.optionalFieldOf("kill_when_still", false).forGetter(c -> c.killWhenStill),
            LiquidAffinity.CODEC.optionalFieldOf("liquid_affinity", LiquidAffinity.ANY).forGetter(c -> c.liquidAffinity),
            Colormap.CODEC.optionalFieldOf("colormap").forGetter(c -> Optional.ofNullable(c.colormap)),
            Codec.BOOL.optionalFieldOf("random_sprite", false).forGetter(c -> c.randomSprite),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("limit", 0).forGetter(c ->
                    c.particleGroupLimit.map(ParticleLimit::limit).orElse(0)),
            Codec.BOOL.optionalFieldOf("force_spawn", false).forGetter(c -> c.forceSpawn),
            ParticleInitializer.CODEC.optionalFieldOf("initializer").forGetter(c -> Optional.ofNullable(c.initializer)),
            Ticker.CODEC.optionalFieldOf("ticker").forGetter(c -> Optional.ofNullable(c.ticker)),
            ParticleSoundEmitter.CODEC.listOf().optionalFieldOf("sound_emitters", List.of()).forGetter(c -> c.sounds),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("tick_interval", 1).forGetter(c -> c.tickRate),
            Codec.PASSTHROUGH.listOf().optionalFieldOf("particle_emitters", List.of()).forGetter(c -> c.lazyParticles),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("exclusion_radius", 0).forGetter(c -> c.exclusionRadius)
    ).apply(i, CustomParticleType::new));

    private CustomParticleType(ParticleRenderMode renderType, RotationProvider rotationProvider,
                               Optional<Identifier> model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact, boolean killWhenStill,
                               LiquidAffinity liquidAffinity, Optional<IColorGetter> colormap,
                               boolean randomSprite,
                               int limit, boolean forceSpawn, Optional<ParticleInitializer> initializer,
                               Optional<Ticker> ticker, List<ParticleSoundEmitter> sounds, int tickRate, List<Dynamic<?>> particles, int killSimilarInRadius) {
        this(renderType, rotationProvider, model.orElse(null), offset,
                light, hasPhysics, killOnContact, killWhenStill, liquidAffinity, colormap.orElse(null),
                randomSprite, limit, forceSpawn,
                initializer.orElse(null), ticker.orElse(null), sounds, tickRate, particles, killSimilarInRadius);
    }

    @Override
    public boolean forceSpawns() {
        return forceSpawn;
    }

    @Override
    public @Nullable Identifier getCustomModel() {
        return this.model;
    }

    public static void setStateHack(BlockState state) {
        STATE_HACK = state;
    }

    private ParticleRenderMode getRenderType() {
        return renderType;
    }

    @Override
    public Particle createParticleWithState(ExtraDataParticleOptions opt, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                            @Nullable BlockState state, RandomSource random) {
        if (spriteSet != null) {
            // some people might want this

            Instance newParticle = new Instance(world, x, y, z, xSpeed, ySpeed, zSpeed, state, this, random);
            opt.apply(newParticle);
            if (this.hasPhysics) {
                for (VoxelShape voxelShape : world.getBlockCollisions(null, newParticle.getBoundingBox())) {
                    if (!voxelShape.isEmpty()) {
                        return null;
                    }
                }
            }

            if (this.ticker != null && this.ticker.removeIf != null) {
                if (this.ticker.removeIf.evaluate(newParticle, world) > 0) {
                    return null;
                }
            }
            if (exclusionRadius > 0) {
                ParticleRenderType particleRenderType = this.getParticleGroup();
                double radiusSquared = exclusionRadius * exclusionRadius;
                var particleQueue = Minecraft.getInstance().particleEngine.particles.get(particleRenderType);

                if (particleQueue != null) {
                    for (var p : particleQueue.getAll()) {
                        if (p instanceof Instance inst && inst.type == this) {
                            //calculate distance between p and newParticle
                            double distSqrt = Mth.lengthSquared(
                                    inst.x - newParticle.x,
                                    inst.y - newParticle.y,
                                    inst.z - newParticle.z);

                            if (distSqrt < radiusSquared) {
                                if (inst.hasAgeLeft()) {
                                    //If it is still alive, we should not spawn a new one in the same place.
                                    return null;
                                } else {
                                    //It's dead, but still present — remove it to make room for the new one
                                    inst.remove();
                                }
                            }
                        }
                    }
                }
            }
            return newParticle;
        } else {
            throw new IllegalStateException("Sprite set not set for custom particle type");
        }
    }

    private ParticleRenderType getParticleGroup() {
        if (renderType == ParticleRenderMode.INVISIBLE) return ParticleRenderType.NO_RENDER;
        return model != null ? PolytoneRenderTypes.PARTICLE_MODEL_GROUP : ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    public void setSpriteSet(SpriteSet spriteSet) {
        this.spriteSet = spriteSet;
    }

    public void setUnregistered() {
        this.isValid = false;
    }

    public static class Instance extends SingleQuadParticle {

        protected final CustomParticleType type;
        protected final @Nullable QuadCollection model;
        protected final SpriteSet spriteSet;
        protected final LiquidAffinity liquidAffinity;
        protected final List<ParticleTickable> tickables;
        protected float oQuadSize;
        protected double custom;

        protected Instance(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                           @Nullable BlockState state, CustomParticleType customType, RandomSource random) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed, customType.randomSprite ? customType.spriteSet.get(random) : customType.spriteSet.first());
            if (customType.randomSprite) {
                this.spriteSet = null;
                this.setSprite(customType.spriteSet.get(random));

            } else {
                this.spriteSet = customType.spriteSet;
                this.setSpriteFromAge(spriteSet);
            }


            this.setSize(0.1f, 0.1f);
            this.type = customType;

            this.tickables = new ArrayList<>();
            this.tickables.addAll(customType.sounds);
            this.tickables.addAll(customType.particles);

            //for normal particles since its simple particle types (so that they can be ued in biomes) we can pass extra params
            if (state == null) state = STATE_HACK;

            // remove randomness
            this.x = x;
            this.y = y;
            this.z = z;
            this.xd = xSpeed;
            this.yd = ySpeed;
            this.zd = zSpeed;
            this.model = customType.model == null ? null : SpecialModelsHandler.getSpecialModel(customType.model);
            ParticleInitializer initializer = customType.initializer;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (initializer != null) {
                initializer.initialize(this, level, state, pos);
            }

            this.oQuadSize = quadSize;

            this.liquidAffinity = customType.liquidAffinity;
            this.hasPhysics = customType.hasPhysics;

            if (this.type.colormap != null) {
                float[] unpack = ColorUtils.unpack(this.type.colormap.getColor(state, level, pos, 0));
                this.setColor(unpack[0], unpack[1], unpack[2]);
            }

        }

        private boolean hasAgeLeft() {
            return this.age < this.lifetime;
        }

        public double getCustom() {
            return custom;
        }

        @Override
        public @NotNull Optional<ParticleLimit> getParticleLimit() {
            return this.type.particleGroupLimit;
        }

        //Unused. Why? because we need extra context of the particle iteself to be passed in apply rotation
        @Override
        public FacingCameraMode getFacingCameraMode() {
            return this.type.rotationProvider;
        }

        @Override
        public void extract(QuadParticleRenderState quadParticleRenderState, Camera camera, float f) {
            Quaternionf quaternionf = new Quaternionf();
            this.type.rotationProvider.setRotation(this, quaternionf, camera, f);
            if (this.roll != 0.0F) {
                quaternionf.rotateZ(Mth.lerp(f, this.oRoll, this.roll));
            }

            this.extractRotatedQuad(quadParticleRenderState, camera, quaternionf, f);

            if (!this.type.rotationProvider.alwaysFacesCamera() && model == null) {
                quaternionf.rotateX(Mth.PI);
                //render back face
                this.extractRotatedQuad(quadParticleRenderState, camera, quaternionf, f);
            }
        }

        public void extractModel(ModelParticleRenderState modelParticleRenderState, Camera camera, float f) {
            Quaternionf quaternionf = new Quaternionf();
            this.type.rotationProvider.setRotation(this, quaternionf, camera, f);
            if (this.roll != 0.0F) {
                quaternionf.rotateZ(Mth.lerp(f, this.oRoll, this.roll));
            }
            var offset = this.type.offset;
            modelParticleRenderState.add(
                    RenderTypes.cutoutMovingBlock() ,
                    (float) (x + offset.x), (float) (y + offset.y), (float) (z + offset.z),
                    quaternionf.x,
                    quaternionf.y,
                    quaternionf.z,
                    quaternionf.w,
                    this.getQuadSize(f),
                    this.rCol, this.gCol, this.bCol, this.alpha,
                    this.getLightColor(f),
                    this.model
            );
        }

        @Override
        protected void extractRotatedQuad(QuadParticleRenderState quadParticleRenderState,
                                          Quaternionf rot, float x, float y, float z, float f) {
            var offset = this.type.offset;
            super.extractRotatedQuad(quadParticleRenderState, rot,
                    (float) (x + offset.x), (float) (y + offset.y), (float) (z + offset.z), f);
        }

        @Override
        protected int getLightColor(float partialTick) {
            int total = super.getLightColor(partialTick);
            if (this.type.lightLevel > 0) {
                int sky = LightTexture.sky(total);
                int block = LightTexture.block(total);
                block = Math.max(block, this.type.lightLevel);
                return LightTexture.pack(block, sky);
            }
            return total;
        }

        @Override
        public void remove() {
            super.remove();
            this.age = this.lifetime;
        }

        @Override
        public void tick() {
            if (!this.type.isValid) {
                this.remove();
                return;
            }
            if (spriteSet != null) this.setSpriteFromAge(spriteSet);
            super.tick();
            //interpolate our states
            this.oRoll = this.roll;
            this.oQuadSize = this.quadSize;

            boolean isTickTime = this.age % this.type.tickRate == 0;

            if (type.ticker != null && isTickTime) {
                type.ticker.tick(this, level);
            }

            if (this.type.colormap != null) {
                BlockPos pos = BlockPos.containing(x, y, z);
                float[] unpack = ColorUtils.unpack(this.type.colormap.getColor(null, level, pos, 0));
                this.setColor(unpack[0], unpack[1], unpack[2]);
            }

            if (this.age > 1 && type.killWhenStill && this.x == this.xo && this.y == this.yo && this.z == this.zo) {
                this.remove();
            }

            if (liquidAffinity != LiquidAffinity.ANY) {
                BlockState state = level.getBlockState(BlockPos.containing(x, y, z));
                if (liquidAffinity == LiquidAffinity.LIQUIDS ^ !state.getFluidState().isEmpty()) {
                    this.remove();
                }
            }
            if (!this.removed && isTickTime) {
                for (ParticleTickable tickable : this.tickables) {
                    tickable.tick(this, level);
                }
            }
        }

        @Override
        public void move(double x, double y, double z) {
            super.move(x, y, z);
            if (type.killOnContact && this.age > 1) {
                Vec3 myPos = new Vec3(this.x, this.y, this.z);
                Vec3 wantedPos = new Vec3(this.xo + x, this.yo + y, this.zo + z);
                if (myPos.distanceToSqr(wantedPos) > 0.000001) {
                    // collided with any block. pop. It fragile
                    this.remove();
                    this.xd = 0;
                    this.yd = 0;
                    this.zd = 0;
                }
            }
        }

        @Override
        public float getQuadSize(float scaleFactor) {
            return Mth.lerp(scaleFactor, this.oQuadSize, this.quadSize);
        }

        @Override
        protected Layer getLayer() {
            return type.getRenderType().getLayer(model != null);
        }


        @Override
        public ParticleRenderType getGroup() {
            return this.type.getParticleGroup();
        }
    }


    //TODO: merge this and particle modifier
    protected record Ticker(@Nullable IParticleExp x,
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
                            @Nullable IParticleExp removeIf) {

        private static final Codec<Ticker> CODEC = RecordCodecBuilder.create(i -> i.group(
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
        ).apply(i, Ticker::new));

        private Ticker(Optional<IParticleExp> x, Optional<IParticleExp> y,
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

        private void tick(CustomParticleType.Instance particle, ClientLevel level) {
            if (this.roll != null) {
                particle.roll = (float) this.roll.evaluate(particle, level);
            }
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

    public static final Codec<Optional<Identifier>> CUSTOM_MODEL_ONLY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("model").forGetter(e -> e)
    ).apply(i, r -> r));
}

