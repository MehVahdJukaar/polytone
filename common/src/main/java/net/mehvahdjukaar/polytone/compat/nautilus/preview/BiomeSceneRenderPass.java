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

    record WaterQuad(float minX, float minZ, float maxX, float maxZ, float y, float floorY) {}

    // sky/fog are RGB; grass/foliage/water are RGB used to tint their models (water also gets an alpha).
    // grassModifier is the biome's grass post-process (dark forest / swamp), applied per grass block.
    record Colors(int sky, int fog, int grass, int foliage, int water, GrassColorModifier grassModifier) {}

    private static final float WATER_ALPHA = 0.72f;
    // The sky disk hovers this high above the eye; being flat, its points map to elevation atan(h/rho),
    // so the height sets how much of the sky the fog band below can span (too low -> a sliver at the horizon).
    private static final float SKY_DISK_HEIGHT = 16f;
    private static final float SKY_DISK_RADIUS = 512f; // must exceed SKY_FOG_END so the rim is fully fogged
    // Distances over which the disk fades to fog: with h=16 this is a gradient from ~30 deg elevation down
    // to the horizon.
    private static final float SKY_FOG_START = 32f;
    private static final float SKY_FOG_END = 200f;

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

    // Vanilla-style sky: a flat disk hovering a couple blocks above the eye. Being a flat plane just
    // overhead it fills the whole upper sky, and its far parts recede toward the horizon - which is where
    // fog takes over. The core position_color shader doesn't sample fog, so instead of relying on the fog
    // uniforms we bake vanilla's exact linear_fog blend into the vertex colours across a tessellated disk
    // (colour by distance from the eye), giving the same sky->fog horizon the game's fog shader produces.
    private static void drawSky(Colors colors) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float[] sky = rgb(colors.sky());
        float[] fog = rgb(colors.fog());
        float h = SKY_DISK_HEIGHT;
        float rMax = SKY_DISK_RADIUS;
        int rings = 40;
        int segments = 48;

        float prevR = 0f;
        float[] prevC = fogMix(sky, fog, (float) Math.sqrt(prevR * prevR + h * h));
        for (int i = 1; i <= rings; i++) {
            float rho = rMax * i / (float) rings;
            float[] c = fogMix(sky, fog, (float) Math.sqrt(rho * rho + h * h));
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int s = 0; s <= segments; s++) {
                double a = s / (double) segments * Math.PI * 2;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                bb.addVertex(prevR * ca, h, prevR * sa).setColor(prevC[0], prevC[1], prevC[2], 1f);
                bb.addVertex(rho * ca, h, rho * sa).setColor(c[0], c[1], c[2], 1f);
            }
            draw(bb);
            prevR = rho;
            prevC = c;
        }
    }

    // Vanilla linear_fog: fully sky within fogStart, fully fog past fogEnd, linear between.
    private static float[] fogMix(float[] sky, float[] fog, float dist) {
        float fade = dist <= SKY_FOG_START ? 1f
                : dist >= SKY_FOG_END ? 0f
                : (SKY_FOG_END - dist) / (SKY_FOG_END - SKY_FOG_START);
        return mix(sky, fog, 1f - fade);
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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

    // The pool surface plus the side faces on its exposed outer edges (the +x / +z diorama boundary),
    // so the water reads as a body with depth, not a decal. Depth-tested against the terrain but not
    // writing depth, so the dirt bed shows through it like water.
    private static void drawWater(Colors colors, WaterQuad w, float ex, float ey, float ez) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float[] c = rgb(colors.water());
        float top = w.y() - ey;
        float floor = w.floorY() - ey;
        float x0 = w.minX() - ex, x1 = w.maxX() - ex;
        float z0 = w.minZ() - ez, z1 = w.maxZ() - ez;
        float shade = 0.8f; // sides a touch darker so the edge reads as depth
        float sr = c[0] * shade, sg = c[1] * shade, sb = c[2] * shade;

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // Top surface.
        bb.addVertex(x0, top, z0).setColor(c[0], c[1], c[2], WATER_ALPHA);
        bb.addVertex(x0, top, z1).setColor(c[0], c[1], c[2], WATER_ALPHA);
        bb.addVertex(x1, top, z1).setColor(c[0], c[1], c[2], WATER_ALPHA);
        bb.addVertex(x1, top, z0).setColor(c[0], c[1], c[2], WATER_ALPHA);
        // +x edge face.
        bb.addVertex(x1, floor, z0).setColor(sr, sg, sb, WATER_ALPHA);
        bb.addVertex(x1, floor, z1).setColor(sr, sg, sb, WATER_ALPHA);
        bb.addVertex(x1, top, z1).setColor(sr, sg, sb, WATER_ALPHA);
        bb.addVertex(x1, top, z0).setColor(sr, sg, sb, WATER_ALPHA);
        // +z edge face.
        bb.addVertex(x0, floor, z1).setColor(sr, sg, sb, WATER_ALPHA);
        bb.addVertex(x1, floor, z1).setColor(sr, sg, sb, WATER_ALPHA);
        bb.addVertex(x1, top, z1).setColor(sr, sg, sb, WATER_ALPHA);
        bb.addVertex(x0, top, z1).setColor(sr, sg, sb, WATER_ALPHA);
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

    private static void draw(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
}
