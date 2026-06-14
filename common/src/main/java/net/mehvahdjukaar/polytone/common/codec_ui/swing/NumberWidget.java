package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;

/**
 * Single Swing widget for any numeric primitive schema variant.
 * The factory configures the model with appropriate min/max/step/initial typed values.
 */
public final class NumberWidget implements SwingWidget {

    public enum Kind { INT, LONG, FLOAT, DOUBLE }

    private final JSpinner spinner;
    private final Kind kind;

    public NumberWidget(Kind kind, Number min, Number max, Number step, Number initial) {
        this.kind = kind;
        // SpinnerNumberModel signed-Comparable ctor expects Comparable bounds. Pass raw numbers.
        this.spinner = new JSpinner(new SpinnerNumberModel(initial, (Comparable<?>) min, (Comparable<?>) max, step));
        // Allow horizontal stretch in BoxLayout/GridBag parents.
        int h = spinner.getPreferredSize().height;
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        spinner.setMinimumSize(new Dimension(0, h));
    }

    @Override
    public JComponent component() {
        return spinner;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        Object raw = spinner.getValue();
        if (!(raw instanceof Number n)) {
            return DataResult.error(() -> "Spinner value is not a number");
        }
        return switch (kind) {
            case INT -> DataResult.success(new JsonPrimitive(n.intValue()));
            case LONG -> DataResult.success(new JsonPrimitive(n.longValue()));
            case FLOAT -> DataResult.success(new JsonPrimitive(n.floatValue()));
            case DOUBLE -> DataResult.success(new JsonPrimitive(n.doubleValue()));
        };
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        Number n;
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            n = value.getAsNumber();
        } else {
            n = 0;
        }
        try {
            switch (kind) {
                case INT -> spinner.setValue(n.intValue());
                case LONG -> spinner.setValue(n.longValue());
                case FLOAT -> spinner.setValue(n.floatValue());
                case DOUBLE -> spinner.setValue(n.doubleValue());
            }
        } catch (IllegalArgumentException ignored) {
            // value out of bounds; leave as-is
        }
    }
}
