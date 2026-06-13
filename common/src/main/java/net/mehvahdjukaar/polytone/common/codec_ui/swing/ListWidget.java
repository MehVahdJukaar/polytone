package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public final class ListWidget implements SwingWidget {

    private final Schema<?> elementSchema;
    private final JPanel root = new JPanel();
    private final JPanel rowsHost = new JPanel();
    private final List<SwingWidget> rowWidgets = new ArrayList<>();
    private final List<JPanel> rowPanels = new ArrayList<>();

    public ListWidget(Schema.ListOf<?> schema) {
        this.elementSchema = schema.element();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        rowsHost.setLayout(new BoxLayout(rowsHost, BoxLayout.Y_AXIS));
        rowsHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(rowsHost);

        root.add(Box.createVerticalStrut(UiScale.px(4)));

        JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton add = new JButton("+ Add");
        add.putClientProperty("JButton.buttonType", "roundRect");
        add.addActionListener(e -> {
            addRow(null);
            root.revalidate();
            root.repaint();
        });
        addBar.add(add);
        root.add(addBar);
    }

    private void addRow(@Nullable JsonElement initialValue) {
        SwingWidget child = SwingWidgetFactory.create(elementSchema);
        if (initialValue != null) {
            child.setJson(initialValue);
        }
        rowWidgets.add(child);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(child.component());
        row.add(Box.createHorizontalStrut(UiScale.px(6)));

        JButton remove = new JButton("×"); // U+00D7 MULTIPLICATION SIGN
        remove.setToolTipText("Remove");
        remove.putClientProperty("JButton.buttonType", "borderless");
        remove.setMargin(UiScale.insets(0, 4, 0, 4));
        remove.addActionListener(e -> {
            int idx = rowPanels.indexOf(row);
            if (idx >= 0) {
                rowsHost.remove(row);
                rowPanels.remove(idx);
                rowWidgets.remove(idx);
                root.revalidate();
                root.repaint();
            }
        });
        row.add(remove);

        // Keep rows from stretching vertically
        Dimension pref = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        rowPanels.add(row);
        rowsHost.add(row);
        rowsHost.add(Box.createVerticalStrut(UiScale.px(4)));
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        JsonArray array = new JsonArray();
        for (int i = 0; i < rowWidgets.size(); i++) {
            DataResult<JsonElement> r = rowWidgets.get(i).currentJson();
            var error = r.error();
            if (error.isPresent()) {
                int idx = i;
                String msg = error.get().message();
                return DataResult.error(() -> "List[" + idx + "]: " + msg);
            }
            r.result().ifPresent(array::add);
        }
        return DataResult.success(array);
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        // clear existing rows
        rowsHost.removeAll();
        rowPanels.clear();
        rowWidgets.clear();
        if (value != null && value.isJsonArray()) {
            for (JsonElement el : value.getAsJsonArray()) {
                addRow(el);
            }
        }
        root.revalidate();
        root.repaint();
    }
}
