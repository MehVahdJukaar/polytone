package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import net.mehvahdjukaar.nautilus.swing.toolkit.GroupPanels;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** Layout bits shared by the remote-control style preview panels (GUI modifiers, creative tabs). */
final class PreviewPanels {

    /** Adds a left-aligned, height-hugging row plus the standard gap under it. */
    static void addRow(JComponent box, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMaximumSize(UiScale.maxHeightHugging(comp));
        box.add(comp);
        box.add(Box.createVerticalStrut(UiScale.small()));
    }

    // Let a label shrink below its text width so it ellipsizes (with the full text in its tooltip)
    // instead of forcing the panel wider or clipping off the edge.
    static JLabel ellipsizing(JLabel label) {
        label.setToolTipText(label.getText());
        label.setMinimumSize(new Dimension(0, label.getPreferredSize().height));
        return label;
    }

    /** Sticky header: bold title on one row, status underneath. */
    static Box header(String title, JComponent status) {
        Box toolbar = Box.createVerticalBox();
        JLabel label = StyledLabels.of(title, l -> l.setFont(l.getFont().deriveFont(Font.BOLD)));
        label.setIcon(UiICons.viewPanel());
        label.setIconTextGap(UiScale.small());
        addRow(toolbar, label);
        addRow(toolbar, status);
        return toolbar;
    }

    /** The primary action as a full-width CTA, with a subtle undo right-aligned beneath it. */
    static void addCtaWithUndo(JComponent content, JButton cta, JButton undo) {
        addRow(content, cta);
        Box undoRow = Box.createHorizontalBox();
        undoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        undoRow.add(Box.createHorizontalGlue());
        undoRow.add(undo);
        addRow(content, undoRow);
        content.add(Box.createVerticalStrut(UiScale.med()));
    }

    /** Titled outlined group for the setup helpers, visually secondary to the CTA above them. */
    static JPanel outlinedGroup(String title) {
        JPanel group = GroupPanels.outlined();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        addRow(group, StyledLabels.muted(title));
        return group;
    }
}
