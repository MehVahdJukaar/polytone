package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.common.codec.BiggerCodecs;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.particle.ParticleParticleEmitter;
import net.mehvahdjukaar.polytone.content.sound.ParticleSoundEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomParticleType implements ICustomParticleFactory {

    protected final ParticleRenderMode renderType;
    protected final @Nullable Identifier model;
    protected final @Nullable CustomParticleInitializer initializer;
    protected final ICustomParticleTicker ticker;
    protected final List<ParticleSoundEmitter> sounds;
    protected final int tickRate;
    protected final int exclusionRadius;
    protected final List<ParticleParticleEmitter> particles = new ArrayList<>();
    @Nullable
    protected List<Dynamic<?>> lazyParticles;

    protected final int lightLevel;
    protected final LiquidAffinity liquidAffinity;
    protected final boolean hasPhysics;
    protected final boolean killOnContact;
    protected final boolean killWhenStill;
    protected final @Nullable IColorGetter colormap;
    protected final IRotationProvider rotationProvider;
    protected final Vec3 offset;
    protected final Optional<ParticleLimit> particleGroupLimit;
    protected final boolean forceSpawn;

    protected final SpritePicker spritePicker;

    private boolean isValid = true;

    private CustomParticleType(ParticleRenderMode renderType, IRotationProvider rotationProvider,
                               @Nullable Identifier model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact, boolean killWhenStill,
                               LiquidAffinity liquidAffinity, @Nullable IColorGetter colormap,
                               boolean randomSprite,
                               int particleGroupLimit, boolean forceSpawn,
                               @Nullable CustomParticleInitializer initializer, ICustomParticleTicker ticker,
                               @Nullable List<ParticleSoundEmitter> sounds,
                               int tickRate, @Nullable List<Dynamic<?>> particles, int killSimilarInRadius) {
        this.renderType = renderType;
        this.spritePicker = new SpritePicker(randomSprite);
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
            ParticleRenderMode.CODEC.optionalFieldOf("render_type", ParticleRenderMode.OPAQUE).forGetter(c -> c.renderType),
            IRotationProvider.CODEC.optionalFieldOf("rotation_mode", RotationMode.LOOK_AT_XYZ).forGetter(c -> c.rotationProvider),
            Identifier.CODEC.optionalFieldOf("model").forGetter(c -> Optional.ofNullable(c.model)),
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(c -> c.offset),
            Codec.intRange(0, 15).optionalFieldOf("light_level", 0).forGetter(c -> c.lightLevel),
            Codec.BOOL.optionalFieldOf("has_physics", true).forGetter(c -> c.hasPhysics),
            Codec.BOOL.optionalFieldOf("kill_on_contact", false).forGetter(c -> c.killOnContact),
            Codec.BOOL.optionalFieldOf("kill_when_still", false).forGetter(c -> c.killWhenStill),
            LiquidAffinity.CODEC.optionalFieldOf("liquid_affinity", LiquidAffinity.ANY).forGetter(c -> c.liquidAffinity),
            //TODO: remove
            Colormap.CODEC.optionalFieldOf("colormap").forGetter(c -> Optional.ofNullable(c.colormap)),
            Codec.BOOL.optionalFieldOf("random_sprite", false).forGetter(c -> c.spritePicker.selectsRandom()),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("limit", 0).forGetter(c ->
                    c.particleGroupLimit.map(ParticleLimit::limit).orElse(0)),
            Codec.BOOL.optionalFieldOf("force_spawn", false).forGetter(c -> c.forceSpawn),
            CustomParticleInitializer.CODEC.optionalFieldOf("initializer").forGetter(c -> Optional.ofNullable(c.initializer)),
            ICustomParticleTicker.CODEC.optionalFieldOf("ticker", ICustomParticleTicker.NO_OP).forGetter(c -> c.ticker),
            ParticleSoundEmitter.CODEC.listOf().optionalFieldOf("sound_emitters", List.of()).forGetter(c -> c.sounds),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("tick_interval", 1).forGetter(c -> c.tickRate),
            Codec.PASSTHROUGH.listOf().optionalFieldOf("particle_emitters", List.of()).forGetter(c -> c.lazyParticles),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("exclusion_radius", 0).forGetter(c -> c.exclusionRadius)
    ).apply(i, CustomParticleType::new));

    private CustomParticleType(ParticleRenderMode renderType, IRotationProvider rotationProvider,
                               Optional<Identifier> model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact, boolean killWhenStill,
                               LiquidAffinity liquidAffinity, Optional<IColorGetter> colormap,
                               boolean randomSprite,
                               int limit, boolean forceSpawn, Optional<CustomParticleInitializer> initializer,
                               ICustomParticleTicker ticker, List<ParticleSoundEmitter> sounds, int tickRate, List<Dynamic<?>> particles, int killSimilarInRadius) {
        this(renderType, rotationProvider, model.orElse(null), offset,
                light, hasPhysics, killOnContact, killWhenStill, liquidAffinity, colormap.orElse(null),
                randomSprite, limit, forceSpawn,
                initializer.orElse(null), ticker, sounds, tickRate, particles, killSimilarInRadius);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public boolean forceSpawns() {
        return forceSpawn;
    }

    @Override
    public @Nullable Identifier getCustomModel() {
        return this.model;
    }

    @Override
    public Particle createParticleWithState(ExtraDataParticleOptions opt, ClientLevel world,
                                            double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                            @Nullable BlockState state, RandomSource random) {
        if (!spritePicker.hasSprites()) {
            throw new IllegalStateException("Sprite set not set for custom particle type");
        }

        // some people might want this

        CustomParticleInstance newParticle = new CustomParticleInstance(world, x, y, z, xSpeed, ySpeed, zSpeed, state, this);
        opt.apply(newParticle);
        if (this.hasPhysics) {
            for (VoxelShape voxelShape : world.getBlockCollisions(null, newParticle.getBoundingBox())) {
                if (!voxelShape.isEmpty()) {
                    return null;
                }
            }
        }

        //tick once
        //todo replace   initializer with ticker
        this.ticker.tick(newParticle, world);
        if (!newParticle.isAlive()) {
            return null;

        }
        if (exclusionRadius > 0) {
            ParticleRenderType particleRenderType = this.getParticleGroup();
            double radiusSquared = exclusionRadius * exclusionRadius;
            var particleQueue = Minecraft.getInstance().particleEngine.particles.get(particleRenderType);

            if (particleQueue != null) {
                for (var p : particleQueue.getAll()) {
                    if (p instanceof CustomParticleInstance inst && inst.type == this) {
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
    }

    public ParticleRenderType getParticleGroup() {
        if (renderType == ParticleRenderMode.INVISIBLE) return ParticleRenderType.NO_RENDER;
        return model != null ? PolytoneRenderTypes.PARTICLE_MODEL_GROUP : ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    public void setSpriteSet(SpriteSet spriteSet) {
        this.spritePicker.acceptSprites(spriteSet);
    }

    public void setUnregistered() {
        this.isValid = false;
    }

    public static final Codec<Optional<Identifier>> CUSTOM_MODEL_ONLY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("model").forGetter(e -> e)
    ).apply(i, r -> r));

}

