package net.mehvahdjukaar.polytone.content.shaders.shadow;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.PolytoneCoreShaders;
import net.mehvahdjukaar.polytone.content.shaders.shadow.sodium.SodiumShadowRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Replays the level's already-compiled chunk VBOs from the light's point of view; nothing is re-meshed
public class ShadowCasterRenderer {

    public static void render(Minecraft mc, ClientLevel level, Camera cam, float partialTick, ShadowMap map,
                       ShadowMapSettings settings, Matrix4f cameraViewProjection) {
        Vec3 camPos = cam.getPosition();
        map.updateLightMatrices(level, camPos, partialTick, settings);

        ShadowCasterVolume volume = new ShadowCasterVolume(map.lightView(), settings.coverage(), settings.depthRange());

        boolean rendersEveryFrame = settings.updateInterval() <= 0f;
        if (rendersEveryFrame) {
            volume.buildCasterPlanes(cameraViewProjection, map.towardLight());
        }

        List<SectionRenderDispatcher.RenderSection> casterSections = collectCasterSections(mc, volume, camPos);

        RenderTarget mainTarget = mc.getMainRenderTarget();
        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting savedSorting = RenderSystem.getVertexSorting();

        map.bindWriteAndClearDepth();
        try {
            if (!casterSections.isEmpty()) {
                drawTerrainLayer(mc, RenderType.solid(), camPos, map, casterSections, false);
                drawTerrainLayer(mc, RenderType.cutoutMipped(), camPos, map, casterSections, true);
                drawTerrainLayer(mc, RenderType.cutout(), camPos, map, casterSections, true);
            } else if (CompatHandler.SODIUM) {
                SodiumShadowRenderer.replayTerrain(mc, cam, camPos, map.lightView(), map.lightProj(), volume);
            }

            if (settings.renderEntities() || settings.renderBlockEntities()) {
                drawEntities(mc, level, camPos, map, settings, volume, casterSections);
            }
        } finally {
            mainTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static List<SectionRenderDispatcher.RenderSection> collectCasterSections(Minecraft mc, ShadowCasterVolume volume, Vec3 camPos) {
        List<SectionRenderDispatcher.RenderSection> casterSections = new ArrayList<>();
        ViewArea viewArea = mc.levelRenderer.viewArea;
        if (viewArea == null) return casterSections;

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (compiled == SectionRenderDispatcher.CompiledSection.UNCOMPILED || compiled.hasNoRenderableLayers()) continue;

            BlockPos origin = section.getOrigin();
            if (canCastIntoView(volume, camPos, origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8, 8f)) {
                casterSections.add(section);
            }
        }
        return casterSections;
    }

    private static void drawTerrainLayer(Minecraft mc, RenderType renderType, Vec3 camPos, ShadowMap map,
                                         List<SectionRenderDispatcher.RenderSection> casterSections, boolean cutout) {
        renderType.setupRenderState();
        ShaderInstance shader = cutout ? PolytoneCoreShaders.shadowTerrainCutout : PolytoneCoreShaders.shadowTerrain;
        try {
            shader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, map.lightView(), map.lightProj(), mc.getWindow());
            shader.apply();
            Uniform chunkOffset = shader.CHUNK_OFFSET;

            for (SectionRenderDispatcher.RenderSection section : casterSections) {
                if (section.getCompiled().isEmpty(renderType)) continue;
                if (chunkOffset != null) {
                    BlockPos origin = section.getOrigin();
                    chunkOffset.set((float) (origin.getX() - camPos.x), (float) (origin.getY() - camPos.y), (float) (origin.getZ() - camPos.z));
                    chunkOffset.upload();
                }
                VertexBuffer buffer = section.getBuffer(renderType);
                buffer.bind();
                buffer.draw();
            }

            if (chunkOffset != null) chunkOffset.set(0f, 0f, 0f);
        } finally {
            shader.clear();
            VertexBuffer.unbind();
            renderType.clearRenderState();
        }
    }

