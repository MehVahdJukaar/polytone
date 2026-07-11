package net.mehvahdjukaar.polytone.common;

import net.mehvahdjukaar.polytone.Polytone;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TokenBucketTracker {

    // canEmitParticle runs on particle-worker threads (async particles) while tick()/clear() run on
    // the main thread, so both collections must tolerate concurrent access.
    private static final Queue<TokenBucket> BUCKETS = new ConcurrentLinkedQueue<>();
    //TODO: add these inside block and particle emitters
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

    //TODO: change . default logic
    public static boolean canEmitParticle(Object obj) {
        if (!Polytone.CONFIGS.autoParticleRateLimit.get()) return true;
        return OBJECTS_TOKENS.computeIfAbsent(obj, o ->
                track(TokenBucket.create(2, 400))

        ).tryAcquire();
    }
}
