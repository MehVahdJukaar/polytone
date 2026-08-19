package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import net.mehvahdjukaar.nautilus.swing.preview.LivePreview;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.polytone.common.expressions.preview.PreviewContext;
import net.mehvahdjukaar.polytone.common.expressions.preview.SimProxies;
import net.mehvahdjukaar.polytone.common.expressions.preview.SimValue;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

// Shared scaffolding for the live expression previews: the SimProxies harness, which reveals only the
// sliders a pass actually read. Subclasses wrap each evaluation in installSim()/clearSim(), then call
// refreshEnvControls().
public abstract class ExpressionPreview extends LivePreview {

    // One SimProxies per panel, so slider state is independent per open tab.
    protected final SimProxies sim = new SimProxies();
    private final Map<SimValue, EnvControl> envControls = new LinkedHashMap<>();
    private final Box envSection = Box.createVerticalBox();
    private final JLabel envHeader = StyledLabels.muted("Environment");

    protected ExpressionPreview() {
        super("Live at player", "Sample the real world at the player instead of the simulated inputs.");
        // One row per sim input, hidden until an evaluation is seen reading it.
        for (SimValue v : sim.values()) {
            EnvControl c = new EnvControl(v, this::recompute);
            c.row.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.row.setVisible(false);
            envControls.put(v, c);
            envSection.add(c.row);
        }
        envHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        envSection.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    protected JComponent envGroup() {
        Box box = PreviewLayout.column();
        box.add(envHeader);
        box.add(envSection);
        return box;
    }

    protected void installSim() {
        sim.clearReads();
        PreviewContext.install(sim);
    }

    protected void clearSim() {
        PreviewContext.clear();
    }

    // returns whether any env slider is shown, so subclasses adding rows under the same header can OR it
    protected boolean refreshEnvControls() {
        boolean any = false;
        for (EnvControl c : envControls.values()) {
            boolean show = c.input.wasRead();
            c.row.setVisible(show);
            c.value.setText(c.format());
            any |= show;
        }
        envHeader.setVisible(any);
        return any;
    }

    protected void setEnvHeaderVisible(boolean visible) {
        envHeader.setVisible(visible);
    }

    protected void hideEnv() {
        sim.clearReads();
        refreshEnvControls();
    }

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
            this.row = PreviewLayout.labeled(input.label(), PreviewLayout.withValue(slider, value));
            value.setText(format());
        }

        String format() {
            return input.step() >= 1 ? String.format("%.0f", input.value()) : String.format("%.2f", input.value());
        }
    }
}
