package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.render.BlockScene;
import net.mehvahdjukaar.nautilus.render.BlockTint;
import net.mehvahdjukaar.nautilus.swing.preview.PixelTextureView;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.SceneViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.ColorSwatch;
import net.mehvahdjukaar.nautilus.swing.toolkit.EditorOps;
import net.mehvahdjukaar.nautilus.swing.toolkit.GroupPanels;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
import net.mehvahdjukaar.nautilus.swing.widget.RegistryPickerField;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.PreviewContext;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.SimProxies;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.SimValue;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.awt.image.BufferedImage;

/**
 * Live preview for Polytone colormaps.
 *
 * <p>It reuses the standard Nautilus 2D-preview chrome: a {@link PreviewSurface} (scrolling dark canvas
 * under a themed toolbar) holds the visuals - a small square block view and a same-size result swatch
 * side by side, then a bigger square {@link PixelTextureView} of the source colormap texture with the
 * sampled texel marked and its coordinates captioned right under it. The toolbar carries only the
 * "Live at player" toggle and a status line; every simulated input (block/biome pickers, y slider,
 * env/light sliders) lives in one titled group below it that greys out when the toggle is on.
 *
 * <p>The block/biome pickers are the editor's own searchable, icon-bearing {@link RegistryPickerField},
 * so they look and behave exactly like the identifier fields in the schema form.
 *
 * <p>The block view renders the picked block, tinted live by the colormap's current output through the
 * Nautilus {@link BlockTint} seam - so the picker drives both the sampled state and what you see in 3D.
 *
 * <p>All sampling goes through the decoded {@link Colormap}'s instrumented sampler
 * ({@code sampleColorForPreview} + a {@link CaptureSink}), so the swatch, the 2D marker and the 3D block
 * tint all come from one runtime-faithful evaluation. In simulated mode it is fed a minimal
 * {@link SimLevel} and the preview's own {@link SimProxies} (env sliders, shown only when the expression
 * reads them). With "Live at player" on it samples the real level at the player's feet with the live
 * clock/proxies instead, and a timer keeps it refreshed.
 */
public final class ColormapPreview implements TabPreview {

    private final @Nullable Path file;
    private final @Nullable ResourceLocation contentId;

    private PreviewSurface root;

    private final SceneViewport blockView = new SceneViewport();
    private final ColorSwatch swatch = new ColorSwatch();
    private final PixelTextureView imageView = new PixelTextureView();
    private final PreviewStatus status = new PreviewStatus();

    // "Live at player" bypasses all simulation: sample the real level at the player's feet on a timer.
    private final JCheckBox liveToggle = new JCheckBox("Live at player");
    private boolean liveMode;
    private final Timer liveTimer = new Timer(500, e -> { if (liveMode) recompute(); });

    // The editor's searchable, icon-bearing registry pickers (same widget the schema form uses).
    private final RegistryPickerField blockPicker;
    private final RegistryPickerField biomePicker;
    private final JSlider ySlider = new JSlider(-64, 320, 64);
    private final JLabel yLabel = StyledLabels.mutedSmall("");
    private final JLabel climateReadout = StyledLabels.mutedSmall(" ");

    // Titled group holding every simulated input; greyed out (disabled) while in live mode.
    private JPanel inputsBox;

    // Env sliders, one per SimProxies input, shown only when the sampled expression reads them.
    private final SimProxies sim = new SimProxies();
    private final Map<SimValue, EnvControl> envControls = new LinkedHashMap<>();
    private final Box envSection = Box.createVerticalBox();
    private final JLabel envHeader = StyledLabels.muted("Environment");

    // Light sliders feed SimLevel; shown only when the expression reads sky/block light.
    private final JSlider skySlider = new JSlider(0, 15, 15);
    private final JLabel skyLabel = StyledLabels.mutedSmall("");
    private final JComponent skyRow;
    private final JSlider blockSlider = new JSlider(0, 15, 0);
    private final JLabel blockLabel = StyledLabels.mutedSmall("");
    private final JComponent blockRow;

    // The block's BlockTint reads currentTint (set on every recompute); the callback runs on the render
    // thread, so currentTint is volatile. -1 -> vanilla tint (invalid/no sample).
    private volatile int currentTint = -1;
    private final BlockTint blockTint = (state, tintIndex) -> currentTint;
    // The state currently placed in the 3D scene; rebuilt only when the picked/live block changes.
    private @Nullable BlockState sceneState;

    private @Nullable Colormap colormap;

