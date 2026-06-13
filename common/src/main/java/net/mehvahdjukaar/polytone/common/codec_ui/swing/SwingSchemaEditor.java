package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaEditor;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.function.Consumer;

public final class SwingSchemaEditor implements SchemaEditor {

    @Override
    public <A> void open(SchemaCodec<A> codec, @Nullable A initial, Consumer<A> onSave) {
        setupSwingDefaults();
        forceNonHeadless();
        SwingUtilities.invokeLater(() -> openOnEdt(codec, initial, onSave));
    }

    private static volatile boolean swingSetupDone = false;
    private static synchronized void setupSwingDefaults() {
        if (swingSetupDone) return;
        swingSetupDone = true;
        // OpenGL pipeline: significantly faster than the default software renderer at 4K.
        System.setProperty("sun.java2d.opengl", "true");
        // Smooth text.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    /**
     * Install the L&amp;F. Must run on the EDT before any JFrame/JDialog is
     * constructed. Public so {@link net.mehvahdjukaar.polytone.common.codec_ui.example.ExamplesLauncher}
     * can share the exact same bootstrap sequence.
     *
     * <p><b>Strict ordering — do NOT reorder these phases:</b>
     * <ol>
     *   <li>{@code flatlaf.uiScale} system property is set BEFORE
     *       {@code FlatLightLaf.setup()} so FlatLaf reads it during init.</li>
     *   <li>{@code FlatLightLaf.setup()} installs the L&amp;F. Hard requirement —
     *       no fallback. If FlatLaf is missing from the classpath this throws
     *       {@code NoClassDefFoundError} and that's the point: silent fallback
     *       to Metal/Nimbus is what made earlier attempts unreadable.</li>
     *   <li>UIManager polish + font + minimum-component-size keys are applied
     *       AFTER {@code setup()} so they override defaults rather than getting
     *       wiped out by L&amp;F install.</li>
     *   <li>Diagnostic logging dumps everything we know — the user has burned
     *       three phases without visibility. They need numbers to share.</li>
     * </ol></p>
     *
     * <p>Callers must invoke {@link #forceNonHeadless()} BEFORE this method
     * because {@code FlatLightLaf.setup()} touches AWT.</p>
     */
    public static void bootstrapLF() {
        // Phase 1: seed the scale BEFORE FlatLaf reads it.
        if (System.getProperty("flatlaf.uiScale") == null) {
            System.setProperty("flatlaf.uiScale", UiScale.detectInitialScale());
        }

        // Phase 2: install FlatLaf. Hard requirement — let it throw if missing.
        FlatLightLaf.setup();

        // Phase 3: bigger fonts + minimum component sizes + visual polish.
        // FlatLaf will further scale these by flatlaf.uiScale, so 15pt at 2x
        // renders as ~30pt = comfortably readable on a 4K monitor.
        UIManager.put("defaultFont", new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 15));

        // Minimum component heights — logical px, FlatLaf scales them.
        UIManager.put("Button.minimumHeight", 36);
        UIManager.put("TextComponent.minimumHeight", 32);
        UIManager.put("Spinner.minimumHeight", 32);
        UIManager.put("ComboBox.minimumHeight", 32);
        UIManager.put("Button.minimumWidth", 96);

        // Rounded corners + scroll bar polish.
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);

        // Focus ring polish.
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);

