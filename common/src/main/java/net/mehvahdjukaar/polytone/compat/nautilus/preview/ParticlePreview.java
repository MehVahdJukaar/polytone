package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.LiveViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.PreviewContext;
import net.mehvahdjukaar.polytone.content.particle.ParticleParticleEmitter;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.mehvahdjukaar.polytone.content.particle.custom.ICustomParticleFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Live preview for Polytone custom particles. It spawns a real {@link CustomParticleInstance} from
 * the edited type and ticks it on the render thread, drawing each frame through the game's own
 * particle render path into the Nautilus {@link LiveViewport} - so what you see is the runtime
 * particle, not a re-implementation.
 *
 * <p>The particle's own state ({@code p.*}) comes from the live instance, so only the world-context
 * {@code global.*} sliders from {@link ExpressionPreview} are simulated, and only when the type's
 * expressions actually read them. Sprites are borrowed from the pack's already-baked particle of the
 * same id (a not-yet-registered particle can't be drawn until the pack is reloaded).
 */
public final class ParticlePreview extends ExpressionPreview {

    private final @Nullable ResourceLocation contentId;
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
    private volatile boolean loop = true;

    public ParticlePreview(TabPreview.Context ctx) {
        this.contentId = ctx.contentId();
        this.file = ctx.file();

        viewport.setRenderer(renderer);
        viewport.setPanEnabled(true); // orbit, zoom and pan to inspect against the grid

        playButton.addActionListener(e -> setPlaying(!viewport.isPlaying()));
        stepButton.addActionListener(e -> { renderer.requestRespawnIfDead(); viewport.step(); });
        speedSlider.addChangeListener(e -> {
            renderer.speed = speedSlider.getValue() / 100.0;
            speedLabel.setText(String.format("%.1fx", renderer.speed));
        });

        buildLayout();
        // The env readout only refreshes when a frame lands; prime it once.
        setPlaying(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        viewport.dispose();
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

        // Transport controls.
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

        // Live-at-player toggle + the auto-revealing global sliders, exactly like the colormap panel.
        liveToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(liveToggle);
        content.add(Box.createVerticalStrut(UiScale.small()));
        JComponent env = envGroup();
        env.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(env);
        content.add(Box.createVerticalStrut(UiScale.med()));

        // The viewport.
        viewport.setBorder(UiTheme.hairlineBorder());
        SquareRow view = new SquareRow(0, UiScale.px(200), UiScale.px(460), viewport);
        view.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(view);
        content.add(Box.createVerticalStrut(UiScale.small()));

        // Readout HUD.
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
        // Nothing to draw without a type (pack not loaded / unsupported) - don't spin the render loop.
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
        // Particles are driven by the render loop, not a one-shot sample; a value/live change just asks
        // for a fresh spawn. The env sliders re-read on the next rendered frame.
        renderer.requestRespawn();
        viewport.refresh();
    }

    // Sprites come from the pack's already-registered particle of this id (baked into the atlas). The
    // edited form has none of its own, and building the atlas from the editor isn't worth it here.
    private @Nullable SpriteSet borrowSprites() {
        // When editing a file the editor gives us `file`, not `contentId`; derive the id from the path.
        ResourceLocation id = contentId != null ? contentId : idFromFile(file);
        if (id == null) return null;
        ICustomParticleFactory live = Polytone.CUSTOM_PARTICLES.customParticleFactories.getValue(id);
        return live instanceof CustomParticleType ct ? ct.getSpriteSet() : null;
    }

    // <pack>/assets/<namespace>/polytone/custom_particles/<path...>.json -> <namespace>:<path...>
    private static @Nullable ResourceLocation idFromFile(@Nullable Path file) {
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
                return p.isEmpty() ? null : ResourceLocation.fromNamespaceAndPath(ns, p);
            }
        }
        return null;
    }

    // Pushes the latest frame's particle state into the HUD labels (called on the EDT after a frame).
    private void updateReadout() {
        ParticleScene.Snapshot s = renderer.snapshot;
        if (s == null) {
            String d = renderer.diag;
            ageReadout.setText(d != null ? d : "(no particle)");
            motionReadout.setText(" ");
            colorReadout.setText(" ");
            return;
        }
        ageReadout.setText(String.format("age %d / %d%s", s.age, s.lifetime, s.alive ? "" : "  (dead)"));
        motionReadout.setText(String.format("<html>size %.3f&nbsp;&nbsp;vel %.3f, %.3f, %.3f</html>",
                s.size, s.vx, s.vy, s.vz));
        int rgb = ((int) (s.r * 255) << 16) | ((int) (s.g * 255) << 8) | (int) (s.b * 255);
        colorReadout.setText(String.format("<html>color #%06X&nbsp;&nbsp;alpha %.2f</html>", rgb & 0xFFFFFF, s.a));
    }

    // Render-thread half: owns the ticked instance and draws it. LiveViewport calls advance() then
    // render() on the render thread, so instance mutation and read never race the EDT; the HUD reads
    // only the volatile snapshot.
    private final class ParticleScene implements LiveViewport.Renderer {

        private static final int MAX_CHILDREN = 400;

        private @Nullable CustomParticleInstance particle;
        // Children emitted by particle_emitters, captured into the preview instead of the world (custom
        // or vanilla). Touched only on the render thread (advance/render), so no synchronisation needed.
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
            if (respawn || particle == null || (!particle.isAlive() && loop)) {
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

            // Tick `speed` game-ticks per frame, carrying the fractional remainder.
            accumulator += speed;
            int ticks = (int) accumulator;
            accumulator -= ticks;
            // Expressions read global.* through the sim proxies on THIS (render) thread. Emitters route
            // their children into our collection instead of the world for the duration of the tick.
            installSim();
            ParticleParticleEmitter.setSink(this::captureChild);
            try {
                for (int i = 0; i < ticks && p.isAlive(); i++) {
                    p.tickSync();
                    tickChildren();
                }
            } finally {
                ParticleParticleEmitter.setSink(null);
                clearSim();
            }
            diag = null;
            capture(p);
            postReadout();
        }

        // Preview sink: the manager builds a detached child (custom via createPreviewInstance, else via
        // makeParticle), which we tick locally instead of letting it spawn into the world. Returns false
        // when full so the emitter stops for this tick.
        private boolean captureChild(ParticleParticleEmitter emitter, Level level, ParticleOptions po,
                                     double x, double y, double z, double dx, double dy, double dz) {
            if (children.size() >= MAX_CHILDREN) return false;
            if (!(level instanceof ClientLevel clientLevel)) return true;
            try {
                Particle child = Polytone.CUSTOM_PARTICLES.createPreviewParticle(po, clientLevel, x, y, z, dx, dy, dz);
                if (child != null) children.add(child);
            } catch (Exception ignored) {
            }
            return true;
        }

        private void tickChildren() {
            var it = children.iterator();
            while (it.hasNext()) {
                Particle c = it.next();
                if (!c.isAlive()) {
                    it.remove();
                    continue;
                }
                // Custom children tick synchronously (bypassing the async batch); vanilla ones don't.
                if (c instanceof CustomParticleInstance custom) custom.tickSync();
                else c.tick();
                if (!c.isAlive()) it.remove();
            }
        }

        private void postReadout() {
            javax.swing.SwingUtilities.invokeLater(ParticlePreview.this::updateReadout);
        }

        private void spawnParticle(CustomParticleType t, ClientLevel level, Minecraft mc) {
            children.clear();
            if (spawn == null) {
                spawn = mc.player != null ? mc.player.getEyePosition().add(mc.player.getLookAngle().scale(2.5))
                        : new Vec3(0, level.getMinBuildHeight() + 80, 0);
                viewport.frame(new org.joml.Vector3f((float) spawn.x, (float) spawn.y, (float) spawn.z), 1.5f, true);
            }
            installSim();
            try {
                particle = t.createPreviewInstance(level, spawn.x, spawn.y, spawn.z, null);
            } catch (Exception ex) {
                particle = null;
                diag = "spawn failed: " + ex;
                Polytone.LOGGER.warn("Particle preview spawn failed", ex);
            } finally {
                clearSim();
            }
            accumulator = 0;
        }

        @Override
        public void render(SceneCamera camera, int width, int height) {
            CustomParticleInstance p = particle;
            if (p == null || spawn == null) return;
            ParticleRenderPass.render(p, children, camera, spawn, width, height);
        }

        private void capture(CustomParticleInstance p) {
            Snapshot s = new Snapshot();
            s.age = p.age;
            s.lifetime = p.getLifetime();
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

        static final class Snapshot {
            int age, lifetime;
            boolean alive;
            double vx, vy, vz;
            float size, r, g, b, a;
        }
    }
}
