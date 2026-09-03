package net.mehvahdjukaar.polytone.utils;

import net.mehvahdjukaar.polytone.Polytone;

import java.util.Queue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TokenBucketTracker {

    //concurrent: async custom particles register/consume buckets off the render thread
    private static final Queue<TokenBucket> BUCKETS = new ConcurrentLinkedQueue<>();
    private static final Map<Object, TokenBucket> OBJECTS_TOKENS = new ConcurrentHashMap<>();

    public static TokenBucket track(TokenBucket tb) {
        BUCKETS.add(tb);
        return tb;
    }

    public static void clear() {
        BUCKETS.clear();
    }

    public static void tick() {
        for (var b : BUCKETS) {
            b.onTick();
        }
    }

    public static boolean canEmitParticle(Object obj) {
        if (!Polytone.CONFIGS.autoParticleRateLimit.get()) return true;
        return OBJECTS_TOKENS.computeIfAbsent(obj, o ->
                track(TokenBucket.create(2, 400))
        ).tryConsuming();
    }
}