    // Cache the decoded source image so we don't hit the disk on every keystroke.
    private @Nullable String cachedImageKey;
    private @Nullable ArrayImage cachedArrayImage;
    private @Nullable BufferedImage cachedDisplayImage;

    public ColormapPreview(TabPreview.Context ctx) {
        this.file = ctx.file();
        this.contentId = ctx.contentId();

        this.blockPicker = new RegistryPickerField(Registries.BLOCK, id -> recompute());
        this.biomePicker = new RegistryPickerField(Registries.BIOME, id -> recompute());
        blockPicker.setSelected(ResourceLocation.withDefaultNamespace("grass_block"));
        biomePicker.setSelected(ResourceLocation.withDefaultNamespace("plains"));

        this.skyRow = labeled("Sky light", withValue(skySlider, skyLabel));
        this.blockRow = labeled("Block light", withValue(blockSlider, blockLabel));

        blockView.setPanEnabled(false); // small fixed thumbnail: orbit/zoom to inspect, but never pan off-view
        updateScene(Blocks.GRASS_BLOCK.defaultBlockState());

        // One row per sim input, hidden until the sampled expression is seen reading it.
        for (SimValue v : sim.values()) {
            EnvControl c = new EnvControl(v, this::recompute);
            c.row.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.row.setVisible(false);
            envControls.put(v, c);
            envSection.add(c.row);
        }

        buildLayout();

        liveToggle.setOpaque(false);
        liveToggle.setToolTipText("Sample the real world at the player's feet with the live clock, instead of the simulated inputs.");
        liveToggle.addActionListener(e -> setLiveMode(liveToggle.isSelected()));
        ySlider.addChangeListener(e -> recompute());
        skySlider.addChangeListener(e -> recompute());
        blockSlider.addChangeListener(e -> recompute());

        recompute();
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void dispose() {
        liveTimer.stop();
        blockView.dispose();
    }

    private void buildLayout() {
        // Sticky header: only the mode toggle + status stay pinned. Keeping it to one short row means
        // the header never grows tall enough to push the visuals below the fold.
        Box toolbar = Box.createVerticalBox();

        Box topRow = Box.createHorizontalBox();
        topRow.add(liveToggle);
        topRow.add(Box.createHorizontalStrut(UiScale.med()));
        topRow.add(status);
        topRow.add(Box.createHorizontalGlue());
        addRow(toolbar, topRow);

        // Everything else lives in the scrolling body, so a crowded set of controls scrolls instead
        // of clipping the visuals, and the panel can be dragged down to any size.
        Box content = Box.createVerticalBox();

        // All simulated inputs sit in one titled group directly under the "Live at player" toggle, so
        // the toggle plainly reads as the switch that overrides the whole group. Every field is laid
        // out label-over-field at full width, so the pickers and sliders line up and nothing crops.
        inputsBox = GroupPanels.outlined();
        inputsBox.setLayout(new BoxLayout(inputsBox, BoxLayout.Y_AXIS));
        inputsBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel groupHeader = StyledLabels.muted("Simulated inputs");
        groupHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputsBox.add(groupHeader);
        inputsBox.add(Box.createVerticalStrut(UiScale.small()));

        addField(inputsBox, labeled("Block", blockPicker));
        addField(inputsBox, labeled("Biome", biomePicker));
        climateReadout.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputsBox.add(climateReadout);
        inputsBox.add(Box.createVerticalStrut(UiScale.med()));
        addField(inputsBox, labeled("Y level", withValue(ySlider, yLabel)));

        // Env-global + light sliders (visibility driven by detection).
        envHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        envSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        skyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        blockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputsBox.add(envHeader);
        inputsBox.add(envSection);
        inputsBox.add(skyRow);
        inputsBox.add(blockRow);

        content.add(inputsBox);
        content.add(Box.createVerticalStrut(UiScale.med()));

        // Canvas content: block view + result swatch, then the source texture with the sample marker.
        blockView.setBorder(UiTheme.hairlineBorder());
        imageView.setBorder(UiTheme.hairlineBorder());

        SquareRow topSquares = new SquareRow(UiScale.med(), UiScale.px(84), UiScale.px(196), blockView, swatch);
        topSquares.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(topSquares);
        content.add(Box.createVerticalStrut(UiScale.med()));
        SquareRow textureRow = new SquareRow(0, UiScale.px(140), UiScale.px(400), imageView);
        textureRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(textureRow);

        root = new PreviewSurface(toolbar, content);
        // Let the host split pane drag the preview down to any size; the body scrolls when it doesn't fit.
        root.setMinimumSize(new Dimension(UiScale.px(160), UiScale.px(120)));
    }

    private static void addRow(Box toolbar, JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(UiScale.maxHeightHugging(row));
        toolbar.add(row);
        toolbar.add(Box.createVerticalStrut(UiScale.small()));
    }

    // Adds a full-width field to a vertical group, capped to its own height so it doesn't stretch.
    private static void addField(JComponent box, JComponent field) {
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(UiScale.maxHeightHugging(field));
        box.add(field);
        box.add(Box.createVerticalStrut(UiScale.small()));
    }

    // Label over field, field stretches horizontally but keeps its own height.
    private static JComponent labeled(String text, JComponent field) {
        Box row = Box.createVerticalBox();
        JLabel l = StyledLabels.small(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        row.add(l);
        row.add(field);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JComponent withValue(JSlider slider, JLabel value) {
        Box row = Box.createHorizontalBox();
        slider.setAlignmentY(Component.CENTER_ALIGNMENT);
        value.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(slider);
        row.add(Box.createHorizontalStrut(6));
        row.add(value);
        return row;
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.colormap = value instanceof Colormap cm ? cm : null;
        recompute();
    }

    private void setLiveMode(boolean live) {
        this.liveMode = live;
        // Grey out (rather than hide) the whole input group, so the toggle's effect on it is visible.
        setEnabledDeep(inputsBox, !live);
        if (live) {
            hideEnv();
            liveTimer.start();
        } else {
            liveTimer.stop();
        }
        root.revalidate();
        root.repaint();
        recompute();
    }

    private static void setEnabledDeep(Component c, boolean enabled) {
        c.setEnabled(enabled);
        if (c instanceof Container cont) {
            for (Component child : cont.getComponents()) {
                setEnabledDeep(child, enabled);
            }
        }
    }

    private void recompute() {
        Colormap cm = this.colormap;
        if (cm == null) {
            status.info("Waiting for a valid colormap...");
            clearResult();
            clearImage();
            hideEnv();
            return;
        }

        // The form-decoded colormap has no texture yet; load & attach the sibling .png so it can sample.
        if (cm.needsToFillTexture()) {
            ArrayImage img = resolveSourceImage(cm);
            if (img == null) {
                status.error("No source .png found for this colormap.");
                clearResult();
                clearImage();
                hideEnv();
                return;
            }
            cm.acceptTexture(img);
        }

        if (liveMode) sampleLive(cm);
        else sampleSimulated(cm);
    }

    private void sampleSimulated(Colormap cm) {
        BlockState state = resolveBlock(blockPicker.getSelected()).defaultBlockState();
        Biome biome = resolveBiome(biomePicker.getSelected());
        int y = ySlider.getValue();
        yLabel.setText("y = " + y);
        skyLabel.setText(String.valueOf(skySlider.getValue()));
        blockLabel.setText(String.valueOf(blockSlider.getValue()));
        for (EnvControl c : envControls.values()) {
            c.value.setText(c.format());
        }
        updateClimate(biome);
        updateScene(state);

        BlockPos pos = new BlockPos(0, y, 0);
        SimLevel level = new SimLevel(pos, state, biome, skySlider.getValue(), blockSlider.getValue());
        CaptureSink sink = new CaptureSink();

        sim.clearReads();
        PreviewContext.install(sim);
        try {
            cm.sampleColorForPreview(state, pos, biome, null, level, sink);
        } catch (Exception ex) {
            status.error("Sampling failed: " + ex.getMessage());
            clearResult();
            showSourceNoMarker();
            showEnv(level.usedSky, level.usedBlock);
            return;
        } finally {
            PreviewContext.clear();
        }

        // Show only the sliders the expression actually read this pass.
        showEnv(level.usedSky, level.usedBlock);
        applyResult(sink);
    }

    // Real world at the player's feet: no SimLevel, no PreviewContext, so the live clock/proxies drive it.
    private void sampleLive(Colormap cm) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            status.error("No world loaded - can't sample at the player.");
            clearResult();
            showSourceNoMarker();
            return;
        }
        BlockPos pos = player.blockPosition();
        if (mc.level.getBlockState(pos).isAir()) pos = pos.below(); // feet are usually air; sample the ground
        BlockState state = mc.level.getBlockState(pos);
        Biome biome = mc.level.getBiome(pos).value();

        updateClimate(biome);
        updateScene(state.isAir() ? Blocks.GRASS_BLOCK.defaultBlockState() : state);

        CaptureSink sink = new CaptureSink();
        try {
            cm.sampleColorForPreview(state, pos, biome, null, mc.level, sink);
        } catch (Exception ex) {
            status.error("Sampling failed: " + ex.getMessage());
            clearResult();
            showSourceNoMarker();
            return;
        }
        applyResult(sink);
    }

    private void applyResult(CaptureSink sink) {
        if (!sink.captured) {
            status.info("Colormap produced no sample.");
            clearResult();
            showSourceNoMarker();
            return;
        }
        setResult(sink.argb);
        float u = markerFrac(sink.col, imageWidth());
        float v = markerFrac(sink.row, imageHeight());
        imageView.setImage(cachedDisplayImage);
        imageView.setMarker(u, v);
        imageView.setCaption(String.format("x %.3f   y %.3f   @ %d, %d", sink.x, sink.y, sink.col, sink.row));
        status.setText("");
    }

    private void updateClimate(@Nullable Biome biome) {
        if (biome != null) {
            var cs = ColorUtils.getClimateSettings(biome);
            climateReadout.setText(String.format("<html>temp %.2f&nbsp;&nbsp;downfall %.2f</html>",
                    cs.temperature(), cs.downfall()));
        } else {
            climateReadout.setText("(no biome)");
        }
    }

    // Rebuilds the 3D scene only when the block actually changes, so per-keystroke resamples don't churn it.
    private void updateScene(BlockState state) {
        if (state.equals(sceneState)) return;
        boolean first = sceneState == null;
        sceneState = state;
        BlockScene scene = BlockScene.of(List.of(new BlockScene.Placement(BlockPos.ZERO, state, blockTint)));
        blockView.setScene(scene, first); // keep the camera after the first build
    }

    // Updates the swatch (fill + hex) and the 3D block tint, then re-renders the scene. The sampled
    // texel coordinates live under the source texture instead, so the swatch stays uncluttered.
    private void setResult(int argb) {
        int rgb = argb & 0xFFFFFF;
        swatch.set(new Color(rgb), String.format("#%06X", rgb));
        currentTint = 0xFF000000 | rgb;
        blockView.refresh();
    }

    private void clearResult() {
        swatch.set(null);
        currentTint = -1;
        blockView.refresh();
    }

    // No source image at all: blank the texture view.
    private void clearImage() {
        imageView.setImage(null);
        imageView.clearMarker();
        imageView.setCaption(null);
    }

    // Keep the source texture visible but drop the marker/caption (used on sampling failures).
    private void showSourceNoMarker() {
        imageView.setImage(cachedDisplayImage);
        imageView.clearMarker();
        imageView.setCaption(null);
    }

    private void hideEnv() {
        sim.clearReads();
        showEnv(false, false);
    }

    private void showEnv(boolean usedSky, boolean usedBlock) {
        boolean any = false;
        for (EnvControl c : envControls.values()) {
            boolean show = c.input.wasRead();
            c.row.setVisible(show);
            any |= show;
        }
        skyRow.setVisible(usedSky);
        blockRow.setVisible(usedBlock);
        envHeader.setVisible(any || usedSky || usedBlock);
        inputsBox.revalidate();
        inputsBox.repaint();
    }

    private int imageWidth() {
        return cachedArrayImage != null ? cachedArrayImage.width() : 1;
    }

    private int imageHeight() {
        return cachedArrayImage != null ? cachedArrayImage.height() : 1;
    }

    private static float markerFrac(int index, int size) {
        return size <= 1 ? 0f : index / (float) (size - 1);
    }

    // --- picker resolution ----------------------------------------------------------------------

    private static Block resolveBlock(@Nullable ResourceLocation id) {
        if (id == null) return Blocks.AIR;
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
    }

    // Resolve against the same registry view the picker listed from (world registries when loaded, else
    // the cached vanilla ones), so any id the biome picker offered resolves back to a Biome here.
    private @Nullable Biome resolveBiome(@Nullable ResourceLocation id) {
        if (id == null) return null;
        try {
            Side side = EditorOps.resolveSide(biomePicker);
            return EditorOps.registries(side).lookup(Registries.BIOME)
                    .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.BIOME, id)))
                    .map(Holder::value)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    // --- source image resolution ---------------------------------------------------------------

