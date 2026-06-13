package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public final class StringWidget implements SwingWidget {

    private final JTextField field = new JTextField(20);

    public StringWidget(int maxLen) {
        if (maxLen > 0 && maxLen < Integer.MAX_VALUE) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new MaxLenFilter(maxLen));
        }
    }

    @Override
    public JComponent component() {
        return field;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        return DataResult.success(new JsonPrimitive(field.getText()));
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            field.setText(value.getAsString());
        } else {
            field.setText("");
        }
    }

    private static final class MaxLenFilter extends DocumentFilter {
        private final int maxLen;
        MaxLenFilter(int maxLen) { this.maxLen = maxLen; }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            if (text == null) return;
            int curLen = fb.getDocument().getLength();
            int allow = Math.max(0, maxLen - curLen);
            if (allow <= 0) return;
            super.insertString(fb, offset, text.length() > allow ? text.substring(0, allow) : text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) text = "";
            int curLen = fb.getDocument().getLength();
            int allow = Math.max(0, maxLen - (curLen - length));
            if (allow <= 0) {
                super.replace(fb, offset, length, "", attrs);
                return;
            }
            super.replace(fb, offset, length, text.length() > allow ? text.substring(0, allow) : text, attrs);
        }
    }
}
