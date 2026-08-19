package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

final class BiomeSceneRenderPass {

    enum Tint { NONE, GRASS, FOLIAGE }

    record Placement(BlockPos pos, BlockState state, Tint tint) {}

    record WaterQuad(float minX, float minZ, float maxX, float maxZ, float y, float floorY) {}

    record Colors(int sky, int fog, int grass, int foliage, int water, GrassColorModifier grassModifier) {}

    private static final float WATER_ALPHA = 0.72f;
    private static final float SKY_DISK_HEIGHT = 16f;
    private static final float SKY_DISK_RADIUS = 512f;
    private static final float SKY_FOG_START = 32f;
    private static final float SKY_FOG_END = 200f;
    private static final float BACKDROP_RADIUS = 480f;

    private static PerspectiveProjectionMatrixBuffer projectionBuffer;

    static void render(SceneCamera camera, int width, int height, Colors colors,
                       List<Placement> blocks, WaterQuad water) {
        Minecraft mc = Minecraft.getInstance();

        // Eye world position from the orbit camera, so the sky/backdrop can follow it (skybox style).
        Vector3f target = camera.target();
        Matrix4f orbit = new Matrix4f()
                .rotateY((float) Math.toRadians(-camera.yawDeg()))
                .rotateX((float) Math.toRadians(-camera.pitchDeg()));
        Vector3f eyeOffset = orbit.transformPosition(new Vector3f(0f, 0f, camera.distance()));
        float ex = target.x + eyeOffset.x;
        float ey = target.y + eyeOffset.y;
        float ez = target.z + eyeOffset.z;

        if (projectionBuffer == null) projectionBuffer = new PerspectiveProjectionMatrixBuffer("polytone biome scene");
        RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(camera.projection((float) width / height)),
                ProjectionType.PERSPECTIVE);

        PoseStack pose = new PoseStack();
        pose.mulPose(camera.view());

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        drawBackdropAndSky(pose, buffers, colors, ex, ey, ez);
        drawWater(pose, buffers, colors, water);
        buffers.endBatch(); // flush the coloured (sky/fog/water) geometry

