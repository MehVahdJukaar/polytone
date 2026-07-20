package net.mehvahdjukaar.polytone.content.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-local gate for the custom-particle editor preview. It is active ONLY on the render thread
 * while the preview is spawning/ticking its sandbox particle, and inactive (null) on every other
 * thread - so normal gameplay, including the async particle workers, behaves byte-for-byte as before.
 *
 * <p>While active it does two things: particle creation runs its spawn-time pass synchronously
 * (instead of enqueuing into the async batch the preview can't drive), and emitted children route
 * into the preview's {@link EmitSink} (instead of {@code level.addParticle}, which would leak them
 * into the live world and never reach the preview). See {@code ParticlePreview}.
 */
public final class ParticlePreviewState {

    public interface EmitSink {
        void emit(Level level, ParticleOptions options, double x, double y, double z,
                  double dx, double dy, double dz);
    }

    private static final ThreadLocal<EmitSink> SINK = new ThreadLocal<>();

    public static void begin(EmitSink sink) {
        SINK.set(sink);
    }

    public static void end() {
        SINK.remove();
    }

    public static boolean active() {
        return SINK.get() != null;
    }

    public static @Nullable EmitSink sink() {
        return SINK.get();
    }
}
