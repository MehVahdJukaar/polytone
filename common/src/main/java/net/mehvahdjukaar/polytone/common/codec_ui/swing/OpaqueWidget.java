package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.formdev.flatlaf.FlatLaf;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Free-form JSON editor used as fallback when no schema is available for a value.
 *
 * <p>Uses {@code RSyntaxTextArea} for JSON syntax highlighting and folding when available.
 * Falls back to a plain {@link JTextArea} (with a {@code WARN} log) if RSyntaxTextArea is
 * not on the classpath. The fallback is deliberately loud because losing highlighting
 * means a production-jar bundling regression we want flagged.</p>
 */
public final class OpaqueWidget implements SwingWidget {

    private static final Color ERROR_COLOR = new Color(0xC0392B);

    private final JComponent textArea;     // either RSyntaxTextArea or plain JTextArea
    private final JScrollPane scroll;
    private final JLabel errorLabel = new JLabel(" ");
    private final JPanel root = new JPanel();
    private final Border defaultBorder;
    private final Border errorBorder;

    // Strategy methods bound at construction so the rest of the widget doesn't care
    // which backing implementation we have.
    private final java.util.function.Supplier<String> textGetter;
    private final java.util.function.Consumer<String> textSetter;

    public OpaqueWidget() {
        // Try the RSyntaxTextArea route first. Loud-fallback to JTextArea on any failure
        // (ClassNotFoundError, NoClassDefFoundError, init issues) so production jar
        // bundling regressions surface clearly in logs.
        JComponent area;
        JScrollPane sp;
        java.util.function.Supplier<String> getter;
        java.util.function.Consumer<String> setter;
        DocListenerAttach docAttach;
        try {
            org.fife.ui.rsyntaxtextarea.RSyntaxTextArea rsta =
                    new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea(6, 40);
            rsta.setSyntaxEditingStyle(
                    org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JSON);
            rsta.setCodeFoldingEnabled(true);
            rsta.setAntiAliasingEnabled(true);
            rsta.setLineWrap(true);
            rsta.setWrapStyleWord(false);

            // Apply dark/default theme inline so the RSTA type stays out of method
            // signatures (keeps classloading lazy when the dep is missing).
            String themeName = FlatLaf.isLafDark() ? "dark" : "default";
            String resourcePath = "/org/fife/ui/rsyntaxtextarea/themes/" + themeName + ".xml";
            try (var is = OpaqueWidget.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    org.fife.ui.rsyntaxtextarea.Theme.load(is).apply(rsta);
                } else {
                    Polytone.LOGGER.warn("[codec_ui] RSyntaxTextArea theme resource not found: {}", resourcePath);
                }
            } catch (Throwable themeError) {
                Polytone.LOGGER.warn("[codec_ui] Could not apply RSyntaxTextArea theme {}", themeName, themeError);
            }

            // Match the L&F base font SIZE so highlighting text sits at the same scale
            // as the rest of the editor. Family stays monospaced.
            Font base = UIManager.getFont("TextArea.font");
            float size = base != null ? base.getSize2D() : 13f;
            rsta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size)));

            sp = new org.fife.ui.rtextarea.RTextScrollPane(rsta);
            area = rsta;
            getter = rsta::getText;
            setter = rsta::setText;
            docAttach = (listener) -> rsta.getDocument().addDocumentListener(listener);
        } catch (Throwable t) {
            Polytone.LOGGER.warn(
                "[codec_ui] RSyntaxTextArea unavailable — falling back to plain JTextArea " +
                "for raw-JSON editor. JSON highlighting will be missing. " +
                "Check that com.fifesoft:rsyntaxtextarea is bundled in the production jar.",
                t);
            JTextArea plain = new JTextArea(6, 40);
            plain.setLineWrap(true);
            plain.setWrapStyleWord(false);
            Font base = UIManager.getFont("TextArea.font");
            float size = base != null ? base.getSize2D() : 13f;
            plain.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size)));
            plain.setText("");
            sp = new JScrollPane(plain);
            area = plain;
            getter = plain::getText;
            setter = plain::setText;
            docAttach = (listener) -> plain.getDocument().addDocumentListener(listener);
        }
        this.textArea = area;
        this.scroll = sp;
        this.textGetter = getter;
        this.textSetter = setter;

        // Height pinned; minimum width 0 so the outer form is never forced wider
        // than its column. Max width unbounded so BoxLayout stretches to fill.
        scroll.setPreferredSize(UiScale.dim(360, 140));
        scroll.setMinimumSize(new Dimension(0, UiScale.px(120)));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiScale.px(220)));
        // The inner text area wraps lines — disable its own horizontal scrollbar.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        defaultBorder = scroll.getBorder();
        errorBorder = BorderFactory.createLineBorder(ERROR_COLOR, 1);

        JLabel hint = new JLabel("Raw JSON — no schema available for this field");
        hint.setFont(UiScale.deriveFont(hint.getFont(), Font.ITALIC, -1f));
        // L&F-derived muted color so it reads on both light AND dark themes.
        Color muted = UIManager.getColor("Label.disabledForeground");
        hint.setForeground(muted != null ? muted : new Color(0x999999));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, UiScale.small(), 0));

        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setFont(UiScale.deriveFont(errorLabel.getFont(), Font.PLAIN, -1f));
        errorLabel.setVisible(false);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(UiScale.small(), 0, 0, 0));

        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Stretch root to fill the available form width.
        root.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        root.add(hint);
        root.add(scroll);
        root.add(errorLabel);

        // Live syntax validation, on whichever Document we bound.
        docAttach.attach(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validate(); }
            @Override public void removeUpdate(DocumentEvent e) { validate(); }
            @Override public void changedUpdate(DocumentEvent e) { validate(); }
        });
    }

    private void validate() {
        String text = textGetter.get();
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
        String text = textGetter.get();
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
            textSetter.accept("");
        } else {
            textSetter.accept(value.toString());
        }
    }

    @FunctionalInterface
    private interface DocListenerAttach {
        void attach(DocumentListener l);
    }
}
