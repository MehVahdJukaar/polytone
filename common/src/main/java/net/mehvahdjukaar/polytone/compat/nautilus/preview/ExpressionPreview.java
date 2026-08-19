package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import net.mehvahdjukaar.nautilus.swing.preview.LabeledSlider;
import net.mehvahdjukaar.nautilus.swing.preview.LivePreview;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.polytone.common.expressions.preview.PreviewContext;
import net.mehvahdjukaar.polytone.common.expressions.preview.SimProxies;
import net.mehvahdjukaar.polytone.common.expressions.preview.SimValue;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

// Shared scaffolding for the live expression previews: the SimProxies harness, which reveals only the
// sliders a pass actually read. Subclasses wrap each evaluation in installSim()/clearSim(), then call
// refreshEnvControls().
public abstract class ExpressionPreview extends LivePreview {

    // One SimProxies per panel, so slider state is independent per open tab.
    protected final SimProxies sim = new SimProxies();
    private final Map<SimValue, LabeledSlider> envControls = new LinkedHashMap<>();
    private final Box envSection = Box.createVerticalBox();
    private final JLabel envHeader = StyledLabels.muted("Environment");

    protected ExpressionPreview() {
        super("Live at player", "Sample the real world at the player instead of the simulated inputs.");
        // One row per sim input, hidden until an evaluation is seen reading it.
        for (SimValue v : sim.values()) {
            LabeledSlider slider = new LabeledSlider(v.label(), v.min(), v.max(), v.step(), v.value(),
                    value -> {
                        v.set(value);
                        recompute();
                    });
            slider.setVisible(false);
            envControls.put(v, slider);
            envSection.add(slider);
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
        for (Map.Entry<SimValue, LabeledSlider> e : envControls.entrySet()) {
            boolean show = e.getKey().wasRead();
            e.getValue().setVisible(show);
            e.getValue().refreshReadout();
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
}
