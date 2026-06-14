package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

public final class MapOfWidget implements SwingWidget {

    private final Schema<?> keySchema;
    private final Schema<?> valueSchema;
    private final JPanel root = new JPanel();
    private final JPanel rowsHost = new JPanel();
    private final List<SwingWidget> keyWidgets = new ArrayList<>();
    private final List<SwingWidget> valueWidgets = new ArrayList<>();
    private final List<JPanel> rowPanels = new ArrayList<>();

    public MapOfWidget(Schema.MapOf<?, ?> schema) {
        this.keySchema = schema.key();
        this.valueSchema = schema.value();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        // Allow the map widget to fill the parent's available horizontal width.
        root.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        rowsHost.setLayout(new BoxLayout(rowsHost, BoxLayout.Y_AXIS));
        rowsHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowsHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        root.add(rowsHost);

        root.add(Box.createVerticalStrut(UiScale.small()));

        JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton add = new JButton("+ Add");
        add.putClientProperty("JButton.buttonType", "roundRect");
        add.addActionListener(e -> {
            addRow(null, null);
            root.revalidate();
            root.repaint();
        });
        addBar.add(add);
        root.add(addBar);
    }

    private void addRow(@Nullable JsonElement initialKey, @Nullable JsonElement initialValue) {
        SwingWidget keyWidget = SwingWidgetFactory.create(keySchema);
        SwingWidget valueWidget = SwingWidgetFactory.create(valueSchema);
        if (initialKey != null) keyWidget.setJson(initialKey);
        if (initialValue != null) valueWidget.setJson(initialValue);
        keyWidgets.add(keyWidget);
        valueWidgets.add(valueWidget);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(keyWidget.component());
        row.add(Box.createHorizontalStrut(UiScale.small()));
        row.add(valueWidget.component());
        row.add(Box.createHorizontalStrut(UiScale.small()));

        JButton remove = new JButton("×");
        remove.setToolTipText("Remove");
        remove.putClientProperty("JButton.buttonType", "borderless");
        remove.setMargin(UiScale.insets(0, 4, 0, 4));
        remove.addActionListener(e -> {
            int idx = rowPanels.indexOf(row);
            if (idx >= 0) {
                rowsHost.remove(row);
                rowPanels.remove(idx);
                keyWidgets.remove(idx);
                valueWidgets.remove(idx);
                root.revalidate();
                root.repaint();
            }
        });
        row.add(remove);

        Dimension pref = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        rowPanels.add(row);
        rowsHost.add(row);
        rowsHost.add(Box.createVerticalStrut(UiScale.med()));
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        JsonObject out = new JsonObject();
        for (int i = 0; i < keyWidgets.size(); i++) {
            DataResult<JsonElement> keyResult = keyWidgets.get(i).currentJson();
            var keyError = keyResult.error();
            if (keyError.isPresent()) {
                int idx = i;
                String msg = keyError.get().message();
                return DataResult.error(() -> "Map key[" + idx + "]: " + msg);
            }
            JsonElement keyJson = keyResult.result().orElse(null);
            if (keyJson == null) {
                int idx = i;
                return DataResult.error(() -> "Map key[" + idx + "] missing");
            }
            String keyStr;
            if (keyJson.isJsonPrimitive()) {
                keyStr = keyJson.getAsJsonPrimitive().getAsString();
            } else {
                int idx = i;
                return DataResult.error(() -> "Map key[" + idx + "] is not stringifiable");
            }

            DataResult<JsonElement> valueResult = valueWidgets.get(i).currentJson();
            var valueError = valueResult.error();
            if (valueError.isPresent()) {
                int idx = i;
                String msg = valueError.get().message();
                return DataResult.error(() -> "Map value[" + idx + "]: " + msg);
            }
            JsonElement valueJson = valueResult.result().orElse(null);
            if (valueJson != null) {
                out.add(keyStr, valueJson);
            }
        }
        return DataResult.success(out);
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        rowsHost.removeAll();
        rowPanels.clear();
        keyWidgets.clear();
        valueWidgets.clear();
        if (value != null && value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                addRow(new JsonPrimitive(entry.getKey()), entry.getValue());
            }
        }
        root.revalidate();
        root.repaint();
    }
}
