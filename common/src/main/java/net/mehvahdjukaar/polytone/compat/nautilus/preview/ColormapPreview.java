package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.render.BlockScene;
import net.mehvahdjukaar.nautilus.render.BlockTint;
import net.mehvahdjukaar.nautilus.swing.preview.PixelTextureView;
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
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

// Live preview for Polytone colormaps
public final class ColormapPreview extends ExpressionPreview {

    private final @Nullable Path file;
    private final @Nullable Identifier contentId;

    private final SceneViewport blockView = new SceneViewport();
    private final ColorSwatch swatch = new ColorSwatch();
    private final PixelTextureView imageView = new PixelTextureView();

    private final RegistryPickerField blockPicker;
    private final RegistryPickerField biomePicker;
    private final JSlider ySlider = new JSlider(-64, 320, 64);
    private final JLabel yLabel = StyledLabels.mutedSmall("");
    private final JLabel climateReadout = StyledLabels.mutedSmall(" ");

    private JPanel inputsBox;

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
        blockPicker.setSelected(Identifier.withDefaultNamespace("grass_block"));
        biomePicker.setSelected(Identifier.withDefaultNamespace("plains"));

        this.skyRow = labeled("Sky light", withValue(skySlider, skyLabel));
        this.blockRow = labeled("Block light", withValue(blockSlider, blockLabel));

        blockView.setPanEnabled(false); // small fixed thumbnail: orbit/zoom to inspect, but never pan off-view
        updateScene(Blocks.GRASS_BLOCK.defaultBlockState());

        buildLayout();

        ySlider.addChangeListener(e -> recompute());
        skySlider.addChangeListener(e -> recompute());
        blockSlider.addChangeListener(e -> recompute());

        recompute();
    }

    @Override
    public void dispose() {
        super.dispose();
        blockView.dispose();
    }

    private void buildLayout() {
        Box toolbar = Box.createVerticalBox();

        Box topRow = Box.createHorizontalBox();
        topRow.add(status);
        topRow.add(Box.createHorizontalGlue());
        addRow(toolbar, topRow);

        Box content = Box.createVerticalBox();

        // The block choice isn't a simulated input - it only picks which block the 3D view shows and
        // tints - so it lives outside the group and stays usable in every mode, live included.
        addField(content, labeled("Block", blockPicker));

        liveToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(liveToggle);
        content.add(Box.createVerticalStrut(UiScale.small()));

        inputsBox = GroupPanels.outlined();
        inputsBox.setLayout(new BoxLayout(inputsBox, BoxLayout.Y_AXIS));
        inputsBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel groupHeader = StyledLabels.muted("Simulated inputs");
        groupHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputsBox.add(groupHeader);
        inputsBox.add(Box.createVerticalStrut(UiScale.small()));

        addField(inputsBox, labeled("Biome", biomePicker));
        climateReadout.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputsBox.add(climateReadout);
        inputsBox.add(Box.createVerticalStrut(UiScale.med()));
        addField(inputsBox, labeled("Y level", withValue(ySlider, yLabel)));

        skyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        blockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputsBox.add(envGroup());
        inputsBox.add(skyRow);
        inputsBox.add(blockRow);

        content.add(inputsBox);
        content.add(Box.createVerticalStrut(UiScale.med()));

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
        root.setMinimumSize(new Dimension(UiScale.px(160), UiScale.px(120)));
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.colormap = value instanceof Colormap cm ? cm : null;
        recompute();
    }

    @Override
    protected void onLiveModeChanged(boolean live) {
        inputsBox.setVisible(!live);
    }

    @Override
    protected void recompute() {
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
        updateClimate(biome);
        updateScene(state);

        BlockPos pos = new BlockPos(0, y, 0);
        SimLevel level = new SimLevel(pos, state, biome, skySlider.getValue(), blockSlider.getValue());
        CaptureSink sink = new CaptureSink();

        installSim();
        try {
            cm.sampleColor(level, state, pos.getCenter(), biome, null, sink);
        } catch (Exception ex) {
            status.error("Sampling failed: " + ex.getMessage());
            clearResult();
            showSourceNoMarker();
            showEnv(level.usedSky, level.usedBlock);
            return;
        } finally {
            clearSim();
        }

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
        // The block stays the user's pick even in live mode; only the position/biome/light come live.
        BlockState state = resolveBlock(blockPicker.getSelected()).defaultBlockState();
        Biome biome = mc.level.getBiome(pos).value();

        updateClimate(biome);
        updateScene(state.isAir() ? Blocks.GRASS_BLOCK.defaultBlockState() : state);

        CaptureSink sink = new CaptureSink();
        try {
            cm.sampleColor(mc.level, state, pos.getCenter(), biome, null, sink);
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

    @Override
    protected void hideEnv() {
        super.hideEnv();
        skyRow.setVisible(false);
        blockRow.setVisible(false);
        inputsBox.revalidate();
        inputsBox.repaint();
    }

    private void showEnv(boolean usedSky, boolean usedBlock) {
        boolean anyEnv = refreshEnvControls();
        skyRow.setVisible(usedSky);
        blockRow.setVisible(usedBlock);
        setEnvHeaderVisible(anyEnv || usedSky || usedBlock);
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

    private static Block resolveBlock(@Nullable Identifier id) {
        if (id == null) return Blocks.AIR;
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
    }

    // Resolve against the same registry view the picker listed from (world registries when loaded, else
    // the cached vanilla ones), so any id the biome picker offered resolves back to a Biome here.
    private @Nullable Biome resolveBiome(@Nullable Identifier id) {
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

    private @Nullable ArrayImage resolveSourceImage(Colormap cm) {
        String key;
        Supplier<BufferedImage> loader;

        Identifier explicit = cm.getTargetTexture(null);
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
            Identifier base = explicit != null ? explicit : contentId;
            if (base == null) return null;
            Identifier rl = Identifier.fromNamespaceAndPath(base.getNamespace(),
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

    private static @Nullable BufferedImage readResource(Identifier rl) {
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

    // captures the intermediates of one real sampling pass

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

    // minimal BlockAndTintGetter for axis evaluation

    // A one-block world: the picked state at the origin, air elsewhere, the picked biome, slider-driven
    // light. Records whether sky/block light was queried so the panel can hide those sliders.
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
        public int getMinY() {
            return -64;
        }
    }
}
