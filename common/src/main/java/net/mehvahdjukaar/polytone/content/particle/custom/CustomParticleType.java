package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.particle.ParticleParticleEmitter;
import net.mehvahdjukaar.polytone.content.sound.ParticleSoundEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomParticleType implements ICustomParticleFactory {

    protected final ParticleRenderMode renderType;
    protected final @Nullable ResourceLocation model;
    protected final @Nullable CustomParticleInitializer initializer;
    protected final ICustomParticleTicker ticker;
    protected final List<ParticleSoundEmitter> sounds;
    protected final int tickRate;
    protected final int exclusionRadius;
    protected final List<ParticleParticleEmitter> particleEmitters = new ArrayList<>();
    @Nullable
    protected List<Dynamic<?>> lazyEmitters;

    protected final int lightLevel;
    protected final LiquidAffinity liquidAffinity;
    protected final boolean hasPhysics;
    protected final boolean killOnContact;
    protected final boolean killWhenStill;
    protected final boolean killWhenNotInView;
    protected final @Nullable ParticleColor colormap;
    protected final IRotationProvider rotationProvider;
    protected final Vec3 offset;
    protected final Optional<ParticleGroup> particleGroupLimit;
    protected final boolean forceSpawn;
    protected final boolean sticky;

    protected final SpritePicker spritePicker;

    private boolean isValid = true;
    protected ResourceLocation debugId = null;

    private CustomParticleType(ParticleRenderMode renderType, IRotationProvider rotationProvider,
                               @Nullable ResourceLocation model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact, boolean killWhenStill, boolean killWhenNotInView,
                               LiquidAffinity liquidAffinity, @Nullable ParticleColor colormap,
                               boolean randomSprite,
                               int particleGroupLimit, boolean forceSpawn,
                               @Nullable CustomParticleInitializer initializer, ICustomParticleTicker ticker,
                               @Nullable List<ParticleSoundEmitter> sounds,
                               int tickRate, @Nullable List<Dynamic<?>> particles, int killSimilarInRadius,
                               boolean sticky) {
        this.renderType = renderType;
        this.spritePicker = new SpritePicker(randomSprite);
        this.model = model;
        this.initializer = initializer;
        this.ticker = ticker;
        this.sounds = sounds;
        this.lazyEmitters = particles;
        this.lightLevel = light;
        this.hasPhysics = hasPhysics;
        this.killOnContact = killOnContact;
        this.killWhenStill = killWhenStill;
        this.killWhenNotInView = killWhenNotInView;
        this.liquidAffinity = liquidAffinity;
        this.forceSpawn = forceSpawn;
        this.colormap = colormap;
        this.offset = offset;
        this.rotationProvider = rotationProvider;
        this.tickRate = tickRate;
        this.exclusionRadius = killSimilarInRadius;
        this.particleGroupLimit = particleGroupLimit > 0 ? Optional.of(new ParticleGroup(particleGroupLimit)) : Optional.empty();
        this.sticky = sticky;
    }

    public static final SchemaCodec<CustomParticleType> CODEC = SchemaRecord.create(CustomParticleType.class, i -> i.group(
            i.optional("render_type", ParticleRenderMode.CODEC, ParticleRenderMode.OPAQUE, c -> c.renderType),
            i.optional("rotation_mode", IRotationProvider.CODEC, RotationMode.LOOK_AT_XYZ, c -> c.rotationProvider),
            i.optional("model", ResourceLocation.CODEC, c -> Optional.ofNullable(c.model)),
            i.optional("offset", Vec3.CODEC, Vec3.ZERO, c -> c.offset),
            i.optional("light_level", Codec.intRange(0, 15), 0, c -> c.lightLevel),
            i.optional("has_physics", Codec.BOOL, true, c -> c.hasPhysics),
            i.optional("kill_on_contact", Codec.BOOL, false, c -> c.killOnContact),
            i.optional("kill_when_still", Codec.BOOL, false, c -> c.killWhenStill),
            i.optional("kill_when_not_in_view", Codec.BOOL, true, c -> c.killWhenNotInView),
            i.optional("liquid_affinity", LiquidAffinity.CODEC, LiquidAffinity.ANY, c -> c.liquidAffinity),
            i.optional("colormap", ParticleColor.CODEC, c -> Optional.ofNullable(c.colormap)),
            i.optional("random_sprite", Codec.BOOL, false, c -> c.spritePicker.selectsRandom()),
            i.optional("limit", ExtraCodecs.NON_NEGATIVE_INT, 0, c ->
                    c.particleGroupLimit.map(ParticleGroup::getLimit).orElse(0)),
            i.optional("force_spawn", Codec.BOOL, false, c -> c.forceSpawn),
            i.optional("initializer", CustomParticleInitializer.CODEC, c -> Optional.ofNullable(c.initializer)),
            i.optional("ticker", ICustomParticleTicker.CODEC, ICustomParticleTicker.NO_OP, c -> c.ticker),
            i.optional("sound_emitters", ParticleSoundEmitter.CODEC.listOf(), List.of(), c -> c.sounds),
            i.optional("tick_interval", ExtraCodecs.POSITIVE_INT, 1, c -> c.tickRate),
            i.optional("particle_emitters", Codec.PASSTHROUGH.listOf(), List.of(), c -> c.lazyEmitters),
            i.optional("exclusion_radius", ExtraCodecs.NON_NEGATIVE_INT, 0, c -> c.exclusionRadius),
            i.optional("sticky", Codec.BOOL, false, c -> c.sticky)
    ).apply(i, CustomParticleType::new));

    private CustomParticleType(ParticleRenderMode renderType, IRotationProvider rotationProvider,
                               Optional<ResourceLocation> model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact, boolean killWhenStill, boolean killWhenNotInView,
                               LiquidAffinity liquidAffinity, Optional<ParticleColor> colormap,
                               boolean randomSprite,
                               int limit, boolean forceSpawn, Optional<CustomParticleInitializer> initializer,
                               ICustomParticleTicker ticker, List<ParticleSoundEmitter> sounds, int tickRate,
                               List<Dynamic<?>> particles, int killSimilarInRadius,
                               boolean sticky) {
        this(renderType, rotationProvider, model.orElse(null), offset,
                light, hasPhysics, killOnContact, killWhenStill,killWhenNotInView,
                liquidAffinity, colormap.orElse(null),
                randomSprite, limit, forceSpawn,
                initializer.orElse(null), ticker, sounds, tickRate,
                particles, killSimilarInRadius, sticky);
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
    public Particle createParticle(ParticleOptions opt, ClientLevel world,
                                   double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        if (!spritePicker.hasSprites()) {
            throw new IllegalStateException("Sprite set not set for custom particle type");
        }

        BlockState state = opt instanceof BlockParticleOption bp ? bp.getState() : null;
        CustomParticleInstance newParticle = new CustomParticleInstance(world, x, y, z, xSpeed, ySpeed, zSpeed, state, this);
        if (opt instanceof ExtraDataParticleOptions ep) ep.apply(newParticle);
        if (this.hasPhysics) {
            for (VoxelShape voxelShape : world.getBlockCollisions(null, newParticle.getBoundingBox())) {
                if (!voxelShape.isEmpty()) {
                    return null;
                }
            }
        }

        //todo replace   initializer with ticker
        if (Polytone.CONFIGS.particlesOffThread.get()) {
            // run the spawn-time ticker pass in this tick's parallel batch instead of on the main
            // thread; the batch joins before render extract, so the first frame is identical
            PolytoneAsyncParticleHandler.enqueueInit(newParticle);
        } else {
            this.ticker.tick(newParticle, world);
            newParticle.setAge(0); //reset age after tick, so that it doesn't get affected by initializer tick
            if (!newParticle.isAlive()) {
                return null;
            }
        }
        if (exclusionRadius > 0) {
            ParticleRenderType particleRenderType = this.getParticleGroup();
            double radiusSquared = exclusionRadius * exclusionRadius;
            var particleQueue = Minecraft.getInstance().particleEngine.particles.get(particleRenderType);

            if (particleQueue != null) {
                for (var p : particleQueue) {
                    //hack
                    if (p instanceof CustomParticleInstance inst && inst.type == this) {
                        double distSqrt = Mth.lengthSquared(
                                inst.x - newParticle.x,
                                inst.y - newParticle.y,
                                inst.z - newParticle.z);

                        if (distSqrt < radiusSquared) {
                            if (inst.hasAgeLeft()) {
                                //If it is still alive, we should not spawn a new one in the same place.
                                return null;
                            } else {
                                //It's dead, but still present - remove it to make room for the new one
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
        // must match CustomParticleInstance.getRenderType() so the particle engine buckets these together
        return model != null ? ParticleRenderType.CUSTOM : renderType.getParticle();
    }

    public @Nullable SpriteSet getSpriteSet() {
        return spritePicker.getSpriteSet();
    }

    // Preview only: build an instance (running the initializer, as the constructor does) without the
    // world collision / async / exclusion-radius machinery createParticle adds. Sprites must be set.
    public CustomParticleInstance createPreviewInstance(ClientLevel level, double x, double y, double z,
                                                        @Nullable BlockState state) {
        return new CustomParticleInstance(level, x, y, z, 0, 0, 0, state, this);
    }

    @Override
    public void setSpriteSet(SpriteSet spriteSet) {
        try {
            this.spritePicker.acceptSprites(spriteSet);
        } catch (SpriteSetErrorException e) {
            throw new RuntimeException("Failed to set sprite set for custom particle type: " + this + ".\nDid you remember to add particle sprites?", e);
        }
    }

    @Override
    public String toString() {
        ResourceLocation id = debugId != null ? debugId :
                Polytone.CUSTOM_PARTICLES.getId(this);
        return "CustomParticleType{" + id + "}";
    }

    public void setUnregistered() {
        this.isValid = false;
    }

    public static final Codec<Optional<ResourceLocation>> CUSTOM_MODEL_ONLY_CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("model").forGetter(e -> e)
    ).apply(i, r -> r));

}

