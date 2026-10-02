package net.mehvahdjukaar.polytone.content.viewpoint;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticles;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderGroup;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderState;
import net.mehvahdjukaar.polytone.content.shaders.ShadowCasterVolume;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import net.mehvahdjukaar.polytone.mixins.accessor.LevelRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
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
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Predicate;

public class ViewpointInstance {

    private static final float PARTICLE_MARGIN = 3f; // same slack as the main pass's particle frustum

    private GpuTexture depthTexture = null;
    private GpuTextureView depthTextureView = null;
    private GpuTexture colorTexture = null;
    private GpuTextureView colorTextureView = null;
    private GpuBuffer projectionBuffer = null;
    private int allocatedResolution = -1;

    private long lastUpdateMs = 0L;
    private boolean hasRendered = false;
    private boolean insidePass = false;

    private ViewpointUniforms uniforms = null;
    private final Matrix4f renderedViewProj = new Matrix4f();
    private final Matrix4f viewProj = new Matrix4f(); // reprojected to the live camera
    private final Vector3f renderedDir = new Vector3f(0, -1, 0);
    private final Vector3f camFract = new Vector3f();
    private Vec3 renderedCamPos = Vec3.ZERO;
    @Nullable
    private ClientLevel renderedLevel = null;
    private float renderedLateralHalf = 0f;
    private float renderedNear = 0f;
    private float renderedFar = 0f;

    private final List<SectionRenderDispatcher.RenderSection> sections = new ArrayList<>();
    private final List<BlockEntity> capturedBlockEntities = new ArrayList<>();

    // render_particles
    private final ViewpointCamera particleCamera = new ViewpointCamera();
    private final QuadParticleRenderState quadParticles = new QuadParticleRenderState();
    private final ModelParticleRenderState modelParticles = new ModelParticleRenderState();

    public GpuTextureView getDepthTexture() {
        return depthTextureView;
    }

    public GpuTextureView getColorTexture() {
        return colorTextureView;
    }

    @Nullable
    public GpuBufferSlice getUniformsSlice() {
        return uniforms == null ? null : uniforms.getSlice();
    }

    // while our own pass draws the textures are attachments and can't be bound
    public boolean isRendering() {
        return insidePass;
    }

    // uniforms are published every frame, reused ones too, so they follow the camera
    public void renderIfNeeded(Viewpoint vp, GpuBufferSlice shaderFog, Camera cam) {
        if (insidePass) return; // a nested level render would clear our section list

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !cam.isInitialized()) return;

        Vec3 camPos = cam.position();
        double interval = vp.updateInterval().evaluate();
        long now = Util.getMillis();

        // the map only covers what was around it when rendered, re-render once we drift too far from it
        float maxDrift = renderedLateralHalf * 0.5f;
        boolean reusable = hasRendered && level == renderedLevel
                && camPos.distanceToSqr(renderedCamPos) <= (double) maxDrift * maxDrift;
        boolean due = !reusable || interval <= 0 || (now - lastUpdateMs) >= interval * 50.0;

        boolean renderedThisFrame = false;
        if (due) {
            insidePass = true;
            boolean ok = true;
            try {
                render(vp, mc, cam, camPos, shaderFog);
            } catch (Exception e) {
                ok = false;
                Polytone.LOGGER.error("Polytone viewpoint render failed", e);
            } finally {
                insidePass = false;
            }
            // don't keep a half drawn map for a whole interval
            hasRendered = ok;
            if (ok) {
                lastUpdateMs = now;
                renderedCamPos = camPos;
                renderedLevel = level;
                renderedThisFrame = true;
            }
        }
        if (!hasRendered) return;

