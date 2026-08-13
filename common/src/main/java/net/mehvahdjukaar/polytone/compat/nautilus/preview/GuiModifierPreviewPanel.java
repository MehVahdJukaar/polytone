package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiStyle;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifier;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

// GUI modifiers decorate foreign screens, so the real preview surface is the live Minecraft window and
// this panel is only a read-only remote control over GuiModifierPreview.
public final class GuiModifierPreviewPanel implements TabPreview {

    private PreviewSurface root;
    private final PreviewStatus status = new PreviewStatus();

    private final JButton reloadButton = UiStyle.ctaButton("Reload live screen", UiICons.refresh());
    private final JButton undoButton = UiStyle.buttonAsToolbar(new JButton("Undo preview"));

    private final JButton detectButton = UiStyle.primaryButton("Detect open screen", UiICons.search());
    private final JToggleButton pickToggle = new JToggleButton("Pick in game", UiICons.eye());
    private final JLabel pickActive = StyledLabels.accentSmall("● Overlay active in the game window");

    private final Readout detectReadout = new Readout("Open a screen in game, then detect it.");
    private final Readout pickReadout = new Readout("Toggle picking, then click a slot in game.");

    // Latest decoded modifier from the form; drives "reload". Null while the form is invalid.
    private @Nullable GuiModifier currentModifier;

    public GuiModifierPreviewPanel(TabPreview.Context ctx) {
        buildLayout();

        reloadButton.setToolTipText("Push the modifier you're editing onto the open screen instantly (no reload).");
        reloadButton.addActionListener(e -> reloadLiveScreen());
        reloadButton.setEnabled(false);

        undoButton.setToolTipText("Remove the live preview and restore the screen to its saved/loaded state.");
        undoButton.addActionListener(e -> {
            GuiModifierPreview.pushPreview(null);
            status.info("Live preview removed - screen restored to its saved state.");
        });

        detectButton.setToolTipText("Read the currently open screen's target_type / target.");
        detectButton.addActionListener(e -> detectScreen());

        pickToggle.setToolTipText("Outline slots and widgets on the open screen; hover for details, click a slot to grab it.");
        pickToggle.setIconTextGap(UiScale.small());
        pickToggle.addActionListener(e -> setPicking(pickToggle.isSelected()));
        pickActive.setVisible(false);

        status.info("Open the target screen in game, then use these controls.");
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.currentModifier = value instanceof GuiModifier gm ? gm : null;
        reloadButton.setEnabled(currentModifier != null);
    }

    @Override
    public void dispose() {
        GuiModifierPreview.setPickingEnabled(false);
        GuiModifierPreview.setPickListener(null);
        GuiModifierPreview.pushPreview(null);
    }

    private void reloadLiveScreen() {
        if (currentModifier == null) {
            status.error("No valid modifier yet - fix the form first.");
            return;
        }
        GuiModifierPreview.pushPreview(currentModifier);
        status.info("Pushed the modifier to the open screen.");
    }

    private void detectScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            GuiModifierPreview.DetectedTarget t = GuiModifierPreview.detectCurrentScreen();
            SwingUtilities.invokeLater(() -> onDetected(t));
        });
    }

    private void onDetected(@Nullable GuiModifierPreview.DetectedTarget t) {
        if (t == null) {
            status.error("No screen open in the game window.");
            detectReadout.clear();
            return;
        }
        detectReadout.set(t.type().getSerializedName() + " = " + t.target(), t.target());
        status.info("Detected the open screen.");
    }

    private void setPicking(boolean on) {
        GuiModifierPreview.setPickingEnabled(on);
        pickActive.setVisible(on);
        pickToggle.setText(on ? "Stop picking" : "Pick in game");
        if (on) {
            GuiModifierPreview.setPickListener(picked -> SwingUtilities.invokeLater(() -> showPicked(picked)));
            status.info("Overlay active: modified elements are marked; click a slot to grab it.");
        } else {
            GuiModifierPreview.setPickListener(null);
            status.setText("");
        }
    }

    private void showPicked(GuiModifierPreview.PickedElement picked) {
        String cls = StrUtils.simpleName(picked.className());
        if (picked.slotIndex() >= 0) {
            // Copy just the index - it drops straight into a "slots" entry.
            pickReadout.set("slot #" + picked.slotIndex() + "  @ (" + picked.x() + ", " + picked.y() + ")  " + cls,
                    String.valueOf(picked.slotIndex()));
        } else {
            pickReadout.set("element  @ (" + picked.x() + ", " + picked.y() + ")  " + cls,
                    picked.x() + ", " + picked.y());
        }
    }

    private void buildLayout() {
        Box toolbar = PreviewPanels.header("Live GUI Preview", status);

        Box content = Box.createVerticalBox();
        PreviewPanels.addRow(content, PreviewPanels.ellipsizing(
                StyledLabels.small("Edits preview on the live game screen.")));
        content.add(Box.createVerticalStrut(UiScale.small()));
        PreviewPanels.addCtaWithUndo(content, reloadButton, undoButton);

        JPanel group = PreviewPanels.outlinedGroup("Inspect & target");
        PreviewPanels.addRow(group, detectButton);
        PreviewPanels.addRow(group, detectReadout.component());
        group.add(Box.createVerticalStrut(UiScale.med()));
        PreviewPanels.addRow(group, pickToggle);
        PreviewPanels.addRow(group, PreviewPanels.ellipsizing(pickActive));
        PreviewPanels.addRow(group, pickReadout.component());
        content.add(group);

        root = new PreviewSurface(toolbar, content);
        root.setMinimumSize(new Dimension(UiScale.px(180), UiScale.px(140)));
    }

    // a labelled value with a copy button, used for the detect/pick read-outs

    private static final class Readout {
        private final JPanel row;
        private final JLabel value;
        private final JButton copy;
        private final String placeholder;
        private @Nullable String clipboard;

        Readout(String placeholder) {
            this.placeholder = placeholder;
            this.value = StyledLabels.small(placeholder);
            this.value.setToolTipText(placeholder);
            this.value.setMinimumSize(new Dimension(0, value.getPreferredSize().height)); // shrink -> ellipsis
            this.copy = UiStyle.toolbarButton(UiICons.copy(), "Copy value to clipboard", e -> copyToClipboard());
            copy.setVisible(false);

            this.row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            value.setAlignmentY(Component.CENTER_ALIGNMENT);
            copy.setAlignmentY(Component.CENTER_ALIGNMENT);
            row.add(value);
            row.add(Box.createHorizontalStrut(UiScale.small()));
            row.add(Box.createHorizontalGlue());
            row.add(copy);
        }

        JComponent component() {
            return row;
        }

        void set(String display, String clipboardValue) {
            this.clipboard = clipboardValue;
            value.setText(display);
            value.setToolTipText(display + "   (copies: " + clipboardValue + ")");
            copy.setToolTipText("Copy \"" + clipboardValue + "\" to clipboard");
            copy.setVisible(true);
        }

        void clear() {
            this.clipboard = null;
            value.setText(placeholder);
            value.setToolTipText(placeholder);
            copy.setVisible(false);
        }

        private void copyToClipboard() {
            if (clipboard == null) return;
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(clipboard), null);
        }
    }
}
