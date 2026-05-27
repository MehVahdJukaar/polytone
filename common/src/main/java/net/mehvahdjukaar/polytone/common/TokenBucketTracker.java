package net.mehvahdjukaar.polytone.common;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokenBucketTracker {

    private static final List<TokenBucket> BUCKETS = new ArrayList<>();
    //TODO: add these inside block and particle emitters
    private static final Map<Object, TokenBucket> OBJECTS_TOKENS = new Object2ObjectOpenHashMap<>();

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