    private @Nullable ArrayImage resolveSourceImage(Colormap cm) {
        String key;
        Supplier<BufferedImage> loader;

        ResourceLocation explicit = cm.getTargetTexture(null);
        if (file != null) {
            // File-based edit: png lives next to the .json (or at <colormaps>/<explicit path>.png).
            Path dir = file.getParent();
            Path png;
            if (explicit != null) {
                png = dir.resolve(explicit.getPath() + ".png");
            } else {
                String name = file.getFileName().toString().replaceFirst("\\.json$", "");
                png = dir.resolve(name + ".png");
            }
            key = "file:" + png;
            loader = () -> readDisk(png);
        } else {
            // Read-only content view: resolve through the game resource manager.
            ResourceLocation base = explicit != null ? explicit : contentId;
            if (base == null) return null;
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(base.getNamespace(),
                    "colormaps/" + base.getPath() + ".png");
            key = "rl:" + rl;
            loader = () -> readResource(rl);
        }

        if (key.equals(cachedImageKey) && cachedArrayImage != null) return cachedArrayImage;

        BufferedImage img = loader.get();
        if (img == null) {
            cachedImageKey = null;
            cachedArrayImage = null;
            cachedDisplayImage = null;
            return null;
        }
        cachedImageKey = key;
        cachedArrayImage = toArrayImage(img);
        cachedDisplayImage = toOpaque(img);
        return cachedArrayImage;
    }

