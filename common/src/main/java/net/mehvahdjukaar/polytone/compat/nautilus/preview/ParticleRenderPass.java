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
import net.mehvahdjukaar.polytone.content.particle.custom.IRotationProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

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

    static void render(CustomParticleInstance particle, List<Particle> children,
                       SceneCamera camera, Vec3 target, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        float yaw = camera.yawDeg();
        float pitch = camera.pitchDeg();
        float distance = camera.distance();

        RenderSystem.setProjectionMatrix(camera.projection((float) width / height), VertexSorting.DISTANCE_TO_ORIGIN);

        // The orbit basis: look direction from the eye toward the target, and the eye itself (target
        // pushed back along -look by the orbit distance).
        Matrix4f orbitRot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(-pitch));
        Vector3f look = orbitRot.transformDirection(new Vector3f(0f, 0f, -1f), new Vector3f());
        Vector3f eyeOffset = orbitRot.transformPosition(new Vector3f(0f, 0f, distance));
        Vec3 eye = new Vec3(target.x + eyeOffset.x, target.y + eyeOffset.y, target.z + eyeOffset.z);

        // Build a fully consistent camera: orient it to actually look along `look`, so every derived
        // vector (rotation, forwards, up, left) agrees. MOVEMENT_ALIGNED / facing modes read
        // getLookVector(); a mismatched one there rolled the quad away from the viewer.
        Camera cam = new Camera();
        cam.setPosition(eye);
        cam.setRotation(IRotationProvider.getYaw(look), IRotationProvider.getPitch(look));

        // Model-view = the camera's view rotation = conjugate of its world orientation. This keeps it
        // consistent with camera.rotation() by construction, so LOOK_AT_* (which copies rotation())
        // still cancels to a flat screen-facing quad, and the look vectors stay correct.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.identity().rotate(new Quaternionf(cam.rotation()).conjugate());
        RenderSystem.applyModelViewMatrix();

        LightTexture lightTexture = mc.gameRenderer.lightTexture();
        lightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderFogStart(Integer.MAX_VALUE); // no distance fog fading the subject out
        RenderSystem.setShaderFogEnd(Integer.MAX_VALUE);
        Lighting.setupLevel(); // world-space diffuse, needed by the block models of model particles

        // Reference grid + axis cross under the subject, drawn with the plainest shader.
        drawReference(target, eye);

        // Emitted children first, then the subject on top. Sodium's fast path is forced off for the
        // whole batch so every quad lands in our buffer instead of Sodium's.
        CustomParticleInstance.PREVIEW_FORCE_FULL_PATH = true;
        try {
            for (Particle child : children) {
                drawParticle(child, cam, mc);
            }
            drawParticle(particle, cam, mc);
        } finally {
            CustomParticleInstance.PREVIEW_FORCE_FULL_PATH = false;
        }

        // Flush the deferred block/additive geometry (model particles + additive-translucent quads) to
        // OUR bound offscreen target. Note: endBatches() force-binds the game's main target, so it would
        // draw onto the live screen instead of the preview - the plain endBatch() honours the current FB.
        PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.endBatch();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }

    private static void drawParticle(Particle particle, Camera cam, Minecraft mc) {
        ParticleRenderType renderType = particle.getRenderType();
        if (renderType == ParticleRenderType.NO_RENDER) return;
        RenderSystem.setShader(GameRenderer::getParticleShader);
        Tesselator tesselator = Tesselator.getInstance();
        // CUSTOM (model particles) has a null buffer: they draw into the deferred block buffers, flushed
        // by the caller. Additive-translucent flat also redirects there; either way the tesselator batch
        // is drawn now, the deferred ones after all particles.
        BufferBuilder builder = renderType.begin(tesselator, mc.getTextureManager());
        VertexConsumer consumer = builder != null ? builder
                : PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(RenderType.cutout());
        particle.render(consumer, cam, 1.0f);
        if (builder != null) {
            MeshData mesh = builder.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        }
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
