package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

public final class EnumWidget implements SwingWidget {

    private final JComboBox<String> combo;

    public EnumWidget(Schema.Enum<?> schema) {
        List<String> labels = new ArrayList<>(schema.options().size());
        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.function.Function labelFn = schema.label();
        for (Object opt : schema.options()) {
            @SuppressWarnings("unchecked")
            String s = (String) labelFn.apply(opt);
            labels.add(s);
        }
        this.combo = new JComboBox<>(labels.toArray(new String[0]));
        // Allow horizontal stretch in BoxLayout/GridBag parents.
        int h = combo.getPreferredSize().height;
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        combo.setMinimumSize(new Dimension(0, h));
    }

    @Override
    public JComponent component() {
        return combo;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        Object sel = combo.getSelectedItem();
        if (sel == null) return DataResult.error(() -> "No enum value selected");
        return DataResult.success(new JsonPrimitive(sel.toString()));
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String s = value.getAsString();
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (s.equals(combo.getItemAt(i))) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
        }
    }
}
