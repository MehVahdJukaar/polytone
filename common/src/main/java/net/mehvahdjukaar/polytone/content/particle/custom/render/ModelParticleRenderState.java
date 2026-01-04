package net.mehvahdjukaar.polytone.content.particle.custom.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.mehvahdjukaar.polytone.common.attributes.EnvironmentAttributeMapMod;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class ModelParticleRenderState implements ParticleGroupRenderState {

    private final Map<RenderType, List<ParticleInstance>> particles = new HashMap<>();

    public void add(
            RenderType layer, float x, float y, float z,
            float rx, float ry, float rz, float rw, float size, int color, int light,
            QuadCollection modelData
    ) {
        this.particles.computeIfAbsent(layer, l -> new ArrayList<>())
                .add(new ParticleInstance(x, y, z, rx, ry, rz, rw, size,
                        color, light, modelData));
    }

    @Override
    public void clear() {
        this.particles.clear();
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        for (var v : particles.entrySet()) {
            for (var p : v.getValue()) {
                Quaternionf q = new Quaternionf(p.rx, p.ry, p.rz, p.rw);
                PoseStack poseStack = new PoseStack();
                poseStack.translate(p.x, p.y, p.z);

                poseStack.scale(p.size, p.size, p.size);
                poseStack.mulPose(q);
                poseStack.translate(-0.5, -0.5, -0.5);

                submitNodeCollector.submitCustomGeometry(poseStack, v.getKey(), (pose1, vertexConsumer) -> {
                    float r = ARGB.red(p.color);
                    float g = ARGB.red(p.color);
                    float b = ARGB.red(p.color);
                    float a = ARGB.red(p.color);
                    putModelBulkData(p.modelData, p.light, OverlayTexture.NO_OVERLAY, pose1, vertexConsumer, r, g, b, a);
                });
            }
        }
    }

    private static void putModelBulkData(QuadCollection model, int combinedLight, int combinedOverlay,
                                        PoseStack.Pose pose, VertexConsumer buffer, float r, float g, float b, float a) {
        for (BakedQuad bakedQuad : model.getAll()) {
            buffer.putBulkData(pose, bakedQuad, r, g, b, a, combinedLight, combinedOverlay);
        }
    }


    private record ParticleInstance(float x, float y, float z,
                                    float rx, float ry, float rz, float rw, float size, int color, int light,
                                    QuadCollection modelData) {
    }
}