package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public final class RecordWidget implements SwingWidget {

    private record FieldEntry(Schema.Field<?, ?> field, SwingWidget widget) {}

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final List<FieldEntry> entries = new ArrayList<>();

    public RecordWidget(Schema.Record<?> schema) {
        int row = 0;
        for (Schema.Field<?, ?> field : schema.fields()) {
            SwingWidget child = SwingWidgetFactory.create(field.schema());
            entries.add(new FieldEntry(field, child));

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = row;
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = new Insets(2, 4, 2, 8);
            String labelText = field.optional() ? field.name() + " (optional)" : field.name();
            panel.add(new JLabel(labelText), gc);

            gc = new GridBagConstraints();
            gc.gridx = 1;
            gc.gridy = row;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = new Insets(2, 0, 2, 4);
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
