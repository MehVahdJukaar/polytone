package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;

public final class OpaqueWidget implements SwingWidget {

    private final JTextArea textArea = new JTextArea(6, 40);
    private final JScrollPane scroll;

    public OpaqueWidget() {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        textArea.setText("null");
        scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(400, 120));
    }

    @Override
    public JComponent component() {
        return scroll;
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
