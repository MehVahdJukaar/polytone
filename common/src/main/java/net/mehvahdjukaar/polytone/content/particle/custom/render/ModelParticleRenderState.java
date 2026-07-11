package net.mehvahdjukaar.polytone.content.particle.custom.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.QuadCollection;
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

    /** Set by ModelParticleRenderGroup when extraction is dispatched off-thread. */
    public void setExtractionFuture(ForkJoinTask<?> future) {
        this.extractionFuture = future;
    }

    /** Block until off-thread extraction (if any) has finished writing this state. */
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
            @NotNull QuadCollection modelData
    ) {
        this.particles.computeIfAbsent(layer, l -> new ArrayList<>())
                .add(new ParticleRenderInstance(x, y, z, rx, ry, rz, rw, size,
                        r, g, b, a, light, modelData));
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

                poseStack.translate(-0.5, -0.5, -0.5);

                submitNodeCollector.submitCustomGeometry(poseStack, v.getKey().getBlock(), (pose1, vertexConsumer) -> {
                    putModelBulkData(p.modelData, p.light, OverlayTexture.NO_OVERLAY, pose1, vertexConsumer, p.r, p.g, p.b, p.a);
                });
            }
        }
    }

    private static void putModelBulkData(@NotNull QuadCollection quadCollection, int combinedLight, int combinedOverlay,
                                         PoseStack.Pose pose, VertexConsumer buffer, float r, float g, float b, float a) {
        for (BakedQuad bakedQuad : quadCollection.getAll()) {
            buffer.putBulkData(pose, bakedQuad, r, g, b, a, combinedLight, combinedOverlay);
        }
    }


    private record ParticleRenderInstance(float x, float y, float z,
                                          float rx, float ry, float rz, float rw, float size,
                                          float r, float g, float b, float a, int light,
                                          @NotNull QuadCollection modelData) {
    }
}