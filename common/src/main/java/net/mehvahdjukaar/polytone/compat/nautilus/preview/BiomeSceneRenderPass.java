package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.mehvahdjukaar.polytone.content.particle.custom.IRotationProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Draws the biome-modifier diorama offscreen through the game's own block/model path: a stylised sky
 * disk, a grass shore with a small tree and tufts of grass, and a translucent water pool. It isn't the
 * real world renderer - just enough to show the four colours a biome modifier sets, in context.
 *
 * <p>Runs on the render thread inside a nautilus {@code LiveViewport}; matrices/fog/shader-colour are
 * saved and restored by the caller. Geometry is drawn camera-relative (vertices are {@code worldPos -
 * eye}) with the model-view carrying only the orbit rotation, the same scheme {@link ParticleRenderPass}
 * uses, so the orbit camera behaves identically here.
 */
final class BiomeSceneRenderPass {

    enum Tint { NONE, GRASS, FOLIAGE }

    record Placement(BlockPos pos, BlockState state, Tint tint) {}

    record WaterQuad(float minX, float minZ, float maxX, float maxZ, float y) {}

    // sky/fog are RGB; grass/foliage/water are RGB used to tint their models (water also gets an alpha).
    // grassModifier is the biome's grass post-process (dark forest / swamp), applied per grass block.
    record Colors(int sky, int fog, int grass, int foliage, int water, GrassColorModifier grassModifier) {}

    private static final float WATER_ALPHA = 0.72f;

    static void render(SceneCamera camera, int width, int height, Colors colors,
                       List<Placement> blocks, WaterQuad water) {
        Minecraft mc = Minecraft.getInstance();

        // Orbit basis: eye pushed back from the target along the view direction (see ParticleRenderPass).
        Matrix4fStack orbit = new Matrix4fStack(1);
        orbit.rotateY((float) Math.toRadians(-camera.yawDeg())).rotateX((float) Math.toRadians(-camera.pitchDeg()));
        Vector3f look = orbit.transformDirection(new Vector3f(0f, 0f, -1f), new Vector3f());
        Vector3f eyeOffset = orbit.transformPosition(new Vector3f(0f, 0f, camera.distance()));
        Vector3f target = camera.target();
        float ex = target.x + eyeOffset.x;
        float ey = target.y + eyeOffset.y;
        float ez = target.z + eyeOffset.z;

        Camera cam = new Camera();
        cam.setPosition(new Vec3(ex, ey, ez));
        cam.setRotation(IRotationProvider.getYaw(look), IRotationProvider.getPitch(look));

        RenderSystem.setProjectionMatrix(camera.projection((float) width / height), VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.identity().rotate(new Quaternionf(cam.rotation()).conjugate());
        RenderSystem.applyModelViewMatrix();

        // Opaque fog fill behind everything, so the canvas colour never shows and the sky-disk rim melts
        // into the horizon.
        float[] fog = rgb(colors.fog());
        GlStateManager._clearColor(fog[0], fog[1], fog[2], 1f);
        GlStateManager._clearDepth(1.0);
        GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderFogStart(Integer.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Integer.MAX_VALUE);

        drawSky(colors);
        drawBlocks(mc, blocks, colors, ex, ey, ez);
        drawWater(colors, water, ex, ey, ez);

        // Leave the pipeline in a sane default state for whatever the caller flushes next.
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    // A sky dome centred on the eye: sky colour at the zenith fading to fog toward the horizon, so the
    // colour spreads across the whole upper sky instead of piling into an overhead spot like a flat disk
    // did. Not vanilla's actual sky, just the same silhouette.
    private static void drawSky(Colors colors) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float[] sky = rgb(colors.sky());
        float[] fog = rgb(colors.fog());
        float radius = 200f;
        int rings = 16;
        int segments = 48;
        float topDeg = 90f;
        float bottomDeg = -8f; // dip below the horizon so the dome's edge hides under the fog fill

        for (int i = 0; i < rings; i++) {
            float e0 = (float) Math.toRadians(lerp(topDeg, bottomDeg, i / (float) rings));
            float e1 = (float) Math.toRadians(lerp(topDeg, bottomDeg, (i + 1) / (float) rings));
            float[] c0 = mix(sky, fog, colorT(e0));
            float[] c1 = mix(sky, fog, colorT(e1));
            float y0 = radius * (float) Math.sin(e0), r0 = radius * (float) Math.cos(e0);
            float y1 = radius * (float) Math.sin(e1), r1 = radius * (float) Math.cos(e1);

            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int s = 0; s <= segments; s++) {
                double a = s / (double) segments * Math.PI * 2;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                bb.addVertex(r0 * ca, y0, r0 * sa).setColor(c0[0], c0[1], c0[2], 1f);
                bb.addVertex(r1 * ca, y1, r1 * sa).setColor(c1[0], c1[1], c1[2], 1f);
            }
            draw(bb);
        }
    }

    // 0 at the zenith (sky), 1 at/under the horizon (fog); linear in elevation.
    private static float colorT(float elevationRad) {
        return clamp((90f - (float) Math.toDegrees(elevationRad)) / 90f, 0f, 1f);
    }

    private static void drawBlocks(Minecraft mc, List<Placement> blocks, Colors colors, float ex, float ey, float ez) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        LightTexture lightTexture = mc.gameRenderer.lightTexture();
        lightTexture.turnOnLightLayer();
        Lighting.setupLevel();

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = new PoseStack();

        for (Placement p : blocks) {
            pose.pushPose();
            pose.translate(p.pos().getX() - ex, p.pos().getY() - ey, p.pos().getZ() - ez);
            int color = tintColor(colors, p.tint(), p.pos());
            if (color < 0) {
                dispatcher.renderSingleBlock(p.state(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } else {
                BakedModel model = dispatcher.getBlockModel(p.state());
                float[] c = rgb(color);
                RenderType type = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(p.state(), false);
                dispatcher.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(type), p.state(), model,
                        c[0], c[1], c[2], LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
        buffers.endBatch();

        lightTexture.turnOffLightLayer();
    }

    // A single translucent quad at the pool surface, depth-tested against the terrain but not writing
    // depth, so the shore reads through it like water.
    private static void drawWater(Colors colors, WaterQuad w, float ex, float ey, float ez) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float[] c = rgb(colors.water());
        float y = w.y() - ey;
        float x0 = w.minX() - ex, x1 = w.maxX() - ex;
        float z0 = w.minZ() - ez, z1 = w.maxZ() - ez;

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bb.addVertex(x0, y, z0).setColor(c[0], c[1], c[2], WATER_ALPHA);
        bb.addVertex(x0, y, z1).setColor(c[0], c[1], c[2], WATER_ALPHA);
        bb.addVertex(x1, y, z1).setColor(c[0], c[1], c[2], WATER_ALPHA);
        bb.addVertex(x1, y, z0).setColor(c[0], c[1], c[2], WATER_ALPHA);
        draw(bb);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static int tintColor(Colors colors, Tint tint, BlockPos pos) {
        return switch (tint) {
            case GRASS -> {
                int base = colors.grass();
                GrassColorModifier gcm = colors.grassModifier();
                yield gcm == null || gcm == GrassColorModifier.NONE ? base
                        : gcm.modifyColor(pos.getX(), pos.getZ(), base) & 0xFFFFFF;
            }
            case FOLIAGE -> colors.foliage();
            case NONE -> -1;
        };
    }

    private static float[] rgb(int color) {
        return new float[]{((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f};
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static void draw(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
}
