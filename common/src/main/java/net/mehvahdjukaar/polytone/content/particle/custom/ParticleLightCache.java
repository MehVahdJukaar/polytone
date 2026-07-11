package net.mehvahdjukaar.polytone.content.particle.custom;

/**
 * Section-version counters for per-particle light caches. Every section rebuild (block or light
 * change) funnels through LevelRenderer#setSectionDirty, which bumps the section's hashed bucket.
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
}
