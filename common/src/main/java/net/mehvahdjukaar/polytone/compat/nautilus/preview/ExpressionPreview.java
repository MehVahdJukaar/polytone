package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.PreviewContext;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.SimProxies;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.SimValue;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

// Shared scaffolding for the editor's live expression previews (colormaps, particles, ...): the
// PreviewSurface chrome, the "Live at player" toggle with its refresh timer, and the SimProxies
// harness feeding simulated global/camera/player inputs, revealing only the sliders a pass read.
// Subclasses wrap each evaluation in installSim()/clearSim(), then call refreshEnvControls().
public abstract class ExpressionPreview implements TabPreview {

    protected final PreviewStatus status = new PreviewStatus();

    // "Live at player" bypasses simulation: subclasses sample/tick against the real world on a timer.
    protected final JCheckBox liveToggle = new JCheckBox("Live at player");
    protected boolean liveMode;
    private final Timer liveTimer = new Timer(500, e -> { if (liveMode) recompute(); });

    // One SimProxies per panel, so slider state is independent per open tab.
    protected final SimProxies sim = new SimProxies();
    private final Map<SimValue, EnvControl> envControls = new LinkedHashMap<>();
    private final Box envSection = Box.createVerticalBox();
    private final JLabel envHeader = StyledLabels.muted("Environment");

    protected PreviewSurface root;

    protected ExpressionPreview() {
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

        liveToggle.setOpaque(false);
        liveToggle.setToolTipText("Sample the real world at the player instead of the simulated inputs.");
        liveToggle.addActionListener(e -> setLiveMode(liveToggle.isSelected()));
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void dispose() {
        liveTimer.stop();
    }

    protected abstract void recompute();

    protected void setLiveMode(boolean live) {
        this.liveMode = live;
        onLiveModeChanged(live);
        if (live) liveTimer.start();
        else liveTimer.stop();
        if (root != null) {
            root.revalidate();
            root.repaint();
        }
        recompute();
    }

    protected void onLiveModeChanged(boolean live) {}

    // embed once in the subclass layout
    protected JComponent envGroup() {
        Box box = Box.createVerticalBox();
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
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

    // returns whether any env slider is shown, so subclasses adding their own rows under the same
    // header can OR that into setEnvHeaderVisible
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

    protected static void addRow(Box toolbar, JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(UiScale.maxHeightHugging(row));
        toolbar.add(row);
        toolbar.add(Box.createVerticalStrut(UiScale.small()));
    }

    // Adds a full-width field to a vertical group, capped to its own height so it doesn't stretch.
    protected static void addField(JComponent box, JComponent field) {
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(UiScale.maxHeightHugging(field));
        box.add(field);
        box.add(Box.createVerticalStrut(UiScale.small()));
    }

    // Label over field, field stretches horizontally but keeps its own height.
    protected static JComponent labeled(String text, JComponent field) {
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

    protected static JComponent withValue(JSlider slider, JLabel value) {
        Box row = Box.createHorizontalBox();
        slider.setAlignmentY(Component.CENTER_ALIGNMENT);
        value.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(slider);
        row.add(Box.createHorizontalStrut(6));
        row.add(value);
        return row;
    }

    // env slider row bound to one SimProxies input

    protected static final class EnvControl {
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
}