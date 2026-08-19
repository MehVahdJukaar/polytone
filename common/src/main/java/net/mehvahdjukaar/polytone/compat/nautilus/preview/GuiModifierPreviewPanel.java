package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.CopyableReadout;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiStyle;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifier;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// GUI modifiers decorate foreign screens, so the real preview surface is the live Minecraft window and
// this panel is only a read-only remote control over GuiModifierPreview.
public final class GuiModifierPreviewPanel extends LiveGamePanel<GuiModifier> {

    private final JButton detectButton = UiStyle.primaryButton("Detect open screen", UiICons.search());
    private final CopyableReadout detectReadout = new CopyableReadout("Open a screen in game, then detect it.");
    private final CopyableReadout pickReadout = new CopyableReadout("Toggle picking, then click a slot in game.");

    public GuiModifierPreviewPanel(TabPreview.Context ctx) {
        super("Reload live screen",
                "Push the modifier you're editing onto the open screen instantly (no reload).",
                "Remove the live preview and restore the screen to its saved/loaded state.",
                "Pick in game",
                "Outline slots and widgets on the open screen; hover for details, click a slot to grab it.");

        detectButton.setToolTipText("Read the currently open screen's target_type / target.");
        detectButton.addActionListener(e -> detectScreen());

        JPanel group = PreviewLayout.group("Inspect & target");
        PreviewLayout.add(group, detectButton);
        PreviewLayout.add(group, detectReadout);
        group.add(Box.createVerticalStrut(UiScale.med()));
        addPickControls(group);
        PreviewLayout.add(group, pickReadout);

        installPanel("Live GUI Preview", "Edits preview on the live game screen.", group);
        statusText("Open the target screen in game, then use these controls.");
    }

    @Override
    protected Class<GuiModifier> valueType() {
        return GuiModifier.class;
    }

    @Override
    public void dispose() {
        GuiModifierPreview.setPickingEnabled(false);
        GuiModifierPreview.setPickListener(null);
        GuiModifierPreview.pushPreview(null);
    }

    @Override
    protected void apply() {
        if (!requireValue()) return;
        GuiModifierPreview.pushPreview(current);
        statusText("Pushed the modifier to the open screen.");
    }

    @Override
    protected void undo() {
        GuiModifierPreview.pushPreview(null);
        statusText("Live preview removed - screen restored to its saved state.");
    }

    @Override
    protected void setPicking(boolean on) {
        GuiModifierPreview.setPickingEnabled(on);
        if (on) {
            GuiModifierPreview.setPickListener(picked -> SwingUtilities.invokeLater(() -> showPicked(picked)));
            statusText("Overlay active: modified elements are marked; click a slot to grab it.");
        } else {
            GuiModifierPreview.setPickListener(null);
            statusText("");
        }
    }

    private void detectScreen() {
        Minecraft.getInstance().execute(() -> {
            GuiModifierPreview.DetectedTarget t = GuiModifierPreview.detectCurrentScreen();
            SwingUtilities.invokeLater(() -> onDetected(t));
        });
    }

    private void onDetected(@Nullable GuiModifierPreview.DetectedTarget t) {
        if (t == null) {
            statusError("No screen open in the game window.");
            detectReadout.clear();
            return;
        }
        detectReadout.set(t.type().getSerializedName() + " = " + t.target(), t.target());
        statusText("Detected the open screen.");
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
}