        // Phase 4: diagnostic logging — gives the user concrete numbers if
        // the editor STILL renders too small after this fix.
        logBootstrapDiagnostics();
    }

    private static void logBootstrapDiagnostics() {
        Polytone.LOGGER.info("[codec_ui] FlatLaf class: {}", FlatLightLaf.class.getName());
        Polytone.LOGGER.info("[codec_ui] flatlaf.uiScale (system prop): {}", System.getProperty("flatlaf.uiScale"));
        Polytone.LOGGER.info("[codec_ui] FlatLaf UIScale.getUserScaleFactor(): {}",
                com.formdev.flatlaf.util.UIScale.getUserScaleFactor());
        Polytone.LOGGER.info("[codec_ui] Toolkit.getScreenResolution(): {}",
                Toolkit.getDefaultToolkit().getScreenResolution());
        try {
            var mode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDisplayMode();
            Polytone.LOGGER.info("[codec_ui] Display mode: {}x{} @ {}Hz",
                    mode.getWidth(), mode.getHeight(), mode.getRefreshRate());
        } catch (Throwable t) {
            Polytone.LOGGER.warn("[codec_ui] Could not read DisplayMode", t);
        }
        Polytone.LOGGER.info("[codec_ui] Detected initial scale: {}", UiScale.detectInitialScale());
        Polytone.LOGGER.info("[codec_ui] Default font: {}", UIManager.getFont("defaultFont"));
        Polytone.LOGGER.info("[codec_ui] GDK_SCALE env: {}, GDK_DPI_SCALE env: {}",
                System.getenv("GDK_SCALE"), System.getenv("GDK_DPI_SCALE"));
    }

    // NeoForge launches with java.awt.headless=true and GraphicsEnvironment caches the flag.
    // setAccessible on a java.desktop private field is blocked since JDK 16, so go through
    // sun.misc.Unsafe (jdk.unsupported is opened for reflection) to overwrite the cache directly.
    private static void forceNonHeadless() {
        System.setProperty("java.awt.headless", "false");
        if (!GraphicsEnvironment.isHeadless()) return;
        try {
            Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Class<?> unsafeClass = unsafe.getClass();
            Field headlessField = GraphicsEnvironment.class.getDeclaredField("headless");
            Object base = unsafeClass.getMethod("staticFieldBase", Field.class).invoke(unsafe, headlessField);
            long offset = (long) unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, headlessField);
            unsafeClass.getMethod("putObject", Object.class, long.class, Object.class)
                    .invoke(unsafe, base, offset, Boolean.FALSE);
        } catch (Throwable t) {
            Polytone.LOGGER.warn("Could not disable AWT headless mode. Add JVM arg -Djava.awt.headless=false "
                    + "to your run config if the editor fails to open.", t);
        }
    }

    private <A> void openOnEdt(SchemaCodec<A> codec, @Nullable A initial, Consumer<A> onSave) {
        bootstrapLF();
        SwingWidget rootWidget = SwingWidgetFactory.create(codec.schema());

        if (initial != null) {
            DataResult<JsonElement> encoded = codec.codec().encodeStart(JsonOps.INSTANCE, initial);
            encoded.result().ifPresent(rootWidget::setJson);
        }

        JFrame frame = new JFrame("Schema Editor");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // 16px outer padding, 12px between header / center / footer rows.
        JPanel content = new JPanel(new BorderLayout(0, UiScale.px(12)));
        content.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(16), UiScale.px(16), UiScale.px(16), UiScale.px(16)));

        // ---- Header: bold title (no manual size: rely on L&F-scaled font) ----
        JLabel title = new JLabel("Schema Editor");
        title.setFont(UiScale.deriveFont(title.getFont(), Font.BOLD, 4f));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, UiScale.px(4), 0));
        content.add(title, BorderLayout.NORTH);

        // ---- Center: scrollable widget area ----
        JScrollPane scroll = new JScrollPane(rootWidget.component());
        scroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        scroll.getVerticalScrollBar().setUnitIncrement(UiScale.px(16));
        scroll.getViewport().setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);

        // ---- Footer: error label above right-aligned action row ----
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(UIManager.getColor("Actions.Red") != null
                ? UIManager.getColor("Actions.Red")
                : new java.awt.Color(0xC0392B));

        JButton open = new JButton("Open");
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.putClientProperty("JButton.buttonType", "default");

        JPanel buttons = new JPanel(new BorderLayout());
        Box right = Box.createHorizontalBox();
        right.add(open);
        right.add(Box.createHorizontalStrut(UiScale.px(8)));
        right.add(cancel);
        right.add(Box.createHorizontalStrut(UiScale.px(8)));
        right.add(save);
        buttons.add(right, BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout(0, UiScale.px(8)));
        south.add(errorLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);

        cancel.addActionListener(e -> frame.dispose());

        open.addActionListener(e -> {
            errorLabel.setText(" ");
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open Schema JSON");
            chooser.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
            int result = chooser.showOpenDialog(frame);
            if (result != JFileChooser.APPROVE_OPTION) return;
            File target = chooser.getSelectedFile();
            if (target == null) return;
            String text;
            try {
                text = Files.readString(target.toPath());
            } catch (Exception ex) {
                errorLabel.setText("Read error: " + ex.getMessage());
                return;
            }
            JsonElement parsedJson;
            try {
                parsedJson = JsonParser.parseString(text);
            } catch (Exception ex) {
                errorLabel.setText("JSON parse error: " + ex.getMessage());
                return;
            }
            DataResult<A> decoded = codec.codec().parse(JsonOps.INSTANCE, parsedJson);
            if (decoded.error().isPresent()) {
                errorLabel.setText("Schema parse error: " + decoded.error().get().message());
                return;
            }
            A value = decoded.result().orElseThrow();
            DataResult<JsonElement> reEncoded = codec.codec().encodeStart(JsonOps.INSTANCE, value);
            if (reEncoded.error().isPresent()) {
                errorLabel.setText("Re-encode error: " + reEncoded.error().get().message());
                return;
            }
            reEncoded.result().ifPresent(rootWidget::setJson);
        });

        save.addActionListener(e -> {
            errorLabel.setText(" ");
            DataResult<JsonElement> jsonResult = rootWidget.currentJson();
            if (jsonResult.error().isPresent()) {
                errorLabel.setText("Build error: " + jsonResult.error().get().message());
                return;
            }
            JsonElement json = jsonResult.result().orElseThrow();

            DataResult<A> parsed = codec.codec().parse(JsonOps.INSTANCE, json);
            if (parsed.error().isPresent()) {
                errorLabel.setText("Parse error: " + parsed.error().get().message());
                return;
            }
            A value = parsed.result().orElseThrow();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Schema JSON");
            chooser.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
            chooser.setSelectedFile(new File("schema.json"));
            int result = chooser.showSaveDialog(frame);
            if (result != JFileChooser.APPROVE_OPTION) {
                // Stay open
                return;
            }
            File target = chooser.getSelectedFile();
            if (target == null) return;
            if (!target.getName().toLowerCase().endsWith(".json")) {
                target = new File(target.getParentFile(), target.getName() + ".json");
            }

            String pretty = new GsonBuilder().setPrettyPrinting().create().toJson(json);
            try {
                Files.writeString(target.toPath(), pretty);
            } catch (Exception ex) {
                errorLabel.setText("Write error: " + ex.getMessage());
                return;
            }

            try {
                onSave.accept(value);
            } catch (Exception ex) {
                errorLabel.setText("Callback error: " + ex.getMessage());
                return;
            }
            frame.dispose();
        });

        frame.setContentPane(content);
        frame.pack();

        // Generous logical-px baselines: 720x800 at 1x becomes 1440x1600 at 2x,
        // plenty of room on a 4K display. Min width 560.
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int targetW = (int) Math.min(UiScale.px(720), screen.getWidth() * 0.85);
        int targetH = (int) Math.min(UiScale.px(800), screen.getHeight() * 0.85);
        Dimension pref = frame.getSize();
        int w = Math.max(pref.width, targetW);
        int h = Math.max(pref.height, targetH);
        frame.setSize(Math.max(w, UiScale.px(560)), Math.max(h, UiScale.px(480)));
        frame.setMinimumSize(new Dimension(UiScale.px(560), UiScale.px(480)));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