        drawBlocks(mc, pose, colors, blocks);
    }

    private static void drawBackdropAndSky(PoseStack pose, MultiBufferSource.BufferSource buffers, Colors colors,
                                           float ex, float ey, float ez) {
        VertexConsumer c = buffers.getBuffer(RenderTypes.debugQuads());
        PoseStack.Pose p = pose.last();
        float[] sky = rgb(colors.sky());
        float[] fog = rgb(colors.fog());

        // Fog sphere (viewed from inside; debugQuads doesn't cull) centered on the eye.
        int rings = 12, segs = 24;
        for (int i = 0; i < rings; i++) {
            float t0 = (float) i / rings, t1 = (float) (i + 1) / rings;
            float phi0 = (float) (Math.PI * (t0 - 0.5)), phi1 = (float) (Math.PI * (t1 - 0.5));
            float y0 = (float) Math.sin(phi0), r0 = (float) Math.cos(phi0);
            float y1 = (float) Math.sin(phi1), r1 = (float) Math.cos(phi1);
            for (int s = 0; s < segs; s++) {
                double a0 = s / (double) segs * Math.PI * 2, a1 = (s + 1) / (double) segs * Math.PI * 2;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                quad(c, p, fog,
                        ex + BACKDROP_RADIUS * r0 * c0, ey + BACKDROP_RADIUS * y0, ez + BACKDROP_RADIUS * r0 * s0,
                        ex + BACKDROP_RADIUS * r0 * c1, ey + BACKDROP_RADIUS * y0, ez + BACKDROP_RADIUS * r0 * s1,
                        ex + BACKDROP_RADIUS * r1 * c1, ey + BACKDROP_RADIUS * y1, ez + BACKDROP_RADIUS * r1 * s1,
                        ex + BACKDROP_RADIUS * r1 * c0, ey + BACKDROP_RADIUS * y1, ez + BACKDROP_RADIUS * r1 * s0, 1f);
            }
        }

        // Overhead sky disk: colour by distance from the eye, baking vanilla's linear sky->fog blend
        // into the vertex colours (the core shader doesn't sample fog).
        float h = SKY_DISK_HEIGHT;
        int diskRings = 40, diskSegs = 48;
        float prevR = 0f;
        for (int i = 1; i <= diskRings; i++) {
            float rho = SKY_DISK_RADIUS * i / (float) diskRings;
            float[] cIn = fogMix(sky, fog, (float) Math.sqrt(prevR * prevR + h * h));
            float[] cOut = fogMix(sky, fog, (float) Math.sqrt(rho * rho + h * h));
            for (int s = 0; s < diskSegs; s++) {
                double a0 = s / (double) diskSegs * Math.PI * 2, a1 = (s + 1) / (double) diskSegs * Math.PI * 2;
                float ca0 = (float) Math.cos(a0), sa0 = (float) Math.sin(a0);
                float ca1 = (float) Math.cos(a1), sa1 = (float) Math.sin(a1);
                float yy = ey + h;
                c.addVertex(p, ex + prevR * ca0, yy, ez + prevR * sa0).setColor(cIn[0], cIn[1], cIn[2], 1f);
                c.addVertex(p, ex + prevR * ca1, yy, ez + prevR * sa1).setColor(cIn[0], cIn[1], cIn[2], 1f);
                c.addVertex(p, ex + rho * ca1, yy, ez + rho * sa1).setColor(cOut[0], cOut[1], cOut[2], 1f);
                c.addVertex(p, ex + rho * ca0, yy, ez + rho * sa0).setColor(cOut[0], cOut[1], cOut[2], 1f);
            }
            prevR = rho;
        }
    }

    private static void drawBlocks(Minecraft mc, PoseStack pose, Colors colors, List<Placement> blocks) {
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Placement pl : blocks) {
            pose.pushPose();
            pose.translate(pl.pos().getX(), pl.pos().getY(), pl.pos().getZ());
            int color = tintColor(colors, pl.tint(), pl.pos());
            if (color < 0) {
                dispatcher.renderSingleBlock(pl.state(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } else {
                BlockStateModel model = dispatcher.getBlockModel(pl.state());
                float[] cc = rgb(color);
                RenderType type = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(pl.state());
                ModelBlockRenderer.renderModel(pose.last(), buffers.getBuffer(type), model,
                        cc[0], cc[1], cc[2], LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
        buffers.endBatch();
    }

    // Pool surface + the exposed outer edge faces, translucent so the dirt bed reads through it.
    private static void drawWater(PoseStack pose, MultiBufferSource.BufferSource buffers, Colors colors, WaterQuad w) {
        VertexConsumer c = buffers.getBuffer(RenderTypes.debugQuads());
        PoseStack.Pose p = pose.last();
        float[] cc = rgb(colors.water());
        float top = w.y(), floor = w.floorY();
        float x0 = w.minX(), x1 = w.maxX(), z0 = w.minZ(), z1 = w.maxZ();
        float shade = 0.8f;
        float sr = cc[0] * shade, sg = cc[1] * shade, sb = cc[2] * shade;

        quad(c, p, cc, x0, top, z0, x0, top, z1, x1, top, z1, x1, top, z0, WATER_ALPHA);          // surface
        quad(c, p, new float[]{sr, sg, sb}, x1, floor, z0, x1, floor, z1, x1, top, z1, x1, top, z0, WATER_ALPHA); // +x face
        quad(c, p, new float[]{sr, sg, sb}, x0, floor, z1, x1, floor, z1, x1, top, z1, x0, top, z1, WATER_ALPHA); // +z face
    }

    private static void quad(VertexConsumer c, PoseStack.Pose p, float[] col,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float alpha) {
        c.addVertex(p, ax, ay, az).setColor(col[0], col[1], col[2], alpha);
        c.addVertex(p, bx, by, bz).setColor(col[0], col[1], col[2], alpha);
        c.addVertex(p, cx, cy, cz).setColor(col[0], col[1], col[2], alpha);
        c.addVertex(p, dx, dy, dz).setColor(col[0], col[1], col[2], alpha);
    }

    private static float[] fogMix(float[] sky, float[] fog, float dist) {
        float fade = dist <= SKY_FOG_START ? 1f
                : dist >= SKY_FOG_END ? 0f
                : (SKY_FOG_END - dist) / (SKY_FOG_END - SKY_FOG_START);
        float t = 1f - fade;
        return new float[]{sky[0] + (fog[0] - sky[0]) * t, sky[1] + (fog[1] - sky[1]) * t, sky[2] + (fog[2] - sky[2]) * t};
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
}
