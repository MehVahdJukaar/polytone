package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

/**
 * Draws one ticked {@link CustomParticleInstance} offscreen through the game's own particle path, so
 * blend modes, billboarding and animated sprites match the runtime exactly. Runs on the render thread
 * inside a nautilus OffscreenContent; matrices/fog/shader-colour are
 * saved and restored by the caller.
 *
 * <p>Particles render camera-relative (vertices are {@code worldPos - cameraEye}), so the model-view
 * is the orbit's rotation alone while the throwaway {@link Camera} carries the eye position that
 * bakes in the orbit distance. Model particles (render type {@code CUSTOM}, whose buffer is null) have
 * no live preview yet.
 */
final class ParticleRenderPass {

    static void render(CustomParticleInstance particle, SceneCamera camera, Vec3 target, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        float yaw = camera.yawDeg();
        float pitch = camera.pitchDeg();
        float distance = camera.distance();

        RenderSystem.setProjectionMatrix(camera.projection((float) width / height), VertexSorting.DISTANCE_TO_ORIGIN);

        // Model-view = rotation only (same composition as SceneCamera.view, minus the translations).
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.identity()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw));
        RenderSystem.applyModelViewMatrix();

        // Eye in world space: target offset back along the inverse view rotation by the orbit distance.
        Vector3f eyeOffset = new Matrix4f()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(-pitch))
                .transformPosition(new Vector3f(0f, 0f, distance));
        Vec3 eye = new Vec3(target.x + eyeOffset.x, target.y + eyeOffset.y, target.z + eyeOffset.z);

        Camera cam = new Camera();
        cam.setPosition(eye);
        // Billboard orientation. If quads face away at some angles, the sign convention here is the knob.
        cam.setRotation(yaw, pitch);

        LightTexture lightTexture = mc.gameRenderer.lightTexture();
        lightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderFogStart(Integer.MAX_VALUE); // no distance fog fading the subject out
        RenderSystem.setShaderFogEnd(Integer.MAX_VALUE);
        Lighting.setupLevel(); // world-space diffuse, needed by the block models of model particles

        // Reference grid + axis cross. Also a sanity check: it draws with the same matrices via the
        // plainest shader, so if THIS shows but the particle doesn't, the problem is the particle draw.
        drawReference(target, eye);

        ParticleRenderType renderType = particle.getRenderType();
        if (renderType != ParticleRenderType.NO_RENDER) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            Tesselator tesselator = Tesselator.getInstance();
            // CUSTOM (model particles) has a null buffer: they draw into the deferred block buffers
            // below instead. The additive-translucent flat mode also redirects there, so either way
            // the tesselator batch is drawn first, then the deferred buffers are flushed.
            BufferBuilder builder = renderType.begin(tesselator, mc.getTextureManager());
            VertexConsumer consumer = builder != null ? builder
                    : PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(RenderType.cutout());
            CustomParticleInstance.PREVIEW_FORCE_FULL_PATH = true; // keep the quad out of Sodium's batch
            try {
                particle.render(consumer, cam, 1.0f);
            } finally {
                CustomParticleInstance.PREVIEW_FORCE_FULL_PATH = false;
            }
            if (builder != null) {
                MeshData mesh = builder.build();
                if (mesh != null) {
                    BufferUploader.drawWithShader(mesh);
                }
            }
        }

        // Flush the deferred block/additive geometry (model particles + additive-translucent quads).
        PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.endBatches();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }

    // A flat grid on the subject's plane plus an X/Y/Z axis cross at its centre, drawn camera-relative
    // (positions minus the eye) with the position-colour shader - the simplest thing that can appear.
    private static void drawReference(Vec3 target, Vec3 eye) {
        float cx = (float) (target.x - eye.x);
        float cy = (float) (target.y - eye.y);
        float cz = (float) (target.z - eye.z);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(1.5f);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float span = 2f;
        int lines = 8;
        float step = span * 2f / lines;
        float gy = cy - 0.75f; // a touch below the subject
        float g = 0.4f;
        for (int i = 0; i <= lines; i++) {
            float o = -span + i * step;
            builder.addVertex(cx - span, gy, cz + o).setColor(g, g, g, 1f);
            builder.addVertex(cx + span, gy, cz + o).setColor(g, g, g, 1f);
            builder.addVertex(cx + o, gy, cz - span).setColor(g, g, g, 1f);
            builder.addVertex(cx + o, gy, cz + span).setColor(g, g, g, 1f);
        }
        // Axis cross at the subject centre: X red, Y green, Z blue.
        float a = 0.6f;
        builder.addVertex(cx, cy, cz).setColor(1f, 0.2f, 0.2f, 1f);
        builder.addVertex(cx + a, cy, cz).setColor(1f, 0.2f, 0.2f, 1f);
        builder.addVertex(cx, cy, cz).setColor(0.2f, 1f, 0.2f, 1f);
        builder.addVertex(cx, cy + a, cz).setColor(0.2f, 1f, 0.2f, 1f);
        builder.addVertex(cx, cy, cz).setColor(0.3f, 0.4f, 1f, 1f);
        builder.addVertex(cx, cy, cz + a).setColor(0.3f, 0.4f, 1f, 1f);

        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }
}