    private static void drawEntities(Minecraft mc, ClientLevel level, Vec3 camPos, ShadowMap map,
                                     ShadowMapSettings settings, ShadowCasterVolume volume,
                                     List<SectionRenderDispatcher.RenderSection> casterSections) {
        var entityDispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            modelViewStack.mul(map.lightView());
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(map.lightProj(), VertexSorting.ORTHOGRAPHIC_Z);

            PoseStack poseStack = new PoseStack();
            if (settings.renderEntities()) {
                TickRateManager tickRate = level.tickRateManager();
                float runningPartialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
                float frozenPartialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);

                for (Entity entity : level.entitiesForRendering()) {
                    if (entity.isSpectator()) continue;

                    AABB box = entity.getBoundingBox();
                    float radius = (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
                    if (!canCastIntoView(volume, camPos, (box.minX + box.maxX) * 0.5,
                            (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5, radius)) continue;

                    float partialTick = tickRate.isEntityFrozen(entity) ? frozenPartialTick : runningPartialTick;
                    Vec3 pos = entity.getPosition(partialTick);
                    try {
                        // fullbright: only depth matters, skip the per-entity light lookup
                        entityDispatcher.render(entity, pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z,
                                entity.getViewYRot(partialTick), partialTick, poseStack, bufferSource,
                                LightTexture.FULL_BRIGHT);
                    } catch (Exception e) {
                        // one broken entity renderer must not kill the frame
                    }
                }
            }

            if (settings.renderBlockEntities()) {
                drawBlockEntities(mc, level, camPos, settings, bufferSource, poseStack, volume, casterSections);
            }

            try {
                bufferSource.endBatch();
            } catch (Exception e) {
                Polytone.LOGGER.error("Error flushing polytone shadow entity batch", e);
            }
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void drawBlockEntities(Minecraft mc, ClientLevel level, Vec3 camPos, ShadowMapSettings settings,
                                          MultiBufferSource bufferSource, PoseStack poseStack, ShadowCasterVolume volume,
                                          List<SectionRenderDispatcher.RenderSection> casterSections) {
        BlockEntityRenderDispatcher blockEntityDispatcher = mc.getBlockEntityRenderDispatcher();
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        float radius = 1.5f; // most fit in a block, slack for taller ones like beds and chests

        if (!casterSections.isEmpty()) {
            for (SectionRenderDispatcher.RenderSection section : casterSections) {
                for (BlockEntity blockEntity : section.getCompiled().getRenderableBlockEntities()) {
                    renderBlockEntity(blockEntityDispatcher, blockEntity, camPos, bufferSource, poseStack,
                            volume, radius, partialTick);
                }
            }
            return;
        }

        //Sodium path
        int camChunkX = Mth.floor(camPos.x) >> 4;
        int camChunkZ = Mth.floor(camPos.z) >> 4;
        int chunkRadius = Mth.ceil(settings.coverage() / 16f) + 1;

        for (int cx = camChunkX - chunkRadius; cx <= camChunkX + chunkRadius; cx++) {
            for (int cz = camChunkZ - chunkRadius; cz <= camChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    renderBlockEntity(blockEntityDispatcher, entry.getValue(), camPos, bufferSource, poseStack,
                            volume, radius, partialTick);
                }
            }
        }
    }

    private static void renderBlockEntity(BlockEntityRenderDispatcher blockEntityDispatcher, BlockEntity blockEntity,
                                          Vec3 camPos, MultiBufferSource bufferSource, PoseStack poseStack,
                                          ShadowCasterVolume volume, float radius, float partialTick) {
        BlockPos pos = blockEntity.getBlockPos();
        if (!canCastIntoView(volume, camPos, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius)) return;

        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
        try {
            blockEntityDispatcher.render(blockEntity, partialTick, poseStack, bufferSource);
        } catch (Exception ignored) {
        }
        poseStack.popPose();
    }

    private static boolean canCastIntoView(ShadowCasterVolume volume, Vec3 camPos,
                                           double x, double y, double z, float halfExtent) {
        return volume.intersects((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z),
                halfExtent, halfExtent, halfExtent);
    }
}
