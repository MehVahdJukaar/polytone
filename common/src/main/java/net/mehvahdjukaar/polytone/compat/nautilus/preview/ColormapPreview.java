package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.render.BlockScene;
import net.mehvahdjukaar.nautilus.render.BlockTint;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.SceneViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.ColorSwatch;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Live preview for Polytone colormaps.
 *
 * <p>It reuses the standard Nautilus 2D-preview chrome: a {@link PreviewSurface} (scrolling dark canvas
 * under a themed toolbar) holds the visuals - a small square block view and a same-size result swatch
 * side by side, then a bigger square of the source colormap texture with the sampled texel marked. The
 * toolbar carries the block/biome pickers, the y slider and the env/light sliders, plus the
 * "Live at player" toggle.
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

    private static final int TEXTURE_SIZE = 220;

    private final @Nullable Path file;
    private final @Nullable ResourceLocation contentId;

    private PreviewSurface root;

    private final SceneViewport blockView = new SceneViewport();
    private final ColorSwatch swatch = new ColorSwatch();
    private final ImageView imageView = new ImageView();
    private final PreviewStatus status = new PreviewStatus();

    // "Live at player" bypasses all simulation: sample the real level at the player's feet on a timer.
    private final JCheckBox liveToggle = new JCheckBox("Live at player");
    private boolean liveMode;
    private final Timer liveTimer = new Timer(500, e -> { if (liveMode) recompute(); });

    private final JComboBox<BlockEntry> blockPicker = new JComboBox<>();
    private final JComboBox<BiomeEntry> biomePicker = new JComboBox<>();
    private final JSlider ySlider = new JSlider(-64, 320, 64);
    private final JLabel yLabel = StyledLabels.mutedSmall("");
    private final JLabel climateReadout = StyledLabels.mutedSmall(" ");

    // Toolbar rows hidden while in live mode (their inputs come from the player instead).
    private Box pickersRow;
    private Box numericRow;

    // Env sliders, one per SimProxies input, shown only when the sampled expression reads them.
    private final SimProxies sim = new SimProxies();
    private final Map<SimValue, EnvControl> envControls = new LinkedHashMap<>();
    private final Box envContainer = Box.createVerticalBox();
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
        populateBlocks();
        populateBiomes();

        liveToggle.setOpaque(false);
        liveToggle.setToolTipText("Sample the real world at the player's feet with the live clock, instead of the simulated inputs.");
        liveToggle.addActionListener(e -> setLiveMode(liveToggle.isSelected()));
        blockPicker.addActionListener(e -> recompute());
        biomePicker.addActionListener(e -> recompute());
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
        // Toolbar: themed control rows stacked above the canvas (fixed height, so nothing stretches).
        Box toolbar = Box.createVerticalBox();

        Box topRow = Box.createHorizontalBox();
        topRow.add(liveToggle);
        topRow.add(Box.createHorizontalStrut(UiScale.med()));
        topRow.add(status);
        topRow.add(Box.createHorizontalGlue());
        addRow(toolbar, topRow);

        pickersRow = Box.createHorizontalBox();
        pickersRow.add(inline("Block", blockPicker));
        pickersRow.add(Box.createHorizontalStrut(UiScale.med()));
        pickersRow.add(inline("Biome", biomePicker));
        pickersRow.add(Box.createHorizontalStrut(UiScale.med()));
        pickersRow.add(climateReadout);
        pickersRow.add(Box.createHorizontalGlue());
        addRow(toolbar, pickersRow);

        numericRow = Box.createHorizontalBox();
        numericRow.add(inline("Y level", withValue(ySlider, yLabel)));
        addRow(toolbar, numericRow);

        // Env-global + light sliders (visibility driven by detection).
        envHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        envSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        skyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        blockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        envContainer.add(envHeader);
        envContainer.add(envSection);
        envContainer.add(skyRow);
        envContainer.add(blockRow);
        envContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.add(envContainer);

        // Canvas content: block view + result swatch, then the source texture with the sample marker.
        blockView.setBorder(UiTheme.hairlineBorder());
        imageView.setBorder(UiTheme.hairlineBorder());

        Box content = Box.createVerticalBox();
        SquareRow topSquares = new SquareRow(UiScale.med(), UiScale.px(84), UiScale.px(196), blockView, swatch);
        topSquares.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(topSquares);
        content.add(Box.createVerticalStrut(UiScale.med()));
        SquareRow textureRow = new SquareRow(0, UiScale.px(140), UiScale.px(400), imageView);
        textureRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(textureRow);

        root = new PreviewSurface(toolbar, content);
    }

    private static void addRow(Box toolbar, JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(UiScale.maxHeightHugging(row));
        toolbar.add(row);
        toolbar.add(Box.createVerticalStrut(UiScale.small()));
    }

    // Label over field (used for the sliders), field stretches horizontally but keeps its own height.
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

    // Label beside field (used for the pickers), field capped so it never grows to swallow the row.
    private static JComponent inline(String text, JComponent field) {
        Box row = Box.createHorizontalBox();
        JLabel l = StyledLabels.small(text);
        field.setMaximumSize(new Dimension(260, field.getPreferredSize().height));
        row.add(l);
        row.add(Box.createHorizontalStrut(6));
        row.add(field);
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
        pickersRow.setVisible(!live);
        numericRow.setVisible(!live);
        envContainer.setVisible(!live);
        if (live) liveTimer.start();
        else liveTimer.stop();
        root.revalidate();
        root.repaint();
        recompute();
    }

    private void recompute() {
        Colormap cm = this.colormap;
        if (cm == null) {
            status.info("Waiting for a valid colormap...");
            clearResult();
            imageView.set(null, -1, -1);
            hideEnv();
            return;
        }

        // The form-decoded colormap has no texture yet; load & attach the sibling .png so it can sample.
        if (cm.needsToFillTexture()) {
            ArrayImage img = resolveSourceImage(cm);
            if (img == null) {
                status.error("No source .png found for this colormap.");
                clearResult();
                imageView.set(null, -1, -1);
                hideEnv();
                return;
            }
            cm.acceptTexture(img);
        }

        if (liveMode) sampleLive(cm);
        else sampleSimulated(cm);
    }

    private void sampleSimulated(Colormap cm) {
        BlockEntry be = (BlockEntry) blockPicker.getSelectedItem();
        BiomeEntry bio = (BiomeEntry) biomePicker.getSelectedItem();
        BlockState state = be != null ? be.block.defaultBlockState() : Blocks.AIR.defaultBlockState();
        Biome biome = bio != null ? bio.biome : null;
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
            imageView.set(cachedDisplayImage, -1, -1);
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
            imageView.set(cachedDisplayImage, -1, -1);
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
            imageView.set(cachedDisplayImage, -1, -1);
            return;
        }
        applyResult(sink);
    }

    private void applyResult(CaptureSink sink) {
        if (!sink.captured) {
            status.info("Colormap produced no sample.");
            clearResult();
            imageView.set(cachedDisplayImage, -1, -1);
            return;
        }
        setResult(sink.argb, sink.col, sink.row);
        float u = markerFrac(sink.col, imageWidth());
        float v = markerFrac(sink.row, imageHeight());
        imageView.set(cachedDisplayImage, u, v);
        status.info(String.format("x_axis %.3f   y_axis %.3f", sink.x, sink.y));
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

    // Updates the swatch (fill + overlaid coordinates) and the 3D block tint, then re-renders the scene.
    private void setResult(int argb, int col, int row) {
        int rgb = argb & 0xFFFFFF;
        swatch.set(new Color(rgb), String.format("#%06X", rgb), String.format("@ %d, %d", col, row));
        currentTint = 0xFF000000 | rgb;
        blockView.refresh();
    }

    private void clearResult() {
        swatch.set(null);
        currentTint = -1;
        blockView.refresh();
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
        envContainer.revalidate();
        envContainer.repaint();
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

    // --- pickers -------------------------------------------------------------------------------

    private void populateBlocks() {
        List<BlockEntry> entries = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            entries.add(new BlockEntry(b, id.toString()));
        }
        entries.sort(Comparator.comparing(e -> e.label));
        blockPicker.setModel(new DefaultComboBoxModel<>(entries.toArray(new BlockEntry[0])));
        for (BlockEntry e : entries) {
            if (e.label.equals("minecraft:grass_block")) {
                blockPicker.setSelectedItem(e);
                break;
            }
        }
    }

    private void populateBiomes() {
        List<BiomeEntry> entries = new ArrayList<>();
        Registry<Biome> reg = biomeRegistry();
        if (reg != null) {
            for (Biome b : reg) {
                ResourceLocation id = reg.getKey(b);
                entries.add(new BiomeEntry(b, id == null ? "?" : id.toString()));
            }
            entries.sort(Comparator.comparing(e -> e.label));
        }
        if (entries.isEmpty()) {
            entries.add(new BiomeEntry(null, "(load a world for biomes)"));
        }
        biomePicker.setModel(new DefaultComboBoxModel<>(entries.toArray(new BiomeEntry[0])));
        for (BiomeEntry e : entries) {
            if (e.label.equals("minecraft:plains")) {
                biomePicker.setSelectedItem(e);
                break;
            }
        }
    }

    private static @Nullable Registry<Biome> biomeRegistry() {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (mc.level != null) {
                return mc.level.registryAccess().registryOrThrow(Registries.BIOME);
            }
            if (mc.getConnection() != null) {
                return mc.getConnection().registryAccess().registryOrThrow(Registries.BIOME);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private record BlockEntry(Block block, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record BiomeEntry(@Nullable Biome biome, String label) {
        @Override
        public String toString() {
            return label;
        }
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

    // --- 2D image view -------------------------------------------------------------------------

    private static final class ImageView extends JComponent {
        private @Nullable BufferedImage image;
        private float u = -1, v = -1;

        ImageView() {
            setPreferredSize(new Dimension(TEXTURE_SIZE, TEXTURE_SIZE));
        }

        void set(@Nullable BufferedImage image, float u, float v) {
            this.image = image;
            this.u = u;
            this.v = v;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int cw = getWidth();
            int ch = getHeight();
            g.setColor(PreviewSurface.canvasColor());
            g.fillRect(0, 0, cw, ch);

            BufferedImage img = image;
            if (img == null) {
                g.setColor(PreviewSurface.canvasTextColor());
                g.drawString("No colormap image", 10, 20);
                g.dispose();
                return;
            }

            // Fit the image into the component, preserving aspect ratio, nearest-neighbor.
            int iw = img.getWidth();
            int ih = img.getHeight();
            double scale = Math.min((double) cw / iw, (double) ch / ih);
            int dw = Math.max(1, (int) Math.round(iw * scale));
            int dh = Math.max(1, (int) Math.round(ih * scale));
            int dx = (cw - dw) / 2;
            int dy = (ch - dh) / 2;

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, dx, dy, dw, dh, null);
            g.setColor(UiTheme.dividerColor());
            g.drawRect(dx, dy, dw - 1, dh - 1);

            if (u >= 0 && v >= 0) {
                int mx = dx + Math.round(u * (dw - 1));
                int my = dy + Math.round(v * (dh - 1));
                g.setColor(Color.WHITE);
                g.drawLine(mx - 8, my, mx + 8, my);
                g.drawLine(mx, my - 8, mx, my + 8);
                g.setColor(Color.BLACK);
                g.drawOval(mx - 4, my - 4, 8, 8);
                g.setColor(Color.WHITE);
                g.drawOval(mx - 5, my - 5, 10, 10);
            }
            g.dispose();
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
