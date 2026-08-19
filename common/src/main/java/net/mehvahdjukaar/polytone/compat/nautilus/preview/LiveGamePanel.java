package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewPanel;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiStyle;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

// Base for the previews whose real surface is the running game window: a creative screen, a container GUI.
// They can't draw anything themselves, so they're a remote control - push what you're editing onto the live
// screen, put it back, and pick things off it - and they all repeat the same three controls.
abstract class LiveGamePanel<T> extends PreviewPanel {

    protected final JButton applyButton;
    protected final JButton undoButton = UiStyle.buttonAsToolbar(new JButton("Undo preview"));
    protected final JToggleButton pickToggle;
    private final JLabel pickActive = PreviewLayout.ellipsizing(
            StyledLabels.accentSmall("● Overlay active in the game window"));
    private final String pickLabel;

    // Latest decoded value from the form; null while the form is invalid.
    protected @Nullable T current;

    protected LiveGamePanel(String applyLabel, String applyTooltip, String undoTooltip,
                            String pickLabel, String pickTooltip) {
        this.pickLabel = pickLabel;
        this.applyButton = UiStyle.ctaButton(applyLabel, UiICons.refresh());
        applyButton.setToolTipText(applyTooltip);
        applyButton.addActionListener(e -> apply());
        applyButton.setEnabled(false);

        undoButton.setToolTipText(undoTooltip);
        undoButton.addActionListener(e -> undo());

        this.pickToggle = new JToggleButton(pickLabel, UiICons.eye());
        pickToggle.setToolTipText(pickTooltip);
        pickToggle.setIconTextGap(UiScale.small());
        pickToggle.addActionListener(e -> togglePicking(pickToggle.isSelected()));
        pickActive.setVisible(false);
    }

    protected abstract Class<T> valueType();

    protected abstract void apply();

    protected abstract void undo();

    protected abstract void setPicking(boolean on);

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.current = valueType().isInstance(value) ? valueType().cast(value) : null;
        applyButton.setEnabled(current != null);
    }

    // False when the form hasn't decoded yet, with the reason already on the status line.
    protected boolean requireValue() {
        if (current != null) return true;
        statusError("No valid modifier yet - fix the form first.");
        return false;
    }

    private void togglePicking(boolean on) {
        pickActive.setVisible(on);
        pickToggle.setText(on ? "Stop picking" : pickLabel);
        setPicking(on);
    }

    protected void addPickControls(JPanel group) {
        PreviewLayout.add(group, pickToggle);
        PreviewLayout.add(group, pickActive);
    }

    // Titled toolbar row, a one-line blurb, the apply/undo pair, then the subclass's own group under it.
    protected void installPanel(String title, String blurb, JPanel group) {
        Box content = PreviewLayout.column();
        PreviewLayout.add(content, PreviewLayout.ellipsizing(StyledLabels.small(blurb)));
        PreviewLayout.ctaWithUndo(content, applyButton, undoButton);
        PreviewLayout.addFilling(content, group);
        install(content, row(PreviewLayout.title(title)));
    }
}
