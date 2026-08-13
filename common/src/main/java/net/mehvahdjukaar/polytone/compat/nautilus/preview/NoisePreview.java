package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.swing.preview.PixelTextureView;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
import net.mehvahdjukaar.polytone.content.noise.NoiseManager.NoiseConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

// samples through the exact call the runtime expression functions use, so the image matches what
// noise_*(x, y) returns in game
public final class NoisePreview implements TabPreview {

    private static final int IMAGE_SIZE = 160;

    private final PreviewStatus status = new PreviewStatus();
    private final PixelTextureView imageView = new PixelTextureView();
    // World span across the image, in blocks; larger = more zoomed out (more features visible).
    private final JSlider spanSlider = new JSlider(4, 128, 32);
    private final JLabel spanLabel = StyledLabels.mutedSmall("");
    private final PreviewSurface root;

    private @Nullable NoiseConfig config;

    public NoisePreview(TabPreview.Context ctx) {
        Box toolbar = Box.createVerticalBox();
        Box topRow = Box.createHorizontalBox();
        topRow.add(status);
        topRow.add(Box.createHorizontalGlue());
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.add(topRow);

        Box content = Box.createVerticalBox();

        JComponent spanRow = labeled("Area shown", withValue(spanSlider, spanLabel));
        spanRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(spanRow);
        content.add(Box.createVerticalStrut(UiScale.med()));

        imageView.setBorder(UiTheme.hairlineBorder());
        imageView.setPlaceholder("Waiting for a valid noise...");
        SquareRow imageRow = new SquareRow(0, UiScale.px(160), UiScale.px(400), imageView);
        imageRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(imageRow);

        spanSlider.addChangeListener(e -> recompute());

        root = new PreviewSurface(toolbar, content);
        root.setMinimumSize(new Dimension(UiScale.px(160), UiScale.px(120)));

        recompute();
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.config = value instanceof NoiseConfig nc ? nc : null;
        recompute();
    }

    private void recompute() {
        int span = spanSlider.getValue();
        spanLabel.setText(span + " blocks");

        NoiseConfig cfg = this.config;
        if (cfg == null) {
            status.info("Waiting for a valid noise...");
            clearImage();
            return;
        }

        PerlinSimplexNoise noise;
        try {
            noise = cfg.build();
        } catch (Exception ex) {
            status.error("Invalid noise: " + ex.getMessage());
            clearImage();
            return;
        }

        imageView.setImage(render(noise, span));
        imageView.setCaption(String.format("seed %d   octaves %s", cfg.seed(), cfg.octaves()));
        status.setText("");
    }

    private void clearImage() {
        imageView.setImage(null);
        imageView.setCaption(null);
    }

    private static BufferedImage render(PerlinSimplexNoise noise, int spanBlocks) {
        BufferedImage img = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        double step = spanBlocks / (double) IMAGE_SIZE;
        for (int py = 0; py < IMAGE_SIZE; py++) {
            for (int px = 0; px < IMAGE_SIZE; px++) {
                double v = noise.getValue(px * step, py * step, false);
                int g = (int) Math.round(Mth.clamp((v + 1) * 0.5, 0, 1) * 255);
                img.setRGB(px, py, (g << 16) | (g << 8) | g);
            }
        }
        return img;
    }

    private static JComponent labeled(String text, JComponent field) {
        Box row = Box.createVerticalBox();
        JLabel l = StyledLabels.small(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        row.add(l);
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
}
