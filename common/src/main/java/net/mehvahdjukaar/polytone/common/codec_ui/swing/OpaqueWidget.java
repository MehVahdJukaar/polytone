package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public final class OpaqueWidget implements SwingWidget {

    private static final Color ERROR_COLOR = new Color(0xC0392B);

    private final JTextArea textArea = new JTextArea(6, 40);
    private final JScrollPane scroll;
    private final JLabel errorLabel = new JLabel(" ");
    private final JPanel root = new JPanel();
    private final Border defaultBorder;
    private final Border errorBorder;

    public OpaqueWidget() {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        // Use the L&F-default font SIZE (FlatLaf already scales it) but force the
        // monospaced family. Multiplying by UiScale.px(...) here would double-scale.
        Font base = UIManager.getFont("TextArea.font");
        float size = base != null ? base.getSize2D() : 13f;
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size)));
        textArea.setText("null");

        scroll = new JScrollPane(textArea);
        // Min visible height ~120 logical px; let it grow with content.
        scroll.setPreferredSize(UiScale.dim(420, 140));
        scroll.setMinimumSize(UiScale.dim(200, 120));

        defaultBorder = scroll.getBorder();
        errorBorder = BorderFactory.createLineBorder(ERROR_COLOR, 1);

        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setFont(UiScale.deriveFont(errorLabel.getFont(), Font.PLAIN, -1f));
        errorLabel.setVisible(false);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(UiScale.px(2), 0, 0, 0));

        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(scroll);
        root.add(errorLabel);

        // Live syntax validation
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validate(); }
            @Override public void removeUpdate(DocumentEvent e) { validate(); }
            @Override public void changedUpdate(DocumentEvent e) { validate(); }
        });
    }

    private void validate() {
        String text = textArea.getText();
        if (text.isBlank()) {
            showError(null);
            return;
        }
        try {
            JsonParser.parseString(text);
            showError(null);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(@Nullable String msg) {
        if (msg == null) {
            scroll.setBorder(defaultBorder);
            errorLabel.setText(" ");
            errorLabel.setVisible(false);
        } else {
            scroll.setBorder(errorBorder);
            errorLabel.setText("Invalid JSON: " + msg);
            errorLabel.setVisible(true);
        }
        root.revalidate();
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        String text = textArea.getText();
        if (text.isBlank()) {
            return DataResult.error(() -> "Empty JSON");
        }
        try {
            return DataResult.success(JsonParser.parseString(text));
        } catch (Exception e) {
            String msg = e.getMessage();
            return DataResult.error(() -> "Invalid JSON: " + msg);
        }
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value == null) {
            textArea.setText("null");
        } else {
            textArea.setText(value.toString());
        }
    }
}
