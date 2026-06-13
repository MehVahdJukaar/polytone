package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

public final class BoolWidget implements SwingWidget {

    private final JCheckBox box = new JCheckBox();

    @Override
    public JComponent component() {
        return box;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        return DataResult.success(new JsonPrimitive(box.isSelected()));
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            box.setSelected(value.getAsBoolean());
        } else {
            box.setSelected(false);
        }
    }
}
