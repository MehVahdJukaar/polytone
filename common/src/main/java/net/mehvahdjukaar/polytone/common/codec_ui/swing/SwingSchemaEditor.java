package net.mehvahdjukaar.polytone.common.codec_ui.swing;

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
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
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
        // Hi-DPI: use auto scaling so 4K displays render at sane sizes.
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.uiScale", "auto");
        // OpenGL pipeline: significantly faster than the default software renderer at 4K.
        System.setProperty("sun.java2d.opengl", "true");
        // Smooth text.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    private static void applySystemLookAndFeel() {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ignored) {
            // Fall back to Nimbus if system L&F unavailable.
            try {
                javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Throwable ignored2) {}
        }
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
        applySystemLookAndFeel();
        SwingWidget rootWidget = SwingWidgetFactory.create(codec.schema());

        if (initial != null) {
            DataResult<JsonElement> encoded = codec.codec().encodeStart(JsonOps.INSTANCE, initial);
            encoded.result().ifPresent(rootWidget::setJson);
        }

        JFrame frame = new JFrame("Schema Editor");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(rootWidget.component());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);

        JButton open = new JButton("Open");
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        JPanel south = new JPanel(new BorderLayout());
        south.add(errorLabel, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(open);
        buttons.add(save);
        buttons.add(cancel);
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

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = (int) Math.min(800, screen.getWidth() * 0.8);
        int maxH = (int) Math.min(600, screen.getHeight() * 0.8);
        Dimension pref = frame.getSize();
        int w = Math.min(pref.width, maxW);
        int h = Math.min(pref.height, maxH);
        frame.setSize(Math.max(w, 480), Math.max(h, 320));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
