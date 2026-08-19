package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.render.BlockScene;
import net.mehvahdjukaar.nautilus.render.BlockTint;
import net.mehvahdjukaar.nautilus.swing.preview.LabeledSlider;
import net.mehvahdjukaar.nautilus.swing.preview.PixelTextureView;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewImages;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.SceneViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.ColorSwatch;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

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

    private final LabeledSlider skySlider = new LabeledSlider("Sky light", 0, 15, 1, 15, v -> recompute());
    private final LabeledSlider blockSlider = new LabeledSlider("Block light", 0, 15, 1, 0, v -> recompute());

    private volatile int currentTint = -1;
    private final BlockTint blockTint = (state, tintIndex) -> currentTint;
    private @Nullable BlockState sceneState;

    private @Nullable Colormap colormap;

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

        blockView.setPanEnabled(false); // small fixed thumbnail: orbit/zoom to inspect, but never pan off-view
        updateScene(Blocks.GRASS_BLOCK.defaultBlockState());

        buildLayout();

        ySlider.addChangeListener(e -> recompute());

        recompute();
    }

    @Override
    public void dispose() {
        super.dispose();
        blockView.dispose();
    }

    private void buildLayout() {
        Box content = PreviewLayout.column();

        PreviewLayout.add(content, PreviewLayout.labeled("Block", blockPicker));
        PreviewLayout.add(content, liveToggle);

        inputsBox = PreviewLayout.group("Simulated inputs");
        PreviewLayout.add(inputsBox, PreviewLayout.labeled("Biome", biomePicker));
        PreviewLayout.add(inputsBox, climateReadout);
        inputsBox.add(Box.createVerticalStrut(UiScale.small()));
        PreviewLayout.add(inputsBox, PreviewLayout.labeled("Y level",
                PreviewLayout.withValue(ySlider, yLabel)));

        PreviewLayout.addFilling(inputsBox, envGroup());
        PreviewLayout.addFilling(inputsBox, skySlider);
        PreviewLayout.addFilling(inputsBox, blockSlider);

        PreviewLayout.addFilling(content, inputsBox);
        content.add(Box.createVerticalStrut(UiScale.med()));

        blockView.setBorder(UiTheme.hairlineBorder());
        imageView.setBorder(UiTheme.hairlineBorder());

        PreviewLayout.addFilling(content,
                new SquareRow(UiScale.med(), UiScale.px(84), UiScale.px(196), blockView, swatch));
        content.add(Box.createVerticalStrut(UiScale.med()));
        PreviewLayout.addFilling(content, new SquareRow(0, UiScale.px(140), UiScale.px(400), imageView));

        install(content);
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.colormap = value instanceof Colormap cm ? cm : null;
        recompute();
    }

    @Override
    protected void onLiveChanged(boolean live) {
        inputsBox.setVisible(!live);
    }

    @Override
    protected void recompute() {
        Colormap cm = this.colormap;
        if (cm == null) {
            statusText("Waiting for a valid colormap...");
            clearResult();
            clearImage();
            hideEnv();
            return;
        }

        // The form-decoded colormap has no texture yet; load & attach the sibling .png so it can sample.
        if (cm.needsToFillTexture()) {
            ArrayImage img = resolveSourceImage(cm);
            if (img == null) {
                statusError("No source .png found for this colormap.");
                clearResult();
                clearImage();
                hideEnv();
                return;
            }
            cm.acceptTexture(img);
        }

        if (isLive()) sampleLive(cm);
        else sampleSimulated(cm);
    }

    private void sampleSimulated(Colormap cm) {
        BlockState state = resolveBlock(blockPicker.getSelected()).defaultBlockState();
        Biome biome = biomePicker.getSelectedValue(Registries.BIOME);
        int y = ySlider.getValue();
        yLabel.setText("y = " + y);
        updateClimate(biome);
        updateScene(state);

        BlockPos pos = new BlockPos(0, y, 0);
        SimLevel level = new SimLevel(pos, state, biome, (int) skySlider.value(), (int) blockSlider.value());
        CaptureSink sink = new CaptureSink();

        installSim();
        try {
            cm.sampleColor(level, state, pos.getCenter(), biome, null, sink);
        } catch (Exception ex) {
            statusError("Sampling failed: " + ex.getMessage());
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

    private void sampleLive(Colormap cm) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            statusText("No world loaded - can't sample at the player.");
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
            statusError("Sampling failed: " + ex.getMessage());
            clearResult();
            showSourceNoMarker();
            return;
        }
        applyResult(sink);
    }

    private void applyResult(CaptureSink sink) {
        if (!sink.captured) {
            statusText("Colormap produced no sample.");
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
        statusText("");
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

    private void showSourceNoMarker() {
        imageView.setImage(cachedDisplayImage);
        imageView.clearMarker();
        imageView.setCaption(null);
    }

    @Override
    protected void hideEnv() {
        super.hideEnv();
        skySlider.setVisible(false);
        blockSlider.setVisible(false);
        inputsBox.revalidate();
        inputsBox.repaint();
    }

    private void showEnv(boolean usedSky, boolean usedBlock) {
        boolean anyEnv = refreshEnvControls();
        skySlider.setVisible(usedSky);
        blockSlider.setVisible(usedBlock);
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
            loader = () -> PreviewImages.readFile(png);
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
        cachedDisplayImage = PreviewImages.toOpaque(img);
        return cachedArrayImage;
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
