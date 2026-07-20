package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.GroupPanels;
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
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * Editor preview for Polytone GUI modifiers. It renders nothing itself: GUI modifiers decorate
 * <i>foreign</i> screens, so the real preview surface is the live Minecraft window. This panel is a
 * thin, read-only remote control over the {@link GuiModifierPreview} bridge. Its primary action is
 * <b>Reload live screen</b> (push the edited modifier onto the open screen in memory, no resource
 * reload); the two helpers below just report what the game sees - the detected target, and slots
 * picked by clicking them in game - each with a Copy button that yields the raw value to paste. The
 * rich per-element detail lives on the in-game overlay, not here; nothing in this panel writes the form.
 */
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

    // --- actions --------------------------------------------------------------------------------

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
        // Show both bits; copy only the target string (the part that's a pain to type).
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

    // --- layout ---------------------------------------------------------------------------------

    private void buildLayout() {
        // Sticky header: title on one row, status underneath.
        Box toolbar = Box.createVerticalBox();
        JLabel title = StyledLabels.of("Live GUI Preview", l -> l.setFont(l.getFont().deriveFont(Font.BOLD)));
        title.setIcon(UiICons.viewPanel());
        title.setIconTextGap(UiScale.small());
        addRow(toolbar, title);
        addRow(toolbar, status);

        Box content = Box.createVerticalBox();

        addRow(content, ellipsizing(StyledLabels.small("Edits preview on the live game screen.")));
        content.add(Box.createVerticalStrut(UiScale.small()));

        // Primary action: the reload loop. Full-width CTA, with a subtle undo beneath it.
        addRow(content, reloadButton);
        Box undoRow = Box.createHorizontalBox();
        undoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        undoRow.add(Box.createHorizontalGlue());
        undoRow.add(undoButton);
        addRow(content, undoRow);
        content.add(Box.createVerticalStrut(UiScale.med()));

        // Setup helpers, grouped and visually secondary to the CTA above.
        JPanel group = GroupPanels.outlined();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        addRow(group, StyledLabels.muted("Inspect & target"));
        addRow(group, detectButton);
        addRow(group, detectReadout.component());
        group.add(Box.createVerticalStrut(UiScale.med()));
        addRow(group, pickToggle);
        addRow(group, ellipsizing(pickActive));
        addRow(group, pickReadout.component());
        content.add(group);

        root = new PreviewSurface(toolbar, content);
        root.setMinimumSize(new Dimension(UiScale.px(180), UiScale.px(140)));
    }

    private static void addRow(JComponent box, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMaximumSize(UiScale.maxHeightHugging(comp));
        box.add(comp);
        box.add(Box.createVerticalStrut(UiScale.small()));
    }

    // Let a label shrink below its text width so it ellipsizes (with the full text in its tooltip)
    // instead of forcing the panel wider or clipping off the edge.
    private static JLabel ellipsizing(JLabel label) {
        label.setToolTipText(label.getText());
        label.setMinimumSize(new Dimension(0, label.getPreferredSize().height));
        return label;
    }

    // --- a labelled value with a copy button, used for the detect/pick read-outs -----------------

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
