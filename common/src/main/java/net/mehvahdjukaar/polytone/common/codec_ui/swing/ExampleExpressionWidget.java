package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Demonstration of the custom-widget binding pattern. Renders a single-line text field
 * with a "expression" placeholder hint and a monospace font, suitable for editing
 * expression-language strings. Round-trips as a JSON string.
 *
 * <p>Bind to a codec like:
 * <pre>{@code
 * public static final SchemaCodec<String> EXPRESSION =
 *     SchemaCodecs.withWidget(Codec.STRING, ExampleExpressionWidget.DEF);
 * }</pre>
 *
 * <p>The {@link #DEF} constant is the entry point — reference it from codec declarations
 * to attach this widget to any {@code Codec<String>}.</p>
 */
public final class ExampleExpressionWidget implements SwingWidget {

    /** The named, reusable factory. This is the recommended way to reference the widget. */
    public static final SwingWidgetDef<String> DEF = schema -> new ExampleExpressionWidget();

    private final JTextField field = new JTextField();
    private final JPanel root = new JPanel();

    private ExampleExpressionWidget() {
        // Monospace font matching the L&F base size — readable for expression-language text.
        Font base = UIManager.getFont("TextField.font");
        float size = base != null ? base.getSize2D() : 13f;
        field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size)));

        // FlatLaf placeholder text — visible when the field is empty, dims away on focus.
        field.putClientProperty("JTextField.placeholderText", "expression");

        // Fill available form-row width but never push the form wider.
        int h = field.getPreferredSize().height;
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        field.setMinimumSize(new Dimension(0, h));

        root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
        root.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.setOpaque(false);
        root.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        root.setMinimumSize(new Dimension(0, h));
        root.add(field);
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        return DataResult.success(new JsonPrimitive(field.getText()));
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        field.setText(value != null && value.isJsonPrimitive() ? value.getAsString() : "");
    }
}
