package net.mehvahdjukaar.polytone.content.shaders.shadow;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.shadow.sodium.SodiumShadowRenderer;
import net.mehvahdjukaar.polytone.mixins.accessor.LevelRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

// Replays the level's already-compiled chunk meshes from the light's point of view; nothing is re-meshed
public class ShadowCasterRenderer {

    private static final ChunkSectionLayer[] SHADOW_LAYERS = {ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT};

    public static void render(Minecraft mc, ClientLevel level, Camera cam, float partialTick, ShadowMap map,
                              ShadowMapSettings settings, Matrix4f cameraViewProjection, GpuBufferSlice shaderFog) {
        Vec3 camPos = cam.position();
        map.updateLightMatrices(cam, camPos, partialTick, settings);

        ShadowCasterVolume volume = new ShadowCasterVolume(map.lightView(), settings.coverage(), settings.depthRange());

        boolean rendersEveryFrame = settings.updateInterval() <= 0f;
        if (rendersEveryFrame) {
            volume.buildCasterPlanes(cameraViewProjection, map.towardLight());
        }

        List<BlockEntity> casterBlockEntities = new ArrayList<>();
        List<SectionRenderDispatcher.RenderSection> casterSections = collectCasterSections(mc, volume, camPos, casterBlockEntities);

        map.uploadProjectionAndClearDepth();
        RenderSystem.setShaderFog(shaderFog);

        if (CompatHandler.SODIUM) {
            //vanilla sections are uncompiled under sodium, it owns the terrain and the block entities
            SodiumShadowRenderer.replayTerrain(mc, cam, camPos, map.lightView(), map.lightProj(), volume,
                    map.colorTexture(), map.depthTexture(), casterBlockEntities);
        } else {
            drawTerrain(mc, camPos, map, casterSections);
        }

        if (settings.renderEntities() || settings.renderBlockEntities()) {
            drawEntities(mc, level, camPos, map, settings, volume, casterBlockEntities);
        }
    }

