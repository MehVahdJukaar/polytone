package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Hex color picker widget. Layout:
 *   [ swatch ] [ #RRGGBB / #AARRGGBB text field ] [ ... button (JColorChooser) ]
 *
 * <p>JSON shape: a single {@code JsonPrimitive} integer holding the packed color:
 * RGB (low 24 bits) when {@code !hasAlpha}, ARGB (full 32 bits) when {@code hasAlpha}.
 * Reads tolerate any integer; reads with a different / wider format are masked.</p>
 */
public final class ColorWidget implements SwingWidget {

    private final boolean hasAlpha;
    private final JPanel root;
    private final Swatch swatch = new Swatch();
    private final JTextField hexField = new JTextField(8);
    private final JButton pickerButton = new JButton("...");

    /** Current packed color value. ARGB layout regardless of hasAlpha (alpha is just ignored on output if !hasAlpha). */
    private int value;
    /** Guard so programmatic edits to the text field don't recurse into the document listener. */
    private boolean updatingText = false;

    public ColorWidget(Schema.Color schema) {
        this.hasAlpha = schema.hasAlpha();
        // Initialize to opaque black so RGB and ARGB defaults are both sensible.
        this.value = 0xFF000000;

        this.root = new JPanel();
        this.root.setLayout(new BoxLayout(this.root, BoxLayout.X_AXIS));
        this.root.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.root.setOpaque(false);

        this.swatch.setPreferredSize(UiScale.dim(28, 28));
        this.swatch.setMinimumSize(UiScale.dim(28, 28));
        this.swatch.setMaximumSize(UiScale.dim(28, 28));
        this.swatch.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        this.hexField.setFont(UiScale.uiFont("TextField.font"));
        // Let the text field absorb the row's leftover horizontal space — the swatch
        // and picker button are fixed-size, the hex field stretches between them so
        // the row fills the form column flush with siblings.
        Dimension prefField = this.hexField.getPreferredSize();
        this.hexField.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefField.height));
        this.hexField.setMinimumSize(new Dimension(0, prefField.height));

        // Root row should also stretch horizontally so the parent form layout grants
        // it the full column width.
        this.root.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefField.height));

        this.pickerButton.setFont(UiScale.uiFont("Button.font"));
        this.pickerButton.setMargin(UiScale.insets(0, 6, 0, 6));

        this.root.add(this.swatch);
        this.root.add(Box.createHorizontalStrut(UiScale.small()));
        this.root.add(this.hexField);
        this.root.add(Box.createHorizontalStrut(UiScale.small()));
        this.root.add(this.pickerButton);

        // Wire interactions.
        this.hexField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTextChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onTextChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onTextChanged(); }
        });
        this.pickerButton.addActionListener(e -> openPicker());

        // Push initial value into the text field + swatch.
        applyValueToUi();
    }

    private void onTextChanged() {
        if (updatingText) return;
        String text = hexField.getText().trim();
        Integer parsed = parseHex(text);
        if (parsed != null) {
            value = hasAlpha ? parsed : (parsed & 0xFFFFFF) | 0xFF000000;
            swatch.repaint();
        }
        // If unparsable: keep prior value; user is mid-edit. Don't fight them.
    }

    private void openPicker() {
        Color initial = new Color(value, hasAlpha);
        Color picked = JColorChooser.showDialog(root, "Pick color", initial, hasAlpha);
        if (picked != null) {
            value = picked.getRGB(); // ARGB int
            if (!hasAlpha) value = (value & 0xFFFFFF) | 0xFF000000;
            applyValueToUi();
        }
    }

    /** Refresh swatch + text field from {@link #value}. Avoids re-triggering the doc listener. */
    private void applyValueToUi() {
        updatingText = true;
        try {
            hexField.setText(formatHex(value, hasAlpha));
        } finally {
            updatingText = false;
        }
        swatch.repaint();
    }

    private static @Nullable Integer parseHex(String s) {
        if (s == null || s.isEmpty()) return null;
        if (s.startsWith("#")) s = s.substring(1);
        else if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.isEmpty() || s.length() > 8) return null;
        try {
            return (int) Long.parseUnsignedLong(s, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatHex(int v, boolean hasAlpha) {
        return hasAlpha
                ? String.format("#%08X", v)
                : String.format("#%06X", v & 0xFFFFFF);
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        int out = hasAlpha ? value : (value & 0xFFFFFF);
        return DataResult.success(new JsonPrimitive(out));
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        int v;
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            v = value.getAsInt();
        } else {
            v = hasAlpha ? 0xFF000000 : 0;
        }
        this.value = hasAlpha ? v : (v & 0xFFFFFF) | 0xFF000000;
        applyValueToUi();
    }

    /** Filled rectangle whose color tracks {@link ColorWidget#value}. */
    private final class Swatch extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Color c = new Color(value, hasAlpha);
            g.setColor(c);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
