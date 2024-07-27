package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class SemiCustomParticleType implements CustomParticleFactory {

    private final ParticleType<?> type;
    private ParticleProvider<?> copyProvider = null;
    private boolean hasBeenInit = false;
    private ParticleEngine.MutableSpriteSet spriteSet = null;
    private final @Nullable CustomParticleType.Initializer initializer;

    public SemiCustomParticleType(ParticleType<?> type, Optional<CustomParticleType.Initializer> initializer) {
        this.type = type;
        this.initializer = initializer.orElse(null);
    }

    public static final Codec<SemiCustomParticleType> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.PARTICLE_TYPE.byNameCodec().fieldOf("copy_from").forGetter(c -> c.type),
            StrOpt.of(CustomParticleType.Initializer.CODEC, "initializer").forGetter(c -> Optional.ofNullable(c.initializer))
            ).apply(i, SemiCustomParticleType::new));


    @Override
    public void setSpriteSet(ParticleEngine.MutableSpriteSet mutableSpriteSet) {
        this.spriteSet = mutableSpriteSet;
    }

    @Override
    public void createParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, @Nullable BlockState state) {
        if (!hasBeenInit) {
            init();
        }

        if (copyProvider != null) {
            var particle = ((ParticleProvider) copyProvider).createParticle(((ParticleOptions) type), level, x, y, z, xSpeed, ySpeed, zSpeed);

            //initialize
            if (initializer != null && particle != null) {
                BlockPos pos = BlockPos.containing(x, y, z);
                if (initializer.roll() != null) {
                    particle.roll = (float) initializer.roll().getValue(level, pos, state);
                }
                if (initializer.size() != null) {
                    particle.scale((float) initializer.size().getValue(level, pos, state));
                }
                if (initializer.red() != null) {
                    particle.rCol = (float) initializer.red().getValue(level, pos, state);
                }
                if (initializer.green() != null) {
                    particle.gCol = (float) initializer.green().getValue(level, pos, state);
                }
                if (initializer.blue() != null) {
                    particle.bCol = (float) initializer.blue().getValue(level, pos, state);
                }
                if (initializer.alpha() != null) {
                    particle.alpha = (float) initializer.alpha().getValue(level, pos, state);
                }
                if (initializer.lifetime() != null) {
                    particle.setLifetime((int) initializer.lifetime().getValue(level, pos, state));
                }
            }
        }
    }

    private void init() {

        hasBeenInit = true;
        copyProvider = PlatStuff.getParticleProvider(type);
        if (copyProvider != null) {
            try {
                copyProvider = cloneProvider(copyProvider, spriteSet);
            } catch (Exception e) {
                Polytone.LOGGER.error("Failed to clone particle provider. Not supported. Try using a different particle type", e);
            }
        } else {
            Polytone.LOGGER.error("Failed to find particle provider for particle type {}", type);
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
