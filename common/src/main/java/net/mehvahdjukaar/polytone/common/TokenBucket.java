package net.mehvahdjukaar.polytone.common;

public class TokenBucket {

    private final int capacity;        // max tokens (burst size)
    private final int refillPerTick;   // tokens added each tick

    private int tokens;

    public TokenBucket(int capacity, int refillPerTick) {
        if (capacity <= 0 || refillPerTick <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.refillPerTick = refillPerTick;
        this.tokens = capacity; // start full
    }

    public static TokenBucket create(
            int maxSteadyStatePerTick,
            int maxEveryFiveSeconds
    ) {
        if (maxSteadyStatePerTick <= 0 || maxEveryFiveSeconds <= 0) {
            throw new IllegalArgumentException();
        }

        final int ticksPerFiveSeconds = 20 * 5; // 100

        double refillPerTick = (double) maxEveryFiveSeconds / ticksPerFiveSeconds;

        // cap the refill so steady state can't exceed the per-tick max
        refillPerTick = Math.min(refillPerTick, maxSteadyStatePerTick);

        return new TokenBucket(maxEveryFiveSeconds, (int) refillPerTick);
    }

    // Called once per game tick (main thread) while tryAcquire may run on particle-worker threads,
    // so both accessors are synchronized to keep the token count consistent.
    public synchronized void onTick() {
        if (tokens >= capacity) return;
        tokens = Math.min(capacity, tokens + refillPerTick);
    }

    // Try to consume 1 token
    public synchronized boolean tryAcquire() {
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[" + tokens + " out of " + refillPerTick + " @ " + refillPerTick + "/tick ]";
    }
}