    private static List<SectionRenderDispatcher.RenderSection> collectCasterSections(Minecraft mc, ShadowCasterVolume volume, Vec3 camPos,
                                                                                     List<BlockEntity> blockEntitiesOut) {
        List<SectionRenderDispatcher.RenderSection> casterSections = new ArrayList<>();
        ViewArea viewArea = ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getViewArea();
        if (viewArea == null) return casterSections;

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            if (!section.getSectionMesh().hasRenderableLayers()) continue;

            BlockPos origin = section.getRenderOrigin();
            if (canCastIntoView(volume, camPos, origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8, 8f)) {
                casterSections.add(section);
                blockEntitiesOut.addAll(section.getSectionMesh().getRenderableBlockEntities());
            }
        }
        return casterSections;
    }

    // vanilla's prepareChunkRenders + renderGroup with the light view as the frustum matrix
    private static void drawTerrain(Minecraft mc, Vec3 camPos, ShadowMap map, List<SectionRenderDispatcher.RenderSection> casterSections) {
        if (casterSections.isEmpty()) return;

        SectionRenderDispatcher dispatcher = ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getSectionRenderDispatcher();
        if (dispatcher == null) return;

        GpuTextureView atlasView = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int atlasW = atlasView.getWidth(0);
        int atlasH = atlasView.getHeight(0);

        //grouped like vanilla's combinedHash, each drawMultipleIndexed needs a single vertex/index buffer
        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawsPerLayer = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : SHADOW_LAYERS) {
            drawsPerLayer.put(layer, new Int2ObjectOpenHashMap<>());
        }
        List<DynamicUniforms.ChunkSectionInfo> infos = new ArrayList<>();
        int maxIndices = 0;
        long now = Util.getMillis();

        //read only! uploadGlobalGeomBuffersToGPU here moves allocations the main pass already built its draws on and chunks flicker
        dispatcher.lock();
        try {
            for (SectionRenderDispatcher.RenderSection section : casterSections) {
                SectionMesh mesh = section.getSectionMesh();
                BlockPos origin = section.getRenderOrigin();
                int infoIndex = -1;
                for (ChunkSectionLayer layer : SHADOW_LAYERS) {
                    SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                    SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
                    if (draw == null || slice == null) continue;
                    if (draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) continue;
                    if (infoIndex == -1) {
                        infoIndex = infos.size();
                        infos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(map.lightView()),
                                origin.getX(), origin.getY(), origin.getZ(),
                                section.getVisibility(now), atlasW, atlasH));
                    }
                    VertexFormat vertexFormat = layer.pipeline().getVertexFormat();
                    GpuBuffer vertexBuffer = slice.vertexBuffer();
                    int bufferGroup = 31 * 173 + vertexBuffer.hashCode();

                    int firstIndex = 0;
                    GpuBuffer indexBuffer;
                    VertexFormat.IndexType indexType;
                    if (!draw.hasCustomIndexBuffer()) {
                        maxIndices = Math.max(maxIndices, draw.indexCount());
                        indexBuffer = null;
                        indexType = null;
                    } else {
                        indexBuffer = slice.indexBuffer();
                        indexType = draw.indexType();
                        bufferGroup = 31 * bufferGroup + indexBuffer.hashCode();
                        bufferGroup = 31 * bufferGroup + indexType.hashCode();
                        firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
                    }
                    int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                    int idx = infoIndex;
                    drawsPerLayer.get(layer).computeIfAbsent(bufferGroup, k -> new ArrayList<>())
                            .add(new RenderPass.Draw<>(0, vertexBuffer, indexBuffer, indexType,
                                    firstIndex, draw.indexCount(), baseVertex,
                                    (slices, uploader) -> uploader.upload("ChunkSection", slices[idx])));
                }
            }
        } finally {
            dispatcher.unlock();
        }
        if (infos.isEmpty()) return;

        GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms()
                .writeChunkSections(infos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));

        RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer sharedIndexBuffer = maxIndices == 0 ? null : sequential.getBuffer(maxIndices);
        VertexFormat.IndexType sharedIndexType = maxIndices == 0 ? null : sequential.type();

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone shadow map terrain", map.colorTextureView(), OptionalInt.empty(),
                map.depthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            //after the defaults, last bind wins
            pass.setUniform("Projection", map.lightProjection());
            pass.bindTexture("Sampler2", mc.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            for (ChunkSectionLayer layer : SHADOW_LAYERS) {
                pass.setPipeline(layer.pipeline());
                pass.bindTexture("Sampler0", atlasView, sampler);
                for (var draws : drawsPerLayer.get(layer).values()) {
                    if (draws.isEmpty()) continue;
                    pass.drawMultipleIndexed(draws, sharedIndexBuffer, sharedIndexType, List.of("ChunkSection"), slices);
                }
            }
        }
    }

    private static void drawEntities(Minecraft mc, ClientLevel level, Vec3 camPos, ShadowMap map,
                                     ShadowMapSettings settings, ShadowCasterVolume volume,
                                     List<BlockEntity> casterBlockEntities) {
        EntityRenderDispatcher entityDispatcher = mc.getEntityRenderDispatcher();
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
        CameraRenderState camState = mc.levelRenderer.levelRenderState.cameraRenderState;

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(map.lightProjection(), ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.set(map.lightView());
        RenderSystem.outputColorTextureOverride = map.colorTextureView();
        RenderSystem.outputDepthTextureOverride = map.depthTextureView();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
        try {
            PoseStack poseStack = new PoseStack();
            if (settings.renderEntities()) {
                for (Entity entity : level.entitiesForRendering()) {
                    if (entity.isSpectator()) continue;

                    AABB box = entity.getBoundingBox();
                    float radius = (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
                    Vec3 center = box.getCenter();
                    if (!canCastIntoView(volume, camPos, center.x, center.y, center.z, radius)) continue;

                    float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(
                            !level.tickRateManager().isEntityFrozen(entity));
                    try {
                        EntityRenderState state = entityDispatcher.extractEntity(entity, partialTick);
                        //name tags can cask a shadow!
                        state.nameTag = null;
                        state.scoreText = null;
                        entityDispatcher.submit(state, camState, state.x - camPos.x, state.y - camPos.y,
                                state.z - camPos.z, poseStack, featureDispatcher.getSubmitNodeStorage());
                    } catch (Exception e) {
                        // one broken entity renderer must not kill the frame
                    }
                }
            }

            if (settings.renderBlockEntities()) {
                submitBlockEntities(mc, featureDispatcher, camState, camPos, poseStack, casterBlockEntities);
            }

            renderFeaturesSafely(mc, featureDispatcher);
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void submitBlockEntities(Minecraft mc, FeatureRenderDispatcher featureDispatcher, CameraRenderState camState,
                                            Vec3 camPos, PoseStack poseStack, List<BlockEntity> casterBlockEntities) {
        BlockEntityRenderDispatcher blockEntityDispatcher = mc.getBlockEntityRenderDispatcher();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (BlockEntity blockEntity : casterBlockEntities) {
            BlockPos pos = blockEntity.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
            try {
                var state = blockEntityDispatcher.tryExtractRenderState(blockEntity, partialTick, null);
                if (state != null) {
                    blockEntityDispatcher.submit(state, poseStack, featureDispatcher.getSubmitNodeStorage(), camState);
                }
            } catch (Exception ignored) {
            }
            poseStack.popPose();
        }
    }

    //dont let light pov stuff leak into the main pass. outline and crumbling buffers aren't flushed by endBatch
    private static void renderFeaturesSafely(Minecraft mc, FeatureRenderDispatcher featureDispatcher) {
        try {
            featureDispatcher.renderAllFeatures();
        } catch (Exception e) {
            Polytone.LOGGER.error("Error rendering polytone shadow features", e);
            try {
                featureDispatcher.getSubmitNodeStorage().clear();
            } catch (Exception ignored) {
            }
        }
        RenderBuffers buffers = mc.renderBuffers();
        MultiBufferSource.BufferSource bufferSource = buffers.bufferSource();
        drainSafely(bufferSource::endBatch);
        drainSafely(buffers.outlineBufferSource()::endOutlineBatch);
        drainSafely(buffers.crumblingBufferSource()::endBatch);
    }

    private static void drainSafely(Runnable endBatch) {
        try {
            endBatch.run();
        } catch (Exception e) {
            Polytone.LOGGER.error("Error flushing polytone shadow entity batch", e);
        }
    }

    private static boolean canCastIntoView(ShadowCasterVolume volume, Vec3 camPos,
                                           double x, double y, double z, float halfExtent) {
        return volume.intersects((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z),
                halfExtent, halfExtent, halfExtent);
    }
}
