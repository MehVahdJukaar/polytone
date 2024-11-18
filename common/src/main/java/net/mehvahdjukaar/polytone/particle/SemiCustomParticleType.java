package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class SemiCustomParticleType implements CustomParticleFactory {

    private final ParticleType<?> copyType;
    private ParticleProvider<?> copyProvider = null;
    private boolean hasBeenInit = false;
    private ParticleEngine.MutableSpriteSet spriteSet = null;
    private final @Nullable ParticleInitializer initializer;
    private final boolean hasPhysics;
    private final @Nullable IColorGetter colormap;

    public SemiCustomParticleType(ParticleType<?> type, Optional<ParticleInitializer> initializer,
                                  boolean hasPhysics, Optional<IColorGetter> colorGetter) {
        this.copyType = type;
        this.hasPhysics = hasPhysics;
        this.colormap = colorGetter.orElse(null);
        this.initializer = initializer.orElse(null);
    }

    public static final Codec<SemiCustomParticleType> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.PARTICLE_TYPE.byNameCodec().fieldOf("copy_from").forGetter(c -> c.copyType),
            ParticleInitializer.CODEC.optionalFieldOf("initializer").forGetter(c -> Optional.ofNullable(c.initializer)),
            Codec.BOOL.optionalFieldOf("has_physics", true).forGetter(c -> c.hasPhysics),
            Colormap.CODEC.optionalFieldOf("colormap").forGetter(c -> Optional.ofNullable(c.colormap))
    ).apply(i, SemiCustomParticleType::new));

    @Override
    public boolean forceSpawns() {
        return copyType.getOverrideLimiter();
    }

    @Override
    public void setSpriteSet(ParticleEngine.MutableSpriteSet mutableSpriteSet) {
        this.spriteSet = mutableSpriteSet;
    }

    @Override
    public Particle createParticle(SimpleParticleType t, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                   @Nullable BlockState state) {
        if (!hasBeenInit) {
            init();
        }

        if (copyProvider != null) {
            var particle = ((ParticleProvider) copyProvider).createParticle(((ParticleOptions) copyType), level, x, y, z, xSpeed, ySpeed, zSpeed);

            BlockPos pos = BlockPos.containing(x, y, z);

            //initialize
            if (initializer != null && particle instanceof SingleQuadParticle sp) {
                initializer.initialize(sp, level, state, pos);
            }

            if (particle != null) {
                particle.hasPhysics = this.hasPhysics;

                if (this.colormap != null) {
                    float[] unpack = ColorUtils.unpack(this.colormap.getColor(state, level, pos, 0));
                    particle.setColor(unpack[0], unpack[1], unpack[2]);
                }

                if (this.hasPhysics) {
                    for (VoxelShape voxelShape : level.getBlockCollisions(null, particle.getBoundingBox())) {
                        if (!voxelShape.isEmpty()) {
                            return null;
                        }
                    }
                }
            }




            return particle;
        }
        return null;
    }

    private void init() {

        hasBeenInit = true;
        copyProvider = PlatStuff.getParticleProvider(copyType);
        if (copyProvider != null) {
            try {
                copyProvider = cloneProvider(copyProvider, spriteSet);
            } catch (Exception e) {
                Polytone.LOGGER.error("Failed to clone particle provider. Not supported. Try using a different particle type", e);
            }
        } else {
            Polytone.LOGGER.error("Failed to find particle provider for particle type {}", copyType);
        }

    }


    private static <T> T cloneProvider(T original, Object arg) throws InvocationTargetException,
            InstantiationException, IllegalAccessException {

        Class<?> clazz = original.getClass();
        Constructor<?> constructor = null;

        // Find a constructor with no arguments or one with a specific argument type
        for (Constructor<?> cons : clazz.getDeclaredConstructors()) {
            if (cons.getParameterCount() == 0) {
                constructor = cons;
                break;
            } else if (cons.getParameterCount() == 1 && cons.getParameterTypes()[0].isInstance(arg)) {
                constructor = cons;
                break;
            }
        }

        if (constructor == null) {
            throw new IllegalArgumentException("No suitable constructor found.");
        }

        constructor.setAccessible(true);

        // Use no-arg constructor or pass the specific argument
        return (T) (constructor.getParameterCount() == 0 ? constructor.newInstance() : constructor.newInstance(arg));
    }
}
