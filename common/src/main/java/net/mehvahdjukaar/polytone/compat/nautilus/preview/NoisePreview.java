package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.nautilus.swing.preview.PixelTextureView;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewPanel;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JSlider;
import java.awt.image.BufferedImage;

// Live preview for Polytone noises. Renders the decoded PerlinSimplexNoise field as a grayscale image, sampled
// through the exact call the runtime expression functions use (getValue(x, y, false)), so the picture matches
// what noise_*(x, y) returns in game.
public final class NoisePreview extends PreviewPanel {

    private static final int IMAGE_SIZE = 160;

    private final PixelTextureView imageView = new PixelTextureView();
    // World span across the image, in blocks; larger = more zoomed out (more features visible).
    private final JSlider spanSlider = new JSlider(4, 128, 32);
    private final JLabel spanLabel = StyledLabels.mutedSmall("");

    // The 1.21.11 noise codec decodes straight to a PerlinSimplexNoise, so we render from the value
    // and keep the raw json only for the seed/octaves caption.
    private @Nullable PerlinSimplexNoise noise;
    private @Nullable String caption;

    public NoisePreview(TabPreview.Context ctx) {
        Box content = PreviewLayout.column();
        PreviewLayout.add(content, PreviewLayout.labeled("Area shown",
                PreviewLayout.withValue(spanSlider, spanLabel)));
        content.add(Box.createVerticalStrut(UiScale.med()));

        imageView.setBorder(UiTheme.hairlineBorder());
        imageView.setPlaceholder("Waiting for a valid noise...");
        PreviewLayout.addFilling(content, new SquareRow(0, UiScale.px(160), UiScale.px(400), imageView));

        spanSlider.addChangeListener(e -> recompute());

        install(content);
        recompute();
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.noise = value instanceof PerlinSimplexNoise n ? n : null;
        this.caption = captionFrom(json);
        recompute();
    }

    private void recompute() {
        int span = spanSlider.getValue();
        spanLabel.setText(span + " blocks");

        PerlinSimplexNoise n = this.noise;
        if (n == null) {
            statusText("Waiting for a valid noise...");
            imageView.setImage(null);
            imageView.setCaption(null);
            return;
        }

        imageView.setImage(render(n, span));
        imageView.setCaption(caption);
        statusText("");
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

    // Best-effort "seed N   octaves [...]" line straight off the source json (the decoded
    // PerlinSimplexNoise doesn't expose its inputs).
    private static @Nullable String captionFrom(@Nullable JsonElement json) {
        if (!(json instanceof JsonObject obj)) return null;
        try {
            String seed = obj.has("seed") ? obj.get("seed").getAsString() : "?";
            String octaves = obj.has("octaves") ? obj.get("octaves").toString() : "?";
            return String.format("seed %s   octaves %s", seed, octaves);
        } catch (Exception e) {
            return null;
        }
    }
}
