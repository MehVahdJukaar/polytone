package net.mehvahdjukaar.polytone.content.particle.custom;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.expressions.ExpTicker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

// Ticks custom particles in parallel chunks collected during ParticleEngine.tick(), joined before
// anything reads or mutates particle state. Lifetime expiry stays in CustomParticleInstance#tick.
public final class PolytoneAsyncParticles {

    private static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private static final ForkJoinPool POOL = new ForkJoinPool(
            THREADS,
            pool -> {
                ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                t.setName("PolytoneParticleTicker-" + COUNTER.getAndIncrement());
                t.setDaemon(true);
                // MVEL's ASM optimizer defines accessors against the worker's context classloader; the
                // default system loader fails to link mod classes (NoClassDefFoundError). Use the mod's.
                t.setContextClassLoader(PolytoneAsyncParticles.class.getClassLoader());
                return t;
            },
            (thread, ex) -> Polytone.LOGGER.error("Uncaught exception ticking particles on {}", thread.getName(), ex),
            true);

    // Main-thread only. Newborns ride the same queue flagged via pendingInitTick. dispatch()
    // snapshots and clears before submitting, so mid-flight enqueues wait for the next batch.
    private static final List<CustomParticleInstance> PENDING = new ArrayList<>();

    // main-thread actions requested from workers (spawns, sounds), replayed when the batch joins
    private static final Queue<Runnable> DEFERRED_MAIN_ACTIONS = new ConcurrentLinkedQueue<>();

    // written and joined on the main thread only
    private static ForkJoinTask<?>[] inFlight;

    // camera snapshot taken on the main thread at dispatch, read by workers
    private static volatile Camera tickCamera;

    // rate-limits suppressed-error logging
    private static final AtomicInteger ERRORS = new AtomicInteger();

    private static final int MIN_PARALLEL = 64; // below this, chunking costs more than it saves

    private PolytoneAsyncParticles() {}

    public static void enqueue(CustomParticleInstance p) {
        PENDING.add(p);
    }

    public static void enqueueInit(CustomParticleInstance p) {
        p.pendingInitTick = true;
        PENDING.add(p);
    }

    public static ForkJoinTask<?> submitRender(Runnable task) {
        return POOL.submit(task);
    }

    public static Camera camera() {
        Camera c = tickCamera;
        return c != null ? c : Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    // a cosmetic particle must not crash the game
    private static void tickOne(CustomParticleInstance p) {
        try {
            if (p.pendingInitTick) {
                p.pendingInitTick = false;
                p.initTick();
            } else {
                p.tickInternal();
            }
        } catch (Exception | LinkageError e) {
            p.remove(); // drop the offender so it can't keep throwing every tick
            if (ERRORS.getAndIncrement() % 200 == 0) {
                Polytone.LOGGER.error("Suppressed error ticking custom particle off-thread (particle removed)", e);
            }
        }
    }

    // ParticleEngine#particlesToAdd and SoundEngine are not thread safe
    public static void deferToMain(Runnable action) {
        if (Minecraft.getInstance().isSameThread()) {
            action.run();
        } else {
            DEFERRED_MAIN_ACTIONS.add(action);
        }
    }

    // called at the TAIL of ParticleEngine.tick(): submits without waiting, so the batch overlaps the
    // rest of the game tick and the early frame render
    public static void dispatch() {
        int size = PENDING.size();
        if (size == 0) return;

        // snapshot per-tick context so workers never reach into the render system
        tickCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        // re-cache post-entity-tick player stats; ExpTicker's own refresh ran at tick start
        ExpTicker.refreshPlayerSnapshot();

        // workers iterate the copy; anything enqueued mid-flight waits for the next batch
        CustomParticleInstance[] batch = PENDING.toArray(new CustomParticleInstance[0]);
        PENDING.clear();

        // even small batches go to the pool, ticking inline on main would defeat the overlap
        int chunks = (size < MIN_PARALLEL || THREADS <= 1) ? 1
                : Math.min(THREADS, (size + MIN_PARALLEL - 1) / MIN_PARALLEL);
        int per = (size + chunks - 1) / chunks;
        ForkJoinTask<?>[] tasks = new ForkJoinTask[chunks];
        for (int c = 0; c < chunks; c++) {
            final int start = c * per;
            final int end = Math.min(start + per, size);
            tasks[c] = POOL.submit(() -> {
                for (int i = start; i < end; i++) tickOne(batch[i]);
            });
        }
        inFlight = tasks;
    }

    // idempotent, main thread only: called before render extract, before the next engine tick, and on
    // level changes
    public static void awaitTicks() {
        if (inFlight == null) return;
        try {
            for (ForkJoinTask<?> t : inFlight) t.join();
        } finally {
            inFlight = null;
            tickCamera = null; // async-off path falls back to a live lookup
        }
        Runnable action;
        while ((action = DEFERRED_MAIN_ACTIONS.poll()) != null) {
            action.run();
        }
    }
}
