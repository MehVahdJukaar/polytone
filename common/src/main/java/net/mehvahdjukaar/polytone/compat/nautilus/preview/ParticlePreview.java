package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.nautilus.render.OffscreenTargetContext;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.LiveViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.ParticlePreviewMode;
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
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Live preview for Polytone custom particles. It spawns the real {@link CustomParticleInstance} from
 * the edited type into a throwaway sandbox {@link ParticleEngine} and ticks it on the render thread,
 * then draws it through the game's own particle collect path ({@link ParticleEngine#extract}) into
 * the Nautilus {@link LiveViewport} - so what you see is the runtime particle, not a re-implementation.
 *
 * <p>Because Polytone's particle system is async and its emitters spawn into the live world, the
 * preview ticks via {@link CustomParticleInstance#tickSync()} (bypassing the async batch the sandbox
 * can't drive) and runs inside {@link ParticlePreviewMode} (which makes creation synchronous and
 * routes emitter children into the sandbox instead of the world). All of that is gated to the render
 * thread, so normal gameplay is unaffected.
 *
 * <p>Only the world-context {@code global.*} sliders from {@link ExpressionPreview} are simulated -
 * the particle's own {@code p.*} state is real. Sprites are borrowed from the pack's already-baked
 * particle of the same id (a not-yet-registered particle can't be drawn until the pack is reloaded).
 */
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
        Box toolbar = Box.createVerticalBox();
        Box topRow = Box.createHorizontalBox();
        topRow.add(status);
        topRow.add(Box.createHorizontalGlue());
        addRow(toolbar, topRow);

        Box content = Box.createVerticalBox();

        Box transport = Box.createHorizontalBox();
        transport.setAlignmentX(Component.LEFT_ALIGNMENT);
        transport.add(playButton);
        transport.add(Box.createHorizontalStrut(UiScale.small()));
        transport.add(stepButton);
        transport.add(Box.createHorizontalStrut(UiScale.med()));
        transport.add(StyledLabels.small("Speed"));
        transport.add(Box.createHorizontalStrut(6));
        transport.add(speedSlider);
        transport.add(Box.createHorizontalStrut(6));
        transport.add(speedLabel);
        transport.setMaximumSize(UiScale.maxHeightHugging(transport));
        content.add(transport);
        content.add(Box.createVerticalStrut(UiScale.small()));

        liveToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(liveToggle);
        content.add(Box.createVerticalStrut(UiScale.small()));
        JComponent env = envGroup();
        env.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(env);
        content.add(Box.createVerticalStrut(UiScale.med()));

        viewport.setBorder(UiTheme.hairlineBorder());
        SquareRow view = new SquareRow(0, UiScale.px(200), UiScale.px(460), viewport);
        view.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(view);
        content.add(Box.createVerticalStrut(UiScale.small()));

        Box hud = Box.createVerticalBox();
        hud.setAlignmentX(Component.LEFT_ALIGNMENT);
        ageReadout.setAlignmentX(Component.LEFT_ALIGNMENT);
        motionReadout.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorReadout.setAlignmentX(Component.LEFT_ALIGNMENT);
        hud.add(ageReadout);
        hud.add(motionReadout);
        hud.add(colorReadout);
        content.add(hud);

        root = new PreviewSurface(toolbar, content);
        root.setMinimumSize(new Dimension(UiScale.px(160), UiScale.px(120)));
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        CustomParticleType t = value instanceof CustomParticleType ct ? ct : null;
        if (t != null) {
            SpriteSet sprites = borrowSprites();
            if (sprites == null) {
                this.type = null;
                status.info("No baked sprites yet - reload the pack in-game to preview this particle.");
            } else {
                t.setSpriteSet(sprites);
                this.type = t;
                status.setText("");
            }
        } else {
            this.type = null;
            if (value instanceof ICustomParticleFactory) {
                status.info("Only fully custom particles have a live preview.");
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
    protected void onLiveModeChanged(boolean live) {
        renderer.requestRespawn();
    }

    @Override
    protected void recompute() {
        renderer.requestRespawn();
        viewport.refresh();
    }

    // Sprites come from the pack's already-registered particle of this id (baked into the atlas).
    private @Nullable SpriteSet borrowSprites() {
        Identifier id = contentId != null ? contentId : idFromFile(file);
        if (id == null) return null;
        ICustomParticleFactory live = Polytone.CUSTOM_PARTICLES.customParticleFactories.getValue(id);
        return live instanceof CustomParticleType ct ? ct.getSpriteSet() : null;
    }

    // <pack>/assets/<namespace>/polytone/custom_particles/<path...>.json -> <namespace>:<path...>
    private static @Nullable Identifier idFromFile(@Nullable Path file) {
        if (file == null) return null;
        int n = file.getNameCount();
        for (int i = 0; i + 3 < n; i++) {
            if (file.getName(i).toString().equals("assets")
                    && file.getName(i + 2).toString().equals(Polytone.MOD_ID)
                    && file.getName(i + 3).toString().equals("custom_particles")) {
                String ns = file.getName(i + 1).toString();
                StringBuilder path = new StringBuilder();
                for (int j = i + 4; j < n; j++) {
                    if (!path.isEmpty()) path.append('/');
                    path.append(file.getName(j).toString());
                }
                String p = path.toString().replaceFirst("\\.json$", "");
                return p.isEmpty() ? null : Identifier.fromNamespaceAndPath(ns, p);
            }
        }
        return null;
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

    /**
     * Render-thread half: owns the ticked instance + its emitter children in a sandbox engine and
     * draws them. {@link LiveViewport} calls {@link #advance()} then {@link #render} on the render
     * thread, so instance mutation and read never race the EDT; the HUD reads only the volatile
     * {@link #snapshot}.
     */
    private final class ParticleScene implements LiveViewport.Renderer {

        private static final int MAX_CHILDREN = 400;

        // Sandbox engine: never ticked (that would route custom particles back into the async batch);
        // only used to build render groups and run the native extract() collect. Lazily created.
        private @Nullable ParticleEngine sandbox;
        private final ParticlesRenderState particlesRenderState = new ParticlesRenderState();
        private final PreviewCamera previewCamera = new PreviewCamera();
        private @Nullable PerspectiveProjectionMatrixBuffer projectionBuffer;
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
            ParticlePreviewMode.begin(this::captureChild);
            try {
                for (int i = 0; i < ticks && p.isAlive(); i++) {
                    p.tickSync();
                    tickChildren();
                }
            } finally {
                ParticlePreviewMode.end();
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
            ParticlePreviewMode.begin(this::captureChild);
            try {
                // null options: the type builds a plain instance (no BlockState / extra data needed here).
                Particle p = t.createParticle(null, level, spawn.x, spawn.y, spawn.z, 0, 0, 0, random);
                particle = p instanceof CustomParticleInstance cpi ? cpi : null;
                if (particle == null) diag = "spawn returned null";
            } catch (Exception ex) {
                particle = null;
                diag = "spawn failed: " + ex;
                Polytone.LOGGER.warn("Particle preview spawn failed", ex);
            } finally {
                ParticlePreviewMode.end();
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
            previewCamera.place(new Vec3(ex, ey, ez), target.x, target.y, target.z);

            if (projectionBuffer == null)
                projectionBuffer = new PerspectiveProjectionMatrixBuffer("polytone particle scene");
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(camera.projection((float) width / height)),
                    ProjectionType.PERSPECTIVE);

            // Ground grid + RGB axes at the spawn point, for spatial reference (and so the viewport is
            // never blank while the particle is between lives). Drawn through the RenderType pipeline,
            // which honours the editor's offscreen redirect, before the particle model-view is pushed.
            drawReference(camera);

            CustomParticleInstance p = particle;
            ParticleEngine engine = sandbox;
            if (p == null || engine == null) return;

            // Collect the live particles into fresh groups (no engine.tick(), which would re-enqueue
            // custom particles into the async batch), then run the native extract.
            engine.particles.clear();
            addToGroup(engine, p);
            for (Particle c : children) if (c.isAlive()) addToGroup(engine, c);

            Frustum frustum = new Frustum(camera.view(), camera.projection((float) width / height));
            frustum.prepare(ex, ey, ez);

            particlesRenderState.reset();
            engine.extract(particlesRenderState, frustum, previewCamera, 1f);

            FeatureRenderDispatcher featureDispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
            CameraRenderState camState = new CameraRenderState();
            camState.pos = new Vec3(ex, ey, ez);
            camState.orientation = previewCamera.rotation();
            camState.initialized = true;

            // Particle quads are stored camera-relative (worldPos - eye) with a world-space billboard
            // (LOOK_AT_* copies previewCamera.rotation()); prepare() bakes the model-view matrix into
            // their transform. Using the conjugate of the very rotation the billboard used guarantees
            // the two cancel to screen-facing, whatever yaw/pitch convention place() picked - deriving
            // it independently (Rx(pitch)Ry(yaw)) only lines up when place() happens to be exact.
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

        // A ground grid on the spawn plane plus short red/green/blue X/Y/Z axes at the spawn point, as
        // constant-width GL lines (so they stay crisp at any zoom, unlike world-space quad slivers).
        // Camera view baked into the pose (world space), like the biome scene pass; drawn with the
        // model-view stack at identity, before the particle pass pushes its rotation onto it.
        private void drawReference(SceneCamera camera) {
            if (spawn == null) return;
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer c = buffers.getBuffer(RenderTypes.lines());
            PoseStack pose = new PoseStack();
            pose.mulPose(camera.view());
            PoseStack.Pose ps = pose.last();

            float cx = (float) spawn.x, cy = (float) spawn.y, cz = (float) spawn.z;
            float ext = 2f, step = 0.5f, gy = cy - 0.5f, g = 0.45f; // grid a touch below the particle
            for (float o = -ext; o <= ext + 1e-4f; o += step) {
                line(c, ps, cx - ext, gy, cz + o, cx + ext, gy, cz + o, g, g, g);
                line(c, ps, cx + o, gy, cz - ext, cx + o, gy, cz + ext, g, g, g);
            }
            float len = 1f;
            line(c, ps, cx, cy, cz, cx + len, cy, cz, 1f, 0.25f, 0.25f);  // X red
            line(c, ps, cx, cy, cz, cx, cy + len, cz, 0.3f, 1f, 0.3f);    // Y green
            line(c, ps, cx, cy, cz, cx, cy, cz + len, 0.35f, 0.5f, 1f);   // Z blue
            buffers.endBatch();
        }

        // One GL line segment; the lines render type needs the segment direction as the vertex normal.
        private static void line(VertexConsumer c, PoseStack.Pose p, float x0, float y0, float z0,
                                 float x1, float y1, float z1, float r, float g, float b) {
            float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-5f) {
                nx /= len;
                ny /= len;
                nz /= len;
            }
            c.addVertex(p, x0, y0, z0).setColor(r, g, b, 1f).setNormal(p, nx, ny, nz);
            c.addVertex(p, x1, y1, z1).setColor(r, g, b, 1f).setNormal(p, nx, ny, nz);
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

    /**
     * A {@link Camera} the preview can aim by hand: particles billboard + position relative to the
     * camera passed to {@code extract}, and vanilla's setters are {@code protected}, so a subclass
     * exposes them (no access widener, matching the biome scene pass's no-AW approach).
     */
    private static final class PreviewCamera extends Camera {
        void place(Vec3 eye, double tx, double ty, double tz) {
            double dx = tx - eye.x, dy = ty - eye.y, dz = tz - eye.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
            float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
            this.setRotation(yaw, pitch);
            this.setPosition(eye);
        }
    }
}
