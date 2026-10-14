package net.mehvahdjukaar.polytone.content.shaders.shadow;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
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
import net.minecraft.client.renderer.SubmitNodeStorage;
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
import java.util.Optional;
import java.util.OptionalDouble;

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
                    map.colorTextureView(), map.depthTextureView(), casterBlockEntities);
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
                    VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
                    GpuBuffer vertexBuffer = slice.vertexBuffer();
                    int bufferGroup = 31 * 173 + vertexBuffer.hashCode();

                    int firstIndex = 0;
                    GpuBuffer indexBuffer;
                    IndexType indexType;
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

        RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer sharedIndexBuffer = maxIndices == 0 ? null : sequential.getBuffer(maxIndices);
        IndexType sharedIndexType = maxIndices == 0 ? null : sequential.type();

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone shadow map terrain", map.colorTextureView(), Optional.empty(),
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
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.featureRenderDispatcher();
        CameraRenderState camState = mc.levelRenderer.levelRenderState.cameraRenderState;
        SubmitNodeStorage submitNodes = new SubmitNodeStorage();

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(map.lightProjection(), ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.set(map.lightView());
        RenderSystem.outputColorTextureOverride = map.colorTextureView();
        RenderSystem.outputDepthTextureOverride = map.depthTextureView();
        mc.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
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
                                state.z - camPos.z, poseStack, submitNodes);
                    } catch (Exception e) {
                        // one broken entity renderer must not kill the frame
                    }
                }
            }

            if (settings.renderBlockEntities()) {
                submitBlockEntities(mc, submitNodes, camState, camPos, poseStack, casterBlockEntities);
            }

            try {
                featureDispatcher.renderAllFeatures(submitNodes);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error rendering polytone shadow features", e);
            }
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void submitBlockEntities(Minecraft mc, SubmitNodeStorage submitNodes, CameraRenderState camState,
                                            Vec3 camPos, PoseStack poseStack, List<BlockEntity> casterBlockEntities) {
        BlockEntityRenderDispatcher blockEntityDispatcher = mc.getBlockEntityRenderDispatcher();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (BlockEntity blockEntity : casterBlockEntities) {
            BlockPos pos = blockEntity.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
            try {
                var state = blockEntityDispatcher.tryExtractRenderState(blockEntity, partialTick, null, false);
                if (state != null) {
                    blockEntityDispatcher.submit(state, poseStack, submitNodes, camState);
                }
            } catch (Exception ignored) {
            }
            poseStack.popPose();
        }
    }

    private static boolean canCastIntoView(ShadowCasterVolume volume, Vec3 camPos,
                                           double x, double y, double z, float halfExtent) {
        return volume.intersects((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z),
                halfExtent, halfExtent, halfExtent);
    }
}
