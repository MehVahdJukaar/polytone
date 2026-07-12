package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class SemiCustomParticleType implements ICustomParticleFactory {

    private final Optional<ParticleType<?>> copyType;
    @Nullable
    private ParticleProvider<?> copyProvider = null;
    private boolean hasBeenInit = false;
    @Nullable
    private SpriteSet spriteSet = null;
    private final @Nullable CustomParticleInitializer initializer;
    private final boolean hasPhysics;
    private final @Nullable IColorGetter colormap;

    public SemiCustomParticleType(Optional<ParticleType<?>> type, Optional<CustomParticleInitializer> initializer,
                                  boolean hasPhysics, Optional<IColorGetter> colorGetter) {
        this.copyType = type;
        this.hasPhysics = hasPhysics;
        this.colormap = colorGetter.orElse(null);
        this.initializer = initializer.orElse(null);
    }

    public static final SchemaCodec<SemiCustomParticleType> CODEC = SchemaRecord.create(SemiCustomParticleType.class, i -> i.group(
            i.field("copy_from", CodecUtils.forwardAwareByNameCodec(BuiltInRegistries.PARTICLE_TYPE), c -> c.copyType),
            i.optional("initializer", CustomParticleInitializer.CODEC, c -> Optional.ofNullable(c.initializer)),
            i.optional("has_physics", Codec.BOOL, true, c -> c.hasPhysics),
            i.optional("colormap", Colormap.CODEC, c -> Optional.ofNullable(c.colormap))
    ).apply(i, SemiCustomParticleType::new));

    @Override
    public boolean isValid() {
        return copyType.isPresent();
    }

    @Override
    public boolean forceSpawns() {
        return copyType.get().getOverrideLimiter();
    }

    @Override
    public void setSpriteSet(SpriteSet spriteSet) {
        this.spriteSet = spriteSet;
    }

    @Nullable
    @Override
    public Particle createParticle(ParticleOptions opt, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        try {
            if (copyType.isEmpty()) {
                return null;
            }
            if (!hasBeenInit) {
                init();
            }

            if (copyProvider != null) {

                BlockState state = opt instanceof BlockParticleOption bo ? bo.getState() : null;

                var particle = ((ParticleProvider) copyProvider).createParticle(((ParticleOptions) copyType.get()), level, x, y, z, xSpeed, ySpeed, zSpeed);

                BlockPos pos = BlockPos.containing(x, y, z);

                //initialize
                if (initializer != null && particle instanceof SingleQuadParticle sqp) {
                    initializer.initialize(sqp, level, state, pos);


                    if (opt instanceof ExtraDataParticleOptions eo) eo.apply(sqp);
                }

                if (particle != null) {
                    particle.hasPhysics = this.hasPhysics;

                    if (this.colormap != null && particle instanceof SingleQuadParticle sqp) {
                        float[] unpack = ColorUtils.unpack(this.colormap.getColor(state, level, pos, 0));
                        sqp.setColor(unpack[0], unpack[1], unpack[2]);
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
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to create semi custom particle of type {}. This likely means the particle itself is invalid and not supported. The resource pack that adds it HAS TO change it.", copyType, e);
        }
        return null;
    }

    private void init() {

        hasBeenInit = true;
        copyProvider = PlatStuff.getParticleProvider(copyType.get());
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
