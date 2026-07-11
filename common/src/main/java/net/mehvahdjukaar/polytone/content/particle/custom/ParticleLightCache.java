package net.mehvahdjukaar.polytone.content.particle.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * Section-version counters for per-particle light caches. Every section rebuild (block or light
 * change) funnels through LevelRenderer#setSectionDirty, which bumps the section's hashed bucket.
 * Each particle owns an {@link Entry} that re-samples only when stale.
 */
public final class ParticleLightCache {

    private static final int BUCKETS = 1024; // power of two, ~4 KB
    private static final int MASK = BUCKETS - 1;
    // Bucket collisions cause a harmless extra re-sample, never a missed update. Plain int[]:
    // reads/writes are atomic and per-frame task submission publishes main-thread bumps to workers.
    private static final int[] VERSIONS = new int[BUCKETS];

    private ParticleLightCache() {}

    private static int bucket(int sx, int sy, int sz) {
        int h = (sx * 31 + sy) * 31 + sz;
        h ^= h >>> 15; // spread so neighbouring sections don't clump into one bucket
        return h & MASK;
    }

    /** Called from the LevelRenderer mixin (section coords) on the client thread. */
    public static void markSectionDirty(int sectionX, int sectionY, int sectionZ) {
        VERSIONS[bucket(sectionX, sectionY, sectionZ)]++;
    }

    /** Current invalidation version for the given section. */
    public static int sectionVersion(int sectionX, int sectionY, int sectionZ) {
        return VERSIONS[bucket(sectionX, sectionY, sectionZ)];
    }

    /**
     * Per-particle cached light sample. Valid while the particle stays inside the same block
     * AND its section's version counter hasn't moved; otherwise {@code sampler} re-runs the
     * expensive world/light lookup. Owned by the particle, so it dies with it.
     */
    public static final class Entry {

        private long blockKey = Long.MIN_VALUE; // forces a first sample
        private int sectionVersion;
        private int rawLight;

        /** The raw (unboosted) light color at the given position, re-sampled only when stale. */
        public int get(double x, double y, double z, float partialTick, Sampler sampler) {
            int bx = Mth.floor(x), by = Mth.floor(y), bz = Mth.floor(z);
            long key = BlockPos.asLong(bx, by, bz);
            int version = sectionVersion(bx >> 4, by >> 4, bz >> 4);
            if (key != this.blockKey || version != this.sectionVersion) {
                this.blockKey = key;
                this.sectionVersion = version;
                this.rawLight = sampler.sample(partialTick); // the expensive world/light lookup
            }
            return this.rawLight;
        }

        /** The underlying lookup; store the method ref once, so calls stay allocation-free. */
        public interface Sampler {
            int sample(float partialTick);
        }
    }
}
