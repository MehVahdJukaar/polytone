package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SemiCustomParticleType implements CustomParticleFactory {

    private final ParticleType<?> type;
    private ParticleProvider<?> copyProvider = null;
    private boolean hasBeenInit = false;
    private ParticleEngine.MutableSpriteSet spriteSet = null;

    public SemiCustomParticleType(ParticleType<?> type) {
        this.type = type;
    }

    public static final Codec<SemiCustomParticleType> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.PARTICLE_TYPE.byNameCodec().fieldOf("copy_from").forGetter(c -> c.type)
    ).apply(i, SemiCustomParticleType::new));


    @Override
    public void setSpriteSet(ParticleEngine.MutableSpriteSet mutableSpriteSet) {
        this.spriteSet = mutableSpriteSet;
    }

    @Override
    public void createParticle(ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, @Nullable BlockState state) {
        if (!hasBeenInit) {
            init();
        }

        if (copyProvider != null) {
            var particle = ((ParticleProvider) copyProvider).createParticle(((ParticleOptions) type), world, x, y, z, xSpeed, ySpeed, zSpeed);
            Minecraft.getInstance().particleEngine.add(particle);
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
