package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.nautilus.render.OffscreenTargetContext;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.LiveViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.ParticlePreviewState;
import net.mehvahdjukaar.polytone.content.particle.PreviewRenderTarget;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.mehvahdjukaar.polytone.content.particle.custom.ICustomParticleFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderGroup;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ParticlePreview extends ExpressionPreview {

    private final @Nullable Identifier contentId;
    private final @Nullable Path file;

    private final LiveViewport viewport = new LiveViewport();
    private final ParticleScene renderer = new ParticleScene();

    private final JButton playButton = new JButton("Play");
    private final JButton stepButton = new JButton("Step");
    private final JSlider speedSlider = new JSlider(10, 400, 100); // 0.1x .. 4.0x, hundredths
    private final JLabel speedLabel = StyledLabels.mutedSmall("1.0x");

    private final JLabel ageReadout = StyledLabels.mutedSmall(" ");
    private final JLabel motionReadout = StyledLabels.mutedSmall(" ");
    private final JLabel colorReadout = StyledLabels.mutedSmall(" ");

    // The edited type, decoded from the form; read on the render thread so it is published via volatile.
    private volatile @Nullable CustomParticleType type;

    public ParticlePreview(TabPreview.Context ctx) {
        this.contentId = ctx.contentId();
        this.file = ctx.file();

        viewport.setRenderer(renderer);
        viewport.setPanEnabled(true); // orbit, zoom and pan to inspect against the grid

        playButton.addActionListener(e -> setPlaying(!viewport.isPlaying()));
        stepButton.addActionListener(e -> {
            renderer.requestRespawnIfDead();
            viewport.step();
        });
        speedSlider.addChangeListener(e -> {
            renderer.speed = speedSlider.getValue() / 100.0;
            speedLabel.setText(String.format("%.1fx", renderer.speed));
        });

        buildLayout();
        setPlaying(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        viewport.dispose();
        renderer.close();
    }

    private void setPlaying(boolean playing) {
        viewport.setPlaying(playing);
        playButton.setText(playing ? "Pause" : "Play");
    }

    private void buildLayout() {
        Box content = PreviewLayout.column();

        Box transport = Box.createHorizontalBox();
        transport.add(playButton);
        transport.add(Box.createHorizontalStrut(UiScale.small()));
        transport.add(stepButton);
        transport.add(Box.createHorizontalStrut(UiScale.med()));
        transport.add(StyledLabels.small("Speed"));
        transport.add(Box.createHorizontalStrut(UiScale.small()));
        transport.add(speedSlider);
        transport.add(Box.createHorizontalStrut(UiScale.small()));
        transport.add(speedLabel);
        PreviewLayout.add(content, transport);

        PreviewLayout.add(content, liveToggle);
        PreviewLayout.addFilling(content, envGroup());
        content.add(Box.createVerticalStrut(UiScale.med()));

        viewport.setBorder(UiTheme.hairlineBorder());
        PreviewLayout.addFilling(content, new SquareRow(0, UiScale.px(200), UiScale.px(460), viewport));
        content.add(Box.createVerticalStrut(UiScale.small()));

        PreviewLayout.addFilling(content, ageReadout);
        PreviewLayout.addFilling(content, motionReadout);
        PreviewLayout.addFilling(content, colorReadout);

        install(content);
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        CustomParticleType t = value instanceof CustomParticleType ct ? ct : null;
        if (t != null) {
            SpriteSet sprites = borrowSprites();
            if (sprites == null) {
                this.type = null;
                statusText("No baked sprites yet - reload the pack in-game to preview this particle.");
            } else {
                t.setSpriteSet(sprites);
                this.type = t;
                statusText("");
            }
        } else {
            this.type = null;
            if (value instanceof ICustomParticleFactory) {
                statusText("Only fully custom particles have a live preview.");
            }
        }
        if (this.type == null) {
            setPlaying(false);
        } else {
            renderer.requestRespawn();
            if (!viewport.isPlaying()) setPlaying(true);
        }
        viewport.refresh();
    }

    @Override
    protected void onLiveChanged(boolean live) {
        renderer.requestRespawn();
    }

    @Override
    protected void recompute() {
        renderer.requestRespawn();
        viewport.refresh();
    }

    // Sprites come from the pack's already-registered particle of this id (baked into the atlas).
    private @Nullable SpriteSet borrowSprites() {
        Identifier id = PreviewIds.of(contentId, file, "custom_particles");
        if (id == null) return null;
        ICustomParticleFactory live = Polytone.CUSTOM_PARTICLES.customParticleFactories.getValue(id);
        return live instanceof CustomParticleType ct ? ct.getSpriteSet() : null;
    }

    private void updateReadout() {
        ParticleScene.Snapshot s = renderer.snapshot;
        if (s == null) {
            String d = renderer.diag;
            ageReadout.setText(d != null ? d : "(no particle)");
            motionReadout.setText(" ");
            colorReadout.setText(" ");
            return;
        }
        ageReadout.setText(String.format("age %d%s", s.age, s.alive ? "" : "  (dead)"));
        motionReadout.setText(String.format("<html>size %.3f&nbsp;&nbsp;vel %.3f, %.3f, %.3f</html>",
                s.size, s.vx, s.vy, s.vz));
        int rgb = ((int) (s.r * 255) << 16) | ((int) (s.g * 255) << 8) | (int) (s.b * 255);
        colorReadout.setText(String.format("<html>color #%06X&nbsp;&nbsp;alpha %.2f</html>", rgb & 0xFFFFFF, s.a));
    }

    // Render-thread half: owns the ticked instance + its emitter children in a sandbox engine and
    // draws them. LiveViewport calls advance() then render() on the render thread, so instance
    // mutation and read never race the EDT; the HUD reads only the volatile snapshot.
    private final class ParticleScene implements LiveViewport.Renderer {

        private static final int MAX_CHILDREN = 400;

        // Sandbox engine: never ticked (that would route custom particles back into the async batch);
        // only used to build render groups and run the native extract() collect. Lazily created.
        private @Nullable ParticleEngine sandbox;
        private final ParticlesRenderState particlesRenderState = new ParticlesRenderState();
        private final PreviewCamera previewCamera = new PreviewCamera();
        private @Nullable ProjectionMatrixBuffer projectionBuffer;
        private final RandomSource random = RandomSource.create();

        private @Nullable CustomParticleInstance particle;
        private final List<Particle> children = new ArrayList<>();
        private @Nullable Vec3 spawn;
        private volatile boolean respawn = true;
        private double accumulator;
        double speed = 1.0;

        volatile @Nullable Snapshot snapshot;
        volatile @Nullable String diag;

        void requestRespawn() {
            respawn = true;
        }

        void requestRespawnIfDead() {
            CustomParticleInstance p = particle;
            if (p == null || !p.isAlive()) respawn = true;
        }

        @Override
        public void advance() {
            CustomParticleType t = type;
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) {
                particle = null;
                snapshot = null;
                diag = "no world loaded - join a world to preview";
                postReadout();
                return;
            }
            if (t == null) {
                particle = null;
                snapshot = null;
                diag = "no drawable type (borrowed sprites missing - reload the pack in-game?)";
                postReadout();
                return;
            }
            if (respawn || particle == null || !particle.isAlive()) {
                respawn = false;
                spawnParticle(t, level, mc);
            }
            CustomParticleInstance p = particle;
            if (p == null) {
                snapshot = null;
                if (diag == null) diag = "spawn returned null";
                postReadout();
                return;
            }

            accumulator += speed;
            int ticks = (int) accumulator;
            accumulator -= ticks;
            // Preview mode (render thread only): expressions read global.* through the sim proxies,
            // creation runs synchronously, and emitter children land in captureChild, not the world.
            installSim();
            ParticlePreviewState.begin(this::captureChild);
            try {
                for (int i = 0; i < ticks && p.isAlive(); i++) {
                    p.tickSync();
                    tickChildren();
                }
            } finally {
                ParticlePreviewState.end();
                clearSim();
            }
            diag = null;
            capture(p);
            postReadout();
        }

        // Emitter sink: build the child in the sandbox (create-only, so it isn't queued into a live
        // engine) and keep it in our own list so we tick + render it alongside the parent.
        private void captureChild(Level level, ParticleOptions po, double x, double y, double z,
                                  double dx, double dy, double dz) {
            if (children.size() >= MAX_CHILDREN) return;
            ParticleEngine engine = sandbox;
            if (engine == null || !(level instanceof ClientLevel)) return;
            try {
                Particle child = engine.makeParticle(po, x, y, z, dx, dy, dz);
                if (child != null) children.add(child);
            } catch (Exception ignored) {
                // a broken child factory must never kill the preview loop
            }
        }

        private void tickChildren() {
            var it = children.iterator();
            while (it.hasNext()) {
                Particle c = it.next();
                if (!c.isAlive()) {
                    it.remove();
                    continue;
                }
                if (c instanceof CustomParticleInstance custom) custom.tickSync();
                else c.tick();
                if (!c.isAlive()) it.remove();
            }
        }

        private void postReadout() {
            SwingUtilities.invokeLater(ParticlePreview.this::updateReadout);
        }

        private void spawnParticle(CustomParticleType t, ClientLevel level, Minecraft mc) {
            children.clear();
            if (spawn == null) {
                spawn = mc.player != null ? mc.player.getEyePosition().add(mc.player.getLookAngle().scale(2.5))
                        : new Vec3(0, level.getMinY() + 80, 0);
                viewport.frame(new Vector3f((float) spawn.x, (float) spawn.y, (float) spawn.z), 1.5f, true);
            }
            if (sandbox == null) {
                sandbox = new ParticleEngine(level, mc.particleEngine.resourceManager);
            }
            installSim();
            ParticlePreviewState.begin(this::captureChild);
            try {
                Particle p = t.createParticle(null, level, spawn.x, spawn.y, spawn.z, 0, 0, 0, random);
                particle = p instanceof CustomParticleInstance cpi ? cpi : null;
                if (particle == null) diag = "spawn returned null";
            } catch (Exception ex) {
                particle = null;
                diag = "spawn failed: " + ex;
                Polytone.LOGGER.warn("Particle preview spawn failed", ex);
            } finally {
                ParticlePreviewState.end();
                clearSim();
            }
            accumulator = 0;
        }

        @Override
        public void render(SceneCamera camera, int width, int height) {
            if (spawn == null || height <= 0) return;

            // Orbit eye in world space (same derivation as the biome scene pass).
            Vector3f target = camera.target();
            Matrix4f orbit = new Matrix4f()
                    .rotateY((float) Math.toRadians(-camera.yawDeg()))
                    .rotateX((float) Math.toRadians(-camera.pitchDeg()));
            Vector3f eyeOffset = orbit.transformPosition(new Vector3f(0f, 0f, camera.distance()));
            double ex = target.x + eyeOffset.x, ey = target.y + eyeOffset.y, ez = target.z + eyeOffset.z;
            previewCamera.place(new Vec3(ex, ey, ez), camera.yawDeg(), camera.pitchDeg());

            if (projectionBuffer == null)
                projectionBuffer = new ProjectionMatrixBuffer("polytone particle scene");
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(camera.projection((float) width / height)),
                    ProjectionType.PERSPECTIVE);

            // Goes through the RenderType pipeline, which honours the editor's offscreen redirect.
            drawReference(camera);

            CustomParticleInstance p = particle;
            ParticleEngine engine = sandbox;
            if (p == null || engine == null) return;

            // Collect the live particles into fresh groups (no engine.tick(), which would re-enqueue
            // custom particles into the async batch), then run the native extract.
            engine.particles.clear();
            addToGroup(engine, p);
            for (Particle c : children) if (c.isAlive()) addToGroup(engine, c);

            // Frustum wants the rotation-only view matrix: cubeInFrustum already subtracts the prepare()
            // camera position from every AABB, so passing the full camera.view() (which carries the
            // -distance/-target translation) double-counts the offset and the particle culls out after a
            // few degrees of orbit. Rx(pitch)Ry(yaw) is SceneCamera.view() minus its translations.
            Matrix4f frustumRotation = new Matrix4f()
                    .rotateX((float) Math.toRadians(camera.pitchDeg()))
                    .rotateY((float) Math.toRadians(camera.yawDeg()));
            Frustum frustum = new Frustum(frustumRotation, camera.projection((float) width / height));
            frustum.prepare(ex, ey, ez);

            // Extract under preview mode so the custom particle lights itself full-bright (see
            // CustomParticleInstance#getLightColor) instead of sampling the real world's time-of-day light.
            particlesRenderState.reset();
            ParticlePreviewState.begin(this::captureChild);
            try {
                engine.extract(particlesRenderState, frustum, previewCamera, 1f);
            } finally {
                ParticlePreviewState.end();
            }

            FeatureRenderDispatcher featureDispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
            CameraRenderState camState = new CameraRenderState();
            camState.pos = new Vec3(ex, ey, ez);
            camState.orientation = previewCamera.rotation();
            camState.initialized = true;

            // Particle quads are camera-relative (worldPos - eye) with a world-space billboard that copies
            // previewCamera.rotation(), and prepare() bakes the model-view into their transform. Using the
            // conjugate of that same rotation is what makes the two cancel to screen-facing; deriving the
            // matrix independently only lines up when place() happens to agree on the convention.
            Matrix4fStack mv = RenderSystem.getModelViewStack();
            mv.pushMatrix();
            mv.mul(new Matrix4f().rotation(new Quaternionf(previewCamera.rotation()).conjugate()));
            // The vanilla particle feature renderer draws into Minecraft#getMainRenderTarget, not the
            // override; point that at the editor's offscreen buffer for this one draw so the quads land
            // in the preview instead of the game screen behind it.
            RenderTarget offscreen = OffscreenTargetContext.current();
            if (offscreen != null) PreviewRenderTarget.begin(offscreen);
            try {
                particlesRenderState.submit(featureDispatcher.getSubmitNodeStorage(), camState);
                featureDispatcher.renderAllFeatures();
            } finally {
                if (offscreen != null) PreviewRenderTarget.end();
                mv.popMatrix();
                particlesRenderState.reset();
            }
        }

        // Ground grid on the spawn plane plus short red/green/blue axes, so the viewport is never blank
        // while the particle is between lives. Quads, not lines: the lines render type needs a per-vertex
        // LineWidth element the buffer source can't supply here, which corrupts the shared buffer and
        // crashes the next frame's ShadowFeatureRenderer. Camera view baked into the pose, since the
        // model-view stack is still at identity at this point.
        private void drawReference(SceneCamera camera) {
            if (spawn == null) return;
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer c = buffers.getBuffer(RenderTypes.debugQuads());
            PoseStack pose = new PoseStack();
            pose.mulPose(camera.view());
            PoseStack.Pose ps = pose.last();

            float cx = (float) spawn.x, cy = (float) spawn.y, cz = (float) spawn.z;
            float ext = 2f, step = 1f, half = 0.02f, gy = cy; // one cell per block; grid/axes origin at the spawn point (0,0,0)
            for (float o = -ext; o <= ext + 1e-4f; o += step) {
                flatQuad(c, ps, cx - ext, cx + ext, cz + o - half, cz + o + half, gy, 0.5f, 0.5f, 0.55f, 0.55f);
                flatQuad(c, ps, cx + o - half, cx + o + half, cz - ext, cz + ext, gy, 0.5f, 0.5f, 0.55f, 0.55f);
            }
            float len = 0.25f, ah = 0.012f, ay = gy + 0.002f; // short axes just above the grid to avoid z-fighting
            flatQuad(c, ps, cx, cx + len, cz - ah, cz + ah, ay, 1f, 0.25f, 0.25f, 1f);          // X red
            flatQuad(c, ps, cx - ah, cx + ah, cz, cz + len, ay, 0.35f, 0.5f, 1f, 1f);           // Z blue
            vertQuad(c, ps, cx - ah, cx + ah, gy, gy + len, cz, 0.3f, 1f, 0.3f, 1f);            // Y green
            buffers.endBatch();
        }

        // Horizontal quad on the y plane; debugQuads is unculled so winding doesn't matter.
        private static void flatQuad(VertexConsumer c, PoseStack.Pose p, float x0, float x1, float z0, float z1,
                                     float y, float r, float g, float b, float a) {
            c.addVertex(p, x0, y, z0).setColor(r, g, b, a);
            c.addVertex(p, x0, y, z1).setColor(r, g, b, a);
            c.addVertex(p, x1, y, z1).setColor(r, g, b, a);
            c.addVertex(p, x1, y, z0).setColor(r, g, b, a);
        }

        // Vertical quad in the xy plane at fixed z (for the upward Y axis).
        private static void vertQuad(VertexConsumer c, PoseStack.Pose p, float x0, float x1, float y0, float y1,
                                     float z, float r, float g, float b, float a) {
            c.addVertex(p, x0, y0, z).setColor(r, g, b, a);
            c.addVertex(p, x1, y0, z).setColor(r, g, b, a);
            c.addVertex(p, x1, y1, z).setColor(r, g, b, a);
            c.addVertex(p, x0, y1, z).setColor(r, g, b, a);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addToGroup(ParticleEngine engine, Particle p) {
            ParticleRenderType rt = p.getGroup();
            if (rt == ParticleRenderType.NO_RENDER) return;
            ParticleGroup group = engine.particles.computeIfAbsent(rt, t ->
                    t == net.mehvahdjukaar.polytone.PolytoneRenderTypes.PARTICLE_MODEL_GROUP
                            ? new ModelParticleRenderGroup(engine)
                            : new QuadParticleGroup(engine, t));
            group.add(p);
        }

        private void capture(CustomParticleInstance p) {
            Snapshot s = new Snapshot();
            s.age = p.age;
            s.alive = p.isAlive();
            s.size = p.getQuadSize(1f);
            s.vx = p.xd;
            s.vy = p.yd;
            s.vz = p.zd;
            s.r = p.rCol;
            s.g = p.gCol;
            s.b = p.bCol;
            s.a = p.alpha;
            snapshot = s;
        }

        void close() {
            if (sandbox != null) {
                sandbox.particles.clear();
                sandbox = null;
            }
            children.clear();
            particle = null;
            if (projectionBuffer != null) {
                projectionBuffer.close();
                projectionBuffer = null;
            }
        }

        final class Snapshot {
            int age;
            boolean alive;
            double vx, vy, vz;
            float size, r, g, b, a;
        }
    }

    // Particles billboard and position relative to the camera handed to extract, and vanilla's setters are
    // protected, so a subclass exposes them (no access widener, same as the biome scene pass).
    private static final class PreviewCamera extends Camera {
        // The rotation has to be exactly Ry(-yaw)Rx(-pitch), the conjugate of SceneCamera.view()'s rotation:
        // the render pass sets the model-view to that conjugate, and the billboard copies this camera's
        // rotation, so only then do the two cancel to screen-facing. setRotation(yRot, xRot) builds
        // Ry(pi - yRot)Rx(-xRot), hence the 180 + yaw. Rebuilding it from the look vector instead lands
        // edge-on, which only shows up as the whole atlas once you zoom far out.
        void place(Vec3 eye, float yawDeg, float pitchDeg) {
            this.setRotation(180f + yawDeg, pitchDeg);
            this.setPosition(eye);
        }
    }
}