        publishUniforms(camPos, now, interval, renderedThisFrame);
    }

    // shifting by the camera delta since the render is exact, it's just a change of origin
    private void publishUniforms(Vec3 camPos, long now, double interval, boolean renderedThisFrame) {
        viewProj.set(renderedViewProj).translate(
                (float) (camPos.x - renderedCamPos.x),
                (float) (camPos.y - renderedCamPos.y),
                (float) (camPos.z - renderedCamPos.z));
        camFract.set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        float ageSeconds = (now - lastUpdateMs) / 1000f;
        // wall clock like the due check, not game time
        float intervalSeconds = (float) Math.max(interval, 0.0) * 0.05f;
        float phase = intervalSeconds > 0f ? Mth.clamp(ageSeconds / intervalSeconds, 0f, 1f) : 0f;

        if (uniforms == null) uniforms = new ViewpointUniforms();
        uniforms.update(viewProj, renderedDir, camFract, renderedNear, renderedFar,
                allocatedResolution, allocatedResolution, renderedThisFrame, ageSeconds, intervalSeconds, phase);
    }

    private void render(Viewpoint vp, Minecraft mc, Camera cam, Vec3 camPos, GpuBufferSlice shaderFog) {
        ensureTarget(vp.resolution());

        // packs give world coordinates, geometry is camera relative
        Vector3f eye = new Vector3f(
                (float) (vp.x().evaluate() - camPos.x),
                (float) (vp.y().evaluate() - camPos.y),
                (float) (vp.z().evaluate() - camPos.z));

        // minecraft convention, +90 looks down, so c.pitch() points where the player looks
        float pitch = (float) Math.toRadians(vp.xRot().evaluate());
        float yaw = (float) Math.toRadians(vp.yRot().evaluate());
        float roll = (float) Math.toRadians(vp.zRot().evaluate());

        float cosPitch = Mth.cos(pitch);
        Vector3f dir = new Vector3f(-Mth.sin(yaw) * cosPitch, -Mth.sin(pitch), Mth.cos(yaw) * cosPitch);
        // world up is degenerate when looking straight up or down
        Vector3f up = Math.abs(dir.y) > 0.99f ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);

        Matrix4f view = new Matrix4f().rotateZ(roll)
                .lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
                .translate(-eye.x, -eye.y, -eye.z);

        float near = (float) vp.near().evaluate();
        float far = (float) vp.far().evaluate();
        double orthoSize = vp.orthographicSize();

        // reversed-Z like vanilla, the terrain pipelines depth test GREATER
        boolean zZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
        Matrix4f proj;
        float lateralHalf;
        if (orthoSize > 0) {
            lateralHalf = (float) (orthoSize * 0.5);
            proj = new Matrix4f().ortho(-lateralHalf, lateralHalf, -lateralHalf, lateralHalf, far, near, zZeroToOne);
        } else {
            proj = new Matrix4f().perspective((float) Math.toRadians(vp.fov().evaluate()), 1.0f, far, near, zZeroToOne);
            // far plane half width, so the cull is never too tight
            lateralHalf = (float) (far * Math.tan(Math.toRadians(vp.fov().evaluate()) * 0.5));
        }

        proj.mul(view, renderedViewProj);
        renderedDir.set(dir);
        renderedLateralHalf = lateralHalf;
        renderedNear = near;
        renderedFar = far;

        // the box is symmetric so it has to contain an asymmetric near..far span
        float depthHalf = Math.max(Math.abs(near), Math.abs(far));
        ShadowCasterVolume volume = new ShadowCasterVolume(view, lateralHalf, depthHalf);

        collectSections(mc, volume, camPos, eye, vp.terrainLayers());

        GpuDevice device = RenderSystem.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bb = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
                    .putMat4f(proj).get();
            device.createCommandEncoder().writeToBuffer(projectionBuffer.slice(), bb);
        }
        device.createCommandEncoder().clearColorAndDepthTextures(colorTexture, new Vector4f(0, 0, 0, 0), depthTexture, 0.0);

        RenderSystem.setShaderFog(shaderFog);

        if (CompatHandler.SODIUM) {
            capturedBlockEntities.clear();
            // vanilla section meshes are empty under sodium, block entities come from the replay
            SodiumShadowRenderer.replayTerrain(mc, cam, camPos, view, proj,
                    volume, colorTextureView, depthTextureView, capturedBlockEntities);
            if (!vp.renderBlockEntities()) capturedBlockEntities.clear();
        } else {
            drawTerrain(mc, view, vp.terrainLayers());
        }

        if (vp.renderParticles()) {
            particleCamera.setup(cam, view, pitch * Mth.RAD_TO_DEG, yaw * Mth.RAD_TO_DEG);
        }
        if (vp.renderEntities() || vp.renderBlockEntities() || vp.renderParticles()) {
            drawFeatures(vp, mc, camPos, eye, view, volume, orthoSize > 0);
        } else {
            capturedBlockEntities.clear();
        }
    }

    // like ShadowMapRenderer, with this viewpoint's matrices and target swapped in
    private void drawFeatures(Viewpoint vp, Minecraft mc, Vec3 camPos, Vector3f eye,
                              Matrix4f view, ShadowCasterVolume volume, boolean ortho) {
        ClientLevel level = mc.level;
        if (level == null) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        BlockEntityRenderDispatcher beDispatcher = mc.getBlockEntityRenderDispatcher();
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.featureRenderDispatcher();
        CameraRenderState camState = mc.levelRenderer.levelRenderState.cameraRenderState;
        // own storage so a throwing renderer can't leave nodes queued for the main pass
        SubmitNodeStorage submitNodes = new SubmitNodeStorage();

        // all restored in the finally, a leak would render the world from the viewpoint
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer.slice(),
                ortho ? ProjectionType.ORTHOGRAPHIC : ProjectionType.PERSPECTIVE);
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.set(view);
        RenderSystem.outputColorTextureOverride = colorTextureView;
        RenderSystem.outputDepthTextureOverride = depthTextureView;
        mc.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
        try {
            PoseStack poseStack = new PoseStack();

            if (vp.renderEntities()) {
                Entity cameraEntity = vp.renderCameraEntity() ? null : mc.getCameraEntity();
                for (Entity entity : level.entitiesForRendering()) {
                    if (entity.isSpectator() || entity == cameraEntity) continue;
                    AABB bb = entity.getBoundingBox();
                    float radius = (float) Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize()));
                    Vec3 c = bb.getCenter();
                    if (!volume.intersects((float) (c.x - camPos.x) - eye.x,
                            (float) (c.y - camPos.y) - eye.y,
                            (float) (c.z - camPos.z) - eye.z, radius, radius, radius)) continue;

                    float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(
                            !level.tickRateManager().isEntityFrozen(entity));
                    try {
                        EntityRenderState state = dispatcher.extractEntity(entity, partial);
                        // name tags would write depth as a floating slab
                        state.nameTag = null;
                        state.scoreText = null;
                        dispatcher.submit(state, camState, state.x - camPos.x, state.y - camPos.y,
                                state.z - camPos.z, poseStack, submitNodes);
                    } catch (Exception e) {
                        // one broken renderer must not kill the frame
                    }
                }
            }

            if (vp.renderBlockEntities()) {
                float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                for (BlockEntity be : capturedBlockEntities) {
                    BlockPos pos = be.getBlockPos();
                    poseStack.pushPose();
                    poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
                    try {
                        var state = beDispatcher.tryExtractRenderState(be, partial, null, false);
                        if (state != null) {
                            beDispatcher.submit(state, poseStack, submitNodes, camState);
                        }
                    } catch (Exception e) {
                    }
                    poseStack.popPose();
                }
            }

            if (vp.renderParticles()) {
                submitParticles(mc, camPos, eye, volume, submitNodes, camState);
            }

            try {
                featureDispatcher.renderAllFeatures(submitNodes);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error rendering polytone viewpoint features", e);
            }
        } finally {
            capturedBlockEntities.clear();
            quadParticles.clear();
            modelParticles.clear();
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            mvStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    // into our own states, never the groups': those still hold the main pass's particles, which draw after this
    private void submitParticles(Minecraft mc, Vec3 camPos, Vector3f eye, ShadowCasterVolume volume,
                                 SubmitNodeStorage submitNodes, CameraRenderState camState) {
        PolytoneAsyncParticles.awaitTicks();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Predicate<Particle> inView = p -> volume.intersects((float) (p.x - camPos.x) - eye.x,
                (float) (p.y - camPos.y) - eye.y, (float) (p.z - camPos.z) - eye.z,
                PARTICLE_MARGIN, PARTICLE_MARGIN, PARTICLE_MARGIN);

        for (ParticleGroup<?> group : mc.particleEngine.particles.values()) {
            if (group instanceof QuadParticleGroup quads) {
                for (SingleQuadParticle particle : quads.particles) {
                    if (!inView.test(particle)) continue;
                    try {
                        particle.extract(quadParticles, particleCamera, partialTick);
                    } catch (Exception e) {
                        // one broken particle must not kill the frame
                    }
                }
            } else if (group instanceof ModelParticleRenderGroup models) {
                models.extract(modelParticles, inView, particleCamera, partialTick);
            } else if (group instanceof ItemPickupParticleGroup) {
                // fresh state per call, frustum unused
                group.extractRenderState(particleCamera.getCullFrustum(), particleCamera, partialTick)
                        .submit(submitNodes, camState);
            }
            // elder guardian curses are a screen overlay, not world particles
        }
        quadParticles.submit(submitNodes, camState);
        modelParticles.submit(submitNodes, camState);
    }

    // same as ShadowMapRenderer#drawVanillaTerrain
    private void drawTerrain(Minecraft mc, Matrix4f view, List<ChunkSectionLayer> layers) {
        if (sections.isEmpty()) return;

        SectionRenderDispatcher dispatcher =
                ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getSectionRenderDispatcher();
        if (dispatcher == null) return;

        GpuTextureView atlasView = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int atlasW = atlasView.getWidth(0);
        int atlasH = atlasView.getHeight(0);

        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawsPerLayer =
                new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : layers) drawsPerLayer.put(layer, new Int2ObjectOpenHashMap<>());
        List<DynamicUniforms.ChunkSectionInfo> infos = new ArrayList<>();
        int maxIndices = 0;
        long now = Util.getMillis();

        // an upload here would move allocations under the main pass's frozen draws
        dispatcher.lock();
        try {
            for (SectionRenderDispatcher.RenderSection section : sections) {
                SectionMesh mesh = section.getSectionMesh();
                BlockPos origin = section.getRenderOrigin();
                int infoIndex = -1;
                for (ChunkSectionLayer layer : layers) {
                    SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                    SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
                    if (draw == null || slice == null) continue;
                    if (draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) continue;
                    if (infoIndex == -1) {
                        infoIndex = infos.size();
                        infos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(view),
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
                () -> "Polytone viewpoint terrain", colorTextureView, Optional.empty(),
                depthTextureView, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Projection", projectionBuffer.slice()); // after the defaults, last bind wins
            pass.bindTexture("Sampler2", mc.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            for (ChunkSectionLayer layer : layers) {
                pass.setPipeline(layer.pipeline());
                pass.bindTexture("Sampler0", atlasView, sampler);
                for (var draws : drawsPerLayer.get(layer).values()) {
                    if (draws.isEmpty()) continue;
                    pass.drawMultipleIndexed(draws, sharedIndexBuffer, sharedIndexType, List.of("ChunkSection"), slices);
                }
            }
        }
    }

    // centred on the viewpoint, not the camera. only already built meshes can be drawn
    private void collectSections(Minecraft mc, ShadowCasterVolume volume, Vec3 camPos, Vector3f eye,
                                 List<ChunkSectionLayer> layers) {
        sections.clear();
        capturedBlockEntities.clear();
        ViewArea viewArea = ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getViewArea();
        if (viewArea == null) return;

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            SectionMesh mesh = section.getSectionMesh();
            if (!mesh.hasRenderableLayers()) continue;
            boolean any = false;
            for (ChunkSectionLayer layer : layers) {
                if (!mesh.isEmpty(layer)) { any = true; break; }
            }
            if (!any) continue;

            BlockPos origin = section.getRenderOrigin();
            if (volume.intersects((float) (origin.getX() + 8 - camPos.x) - eye.x,
                    (float) (origin.getY() + 8 - camPos.y) - eye.y,
                    (float) (origin.getZ() + 8 - camPos.z) - eye.z, 8f, 8f, 8f)) {
                sections.add(section);
                // vanilla only, under sodium the replay supplies them
                capturedBlockEntities.addAll(mesh.getRenderableBlockEntities());
            }
        }
    }

    private void ensureTarget(int resolution) {
        GpuDevice device = RenderSystem.getDevice();
        if (projectionBuffer == null) {
            projectionBuffer = device.createBuffer(() -> "Polytone viewpoint projection UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        }
        if (depthTexture == null || allocatedResolution != resolution) {
            closeTextures();
            allocatedResolution = resolution;
            depthTexture = device.createTexture(() -> "Polytone viewpoint depth",
                    // clearColorAndDepthTextures needs COPY_DST
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING
                            | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.D32_FLOAT, resolution, resolution, 1, 1);
            depthTextureView = device.createTextureView(depthTexture);
            colorTexture = device.createTexture(() -> "Polytone viewpoint color",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING
                            | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM, resolution, resolution, 1, 1);
            colorTextureView = device.createTextureView(colorTexture);
        }
    }

    private void closeTextures() {
        if (depthTexture != null) {
            depthTextureView.close();
            depthTexture.close();
            colorTextureView.close();
            colorTexture.close();
            depthTexture = null;
            depthTextureView = null;
            colorTexture = null;
            colorTextureView = null;
        }
    }

    public void close() {
        closeTextures();
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
        allocatedResolution = -1;
        hasRendered = false;
        renderedLevel = null;
        renderedLateralHalf = 0f;
        sections.clear();
        capturedBlockEntities.clear();
        particleCamera.reset(); // drops the entity
    }
}
