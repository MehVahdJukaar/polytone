package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public final class EitherOfWidget implements SwingWidget {

    private static final String LEFT = "Left";
    private static final String RIGHT = "Right";

    private final SwingWidget leftWidget;
    private final SwingWidget rightWidget;
    private final JComboBox<String> combo;
    private final JPanel root = new JPanel();
    private final JPanel subHost = new JPanel(new BorderLayout());
    private String selected = LEFT;

    public EitherOfWidget(Schema.EitherOf<?, ?> schema) {
        this.leftWidget = SwingWidgetFactory.create(schema.left());
        this.rightWidget = SwingWidgetFactory.create(schema.right());

        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, UiScale.px(6), 0));
        combo = new JComboBox<>(new String[]{LEFT, RIGHT});
        top.add(combo);
        root.add(top);
        root.add(javax.swing.Box.createVerticalStrut(UiScale.px(4)));
        root.add(subHost);

        combo.addActionListener(e -> {
            String sel = (String) combo.getSelectedItem();
            if (sel != null) swapSub(sel);
        });

        swapSub(LEFT);
    }

    private void swapSub(String which) {
        selected = which;
        subHost.removeAll();
        SwingWidget active = which.equals(LEFT) ? leftWidget : rightWidget;
        subHost.add(active.component(), BorderLayout.CENTER);
        subHost.revalidate();
        subHost.repaint();
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        SwingWidget active = selected.equals(LEFT) ? leftWidget : rightWidget;
        return active.currentJson();
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value == null) {
            return;
        }
        // Pragmatic: try left first, then right. Both setJson calls are best-effort.
        boolean leftOk = trySet(leftWidget, value);
        if (leftOk) {
            combo.setSelectedItem(LEFT);
            return;
        }
        boolean rightOk = trySet(rightWidget, value);
        if (rightOk) {
            combo.setSelectedItem(RIGHT);
            return;
        }
        combo.setSelectedItem(LEFT);
    }

    private static boolean trySet(SwingWidget widget, JsonElement value) {
        try {
            widget.setJson(value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
