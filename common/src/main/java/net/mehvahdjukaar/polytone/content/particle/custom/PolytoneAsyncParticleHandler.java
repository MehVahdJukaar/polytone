package net.mehvahdjukaar.polytone.content.particle.custom;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
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

public final class PolytoneAsyncParticleHandler {

    private static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private static final ForkJoinPool POOL = new ForkJoinPool(
            THREADS,
            pool -> {
                ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                t.setName("PolytoneParticleTicker-" + COUNTER.getAndIncrement());
                t.setDaemon(true);
                t.setContextClassLoader(PolytoneAsyncParticleHandler.class.getClassLoader());
                return t;
            },
            (thread, ex) -> Polytone.LOGGER.error("Uncaught exception ticking particles on {}", thread.getName(), ex),
            true);

    private static final List<CustomParticleInstance> PENDING = new ArrayList<>();
    private static final Queue<Runnable> DEFERRED_MAIN_ACTIONS = new ConcurrentLinkedQueue<>();
    private static ForkJoinTask<?>[] inFlight;
    private static volatile Camera tickCamera;
    private static final AtomicInteger ERRORS = new AtomicInteger();

    private static final int MIN_PARALLEL = 64; // below this, chunking costs more than it saves

    private PolytoneAsyncParticleHandler() {}

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

    public static void deferToMain(Runnable action) {
        if (Minecraft.getInstance().isSameThread()) {
            action.run();
        } else {
            DEFERRED_MAIN_ACTIONS.add(action);
        }
    }

    public static void dispatch() {
        int size = PENDING.size();
        if (size == 0) return;

        tickCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        ClientFrameTicker.refreshPlayerSnapshot();

        CustomParticleInstance[] batch = PENDING.toArray(new CustomParticleInstance[0]);
        PENDING.clear();

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
