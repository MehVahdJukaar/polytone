package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Frame;

public final class ResourceIdWidget implements SwingWidget {

    private final @Nullable ResourceKey<? extends Registry<?>> registryKey;
    private final JButton button = new JButton("<none>");
    private @Nullable Identifier current;

    public ResourceIdWidget(Schema.ResourceId schema) {
        this.registryKey = schema.registry();
        button.addActionListener(e -> openPicker());
    }

    private void openPicker() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(button);
        if (registryKey == null) {
            // Plain text fallback: simple input dialog
            String input = javax.swing.JOptionPane.showInputDialog(
                    button, "Identifier:", current == null ? "" : current.toString());
            if (input != null && !input.isBlank()) {
                try {
                    current = Identifier.parse(input.trim());
                    button.setText(current.toString());
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(button,
                            "Invalid identifier: " + ex.getMessage(),
                            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
            return;
        }
        RegistryPickerDialog dlg = new RegistryPickerDialog(owner, registryKey, current, picked -> {
            current = picked;
            button.setText(picked == null ? "<none>" : picked.toString());
        });
        dlg.setVisible(true);
    }

    @Override
    public JComponent component() {
        return button;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        if (current == null) return DataResult.success(JsonNull.INSTANCE);
        return DataResult.success(new JsonPrimitive(current.toString()));
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            try {
                current = Identifier.parse(value.getAsString());
                button.setText(current.toString());
                return;
            } catch (Exception ignored) {}
        }
        current = null;
        button.setText("<none>");
    }
}
