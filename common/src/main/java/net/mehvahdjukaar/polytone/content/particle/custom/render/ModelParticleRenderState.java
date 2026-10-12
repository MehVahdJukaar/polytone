package net.mehvahdjukaar.polytone.content.particle.custom.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.IParticleModel;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinTask;

public class ModelParticleRenderState implements ParticleGroupRenderState {

    private final Map<ParticleRenderMode, List<ParticleRenderInstance>> particles = new HashMap<>();

    private ForkJoinTask<?> extractionFuture;

    // Set by ModelParticleRenderGroup when extraction is dispatched off-thread
    public void setExtractionFuture(ForkJoinTask<?> future) {
        // Join any still-in-flight extraction before overwriting it. submit()/clear() normally join
        // it first (so this is a no-op in the frame loop), but if a frame skips both, this stops the
        // previous worker from racing the new one on this state's particle map.
        awaitExtraction();
        this.extractionFuture = future;
    }

    // Block until off-thread extraction (if any) has finished writing this state
    private void awaitExtraction() {
        ForkJoinTask<?> f = this.extractionFuture;
        if (f != null) {
            this.extractionFuture = null;
            f.join(); // also surfaces any worker exception on the main thread
        }
    }

    public void add(
            ParticleRenderMode layer, float x, float y, float z,
            float rx, float ry, float rz, float rw, float size, float r, float g, float b, float a, int light,
            @NotNull IParticleModel model, CustomParticleInstance particle, float partialTicks
    ) {
        this.particles.computeIfAbsent(layer, l -> new ArrayList<>())
                .add(new ParticleRenderInstance(x, y, z, rx, ry, rz, rw, size,
                        r, g, b, a, light, model, particle, partialTicks));
    }

    @Override
    public void clear() {
        awaitExtraction();
        this.particles.clear();
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        awaitExtraction();
        for (var v : particles.entrySet()) {
            for (var p : v.getValue()) {

                Quaternionf particleRot = new Quaternionf(-p.rx, -p.ry, -p.rz, -p.rw);

                PoseStack poseStack = new PoseStack();

                poseStack.translate(
                        p.x - cameraRenderState.pos.x,
                        p.y - cameraRenderState.pos.y,
                        p.z - cameraRenderState.pos.z
                );
                poseStack.mulPose(particleRot);

                poseStack.scale(p.size, p.size, p.size);

                p.model.submit(p.particle, submitNodeCollector, poseStack, p.light, ARGB.colorFromFloat(p.a, p.r, p.g, p.b), p.partialTicks);
            }
        }
    }

    private record ParticleRenderInstance(float x, float y, float z,
                                          float rx, float ry, float rz, float rw, float size,
                                          float r, float g, float b, float a, int light,
                                          @NotNull IParticleModel model, CustomParticleInstance particle, float partialTicks) {
    }
}