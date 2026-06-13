package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OneOfWidget implements SwingWidget {

    private final String typeField;
    private final Map<String, Schema<?>> variants;
    private final List<String> variantKeys = new ArrayList<>();
    private final JComboBox<String> combo;
    private final JPanel root = new JPanel();
    private final JPanel subHost = new JPanel(new BorderLayout());
    private @Nullable SwingWidget currentSub;
    private @Nullable String currentKey;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public OneOfWidget(Schema.OneOf<?> schema) {
        this.typeField = schema.typeField();
        this.variants = (Map<String, Schema<?>>) (Map) schema.variants();
        variantKeys.addAll(variants.keySet());

        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, UiScale.px(6), 0));
        top.add(new JLabel(typeField + ":"));
        combo = new JComboBox<>(variantKeys.toArray(new String[0]));
        top.add(combo);
        root.add(top);
        root.add(javax.swing.Box.createVerticalStrut(UiScale.px(4)));
        root.add(subHost);

        combo.addActionListener(e -> {
            String sel = (String) combo.getSelectedItem();
            swapSub(sel);
        });

        if (!variantKeys.isEmpty()) {
            swapSub(variantKeys.get(0));
        }
    }

    private void swapSub(@Nullable String key) {
        subHost.removeAll();
        currentKey = key;
        if (key != null) {
            Schema<?> sub = variants.get(key);
            if (sub != null) {
                currentSub = SwingWidgetFactory.create(sub);
                subHost.add(currentSub.component(), BorderLayout.CENTER);
            } else {
                currentSub = null;
            }
        } else {
            currentSub = null;
        }
        subHost.revalidate();
        subHost.repaint();
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        if (currentKey == null) {
            return DataResult.error(() -> "No variant selected for OneOf");
        }
        JsonObject out = new JsonObject();
        out.add(typeField, new JsonPrimitive(currentKey));
        if (currentSub != null) {
            DataResult<JsonElement> sub = currentSub.currentJson();
            var error = sub.error();
            if (error.isPresent()) {
                String msg = error.get().message();
                return DataResult.error(() -> "Variant '" + currentKey + "': " + msg);
            }
            JsonElement subJson = sub.result().orElse(null);
            if (subJson != null) {
                if (subJson.isJsonObject()) {
                    for (var entry : subJson.getAsJsonObject().entrySet()) {
                        out.add(entry.getKey(), entry.getValue());
                    }
                } else {
                    out.add("value", subJson);
                }
            }
        }
        return DataResult.success(out);
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value == null || !value.isJsonObject()) return;
        JsonObject obj = value.getAsJsonObject();
        JsonElement tag = obj.get(typeField);
        if (tag == null || !tag.isJsonPrimitive() || !tag.getAsJsonPrimitive().isString()) return;
        String key = tag.getAsString();
        if (!variants.containsKey(key)) return;
        combo.setSelectedItem(key);
        // swapSub is triggered by the action listener.
        if (currentSub != null) {
            currentSub.setJson(obj);
        }
    }
}
