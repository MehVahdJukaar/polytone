package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

public final class RecordWidget implements SwingWidget {

    private record FieldEntry(Schema.Field<?, ?> field, SwingWidget widget) {}

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final List<FieldEntry> entries = new ArrayList<>();

    public RecordWidget(Schema.Record<?> schema) {
        // Outer padding inside the record so it doesn't crash into the scroll edge.
        panel.setBorder(BorderFactory.createEmptyBorder(
                UiScale.small(), UiScale.small(), UiScale.small(), UiScale.small()));
        // Allow horizontal stretch when nested inside another record / list / map row.
        // BoxLayout in particular will only stretch a child up to its maximumSize, so
        // without this nested records stay at their preferred (narrow) width.
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        Color mutedColor = UIManager.getColor("Label.disabledForeground");
        if (mutedColor == null) mutedColor = new Color(0x999999);

        int row = 0;
        for (Schema.Field<?, ?> field : schema.fields()) {
            SwingWidget child = SwingWidgetFactory.create(field.schema());
            entries.add(new FieldEntry(field, child));

            // Right-aligned label column.
            JLabel name = new JLabel(field.name());
            name.setFont(name.getFont().deriveFont(Font.PLAIN));

            JPanel labelCell = new JPanel(new GridBagLayout());
            GridBagConstraints lc = new GridBagConstraints();
            lc.gridx = 0;
            lc.anchor = GridBagConstraints.LINE_END;
            labelCell.add(name, lc);
            if (field.optional()) {
                JLabel opt = new JLabel("opt");
                opt.setFont(UiScale.deriveFont(opt.getFont(), Font.ITALIC, -2f));
                opt.setForeground(mutedColor);
                opt.setBorder(BorderFactory.createEmptyBorder(0, UiScale.small(), 0, 0));
                lc.gridx = 1;
                labelCell.add(opt, lc);
            }

            // MED vertical between rows, SMALL horizontal gap label↔widget.
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = row;
            gc.anchor = GridBagConstraints.LINE_END;
            gc.insets = new java.awt.Insets(UiScale.small(), UiScale.small(), UiScale.small(), UiScale.med());
            panel.add(labelCell, gc);

            gc = new GridBagConstraints();
            gc.gridx = 1;
            gc.gridy = row;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.LINE_START;
            gc.insets = new java.awt.Insets(UiScale.small(), 0, UiScale.small(), UiScale.small());
            panel.add(child.component(), gc);

            row++;
        }
        // bottom filler so fields stick to the top
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.VERTICAL;
        panel.add(new JLabel(""), filler);
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        JsonObject obj = new JsonObject();
        for (FieldEntry e : entries) {
            DataResult<JsonElement> r = e.widget.currentJson();
            var error = r.error();
            if (error.isPresent()) {
                if (e.field.optional()) {
                    // Optional field with invalid child: omit so codec default applies
                    continue;
                }
                String name = e.field.name();
                String msg = error.get().message();
                return DataResult.error(() -> "Field '" + name + "': " + msg);
            }
            r.result().ifPresent(json -> obj.add(e.field.name(), json));
        }
        return DataResult.success(obj);
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            for (FieldEntry e : entries) {
                e.widget.setJson(null);
            }
            return;
        }
        JsonObject obj = value.getAsJsonObject();
        for (FieldEntry e : entries) {
            JsonElement sub = obj.get(e.field.name());
            e.widget.setJson(sub);
        }
    }
}