    private static @Nullable BufferedImage readDisk(Path png) {
        try {
            if (!Files.isRegularFile(png)) return null;
            return ImageIO.read(png.toFile());
        } catch (Exception e) {
            Polytone.LOGGER.warn("Colormap preview: failed to read {}", png, e);
            return null;
        }
    }

    private static @Nullable BufferedImage readResource(ResourceLocation rl) {
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open()) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            Polytone.LOGGER.warn("Colormap preview: failed to read {}", rl, e);
            return null;
        }
    }

    // Force alpha 255 to match the runtime ArrayImage loader (NativeImage RGBA -> opaque).
    private static ArrayImage toArrayImage(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][] px = new int[h][w];
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                px[yy][xx] = 0xFF000000 | (img.getRGB(xx, yy) & 0xFFFFFF);
            }
        }
        return new ArrayImage(px, w, h);
    }

    private static BufferedImage toOpaque(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                out.setRGB(xx, yy, img.getRGB(xx, yy) & 0xFFFFFF);
            }
        }
        return out;
    }

    // --- env slider row bound to one SimProxies input ------------------------------------------

    private static final class EnvControl {
        final SimValue input;
        final JLabel value = new JLabel();
        final JComponent row;
        private final JSlider slider;

        EnvControl(SimValue input, Runnable onChange) {
            this.input = input;
            int steps = Math.max(1, (int) Math.round((input.max() - input.min()) / input.step()));
            int start = (int) Math.round((input.value() - input.min()) / input.step());
            this.slider = new JSlider(0, steps, Math.clamp(start, 0, steps));
            slider.addChangeListener(e -> {
                input.set(input.min() + slider.getValue() * input.step());
                onChange.run();
            });
            this.row = labeled(input.label(), withValue(slider, value));
            value.setText(format());
        }

        String format() {
            return input.step() >= 1 ? String.format("%.0f", input.value()) : String.format("%.2f", input.value());
        }
    }

    // --- sink: captures the intermediates of one real sampling pass ----------------------------

    private static final class CaptureSink implements Colormap.SampleSink {
        boolean captured;
        float x, y;
        int col, row, argb;

        @Override
        public void report(float x, float y, int col, int row, int argb) {
            this.captured = true;
            this.x = x;
            this.y = y;
            this.col = col;
            this.row = row;
            this.argb = argb;
        }
    }

    // --- minimal BlockAndTintGetter for axis evaluation ----------------------------------------

    // A one-block world: the picked state at the origin, air elsewhere, the picked biome for tint, and
    // slider-driven light. Records whether sky/block light was queried so the preview can show those
    // sliders only when the expression uses them.
    private static final class SimLevel implements BlockAndTintGetter {
        private final BlockPos origin;
        private final BlockState state;
        private final @Nullable Biome biome;
        private final int sky;
        private final int block;
        boolean usedSky;
        boolean usedBlock;

        SimLevel(BlockPos origin, BlockState state, @Nullable Biome biome, int sky, int block) {
            this.origin = origin.immutable();
            this.state = state;
            this.biome = biome;
            this.sky = sky;
            this.block = block;
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return 1f;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return null;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
            return biome == null ? -1 : colorResolver.getColor(biome, pos.getX(), pos.getZ());
        }

        @Override
        public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
            if (lightLayer == LightLayer.SKY) {
                usedSky = true;
                return sky;
            }
            usedBlock = true;
            return block;
        }

        @Override
        public int getRawBrightness(BlockPos blockPos, int amount) {
            usedSky = true;
            usedBlock = true;
            return Math.max(block, sky - amount);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(origin) ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }
}
