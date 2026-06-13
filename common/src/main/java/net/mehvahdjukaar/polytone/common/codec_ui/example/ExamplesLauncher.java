package net.mehvahdjukaar.polytone.common.codec_ui.example;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaEditor;
import net.mehvahdjukaar.polytone.common.codec_ui.swing.SwingSchemaEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Debug launcher: a tiny Swing window with one button per SchemaCodec example.
 * Used during development to manually exercise the SchemaEditor on a variety of shapes.
 */
public final class ExamplesLauncher {
    private static final Logger LOGGER = LoggerFactory.getLogger("ExamplesLauncher");

    private ExamplesLauncher() {}

    public static void open() {
        forceNonHeadless();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SchemaCodec Examples");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            addButton(panel, "Foo (list + ints)", FooExample.SCHEMA_CODEC,
                    foo -> LOGGER.info("Saved Foo: {}", foo));
            addButton(panel, "PhysicsConfig (bool/float/double/int)", PhysicsConfigExample.SCHEMA_CODEC,
                    v -> LOGGER.info("Saved Physics: {}", v));
            addButton(panel, "MapEntry (string -> string)", MapEntryExample.SCHEMA_CODEC,
                    v -> LOGGER.info("Saved MapEntry: {}", v));
            addButton(panel, "Action (sum type)", ActionExample.SCHEMA_CODEC,
                    v -> LOGGER.info("Saved Action: {}", v));
            addButton(panel, "NestedRecipe", NestedRecipeExample.SCHEMA_CODEC,
                    v -> LOGGER.info("Saved Recipe: {}", v));
            addButton(panel, "Migrated GuiDepthTarget (from project)", MigratedGuiDepthTargetExample.SCHEMA_CODEC,
                    v -> LOGGER.info("Saved MigratedGuiDepthTarget: {}", v));

            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static <T> void addButton(JPanel panel, String label, SchemaCodec<T> codec, Consumer<T> onSave) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            SchemaEditor editor = new SwingSchemaEditor();
            editor.open(codec, null, onSave);
        });
        panel.add(button);
    }

    // Duplicated from SwingSchemaEditor (which keeps its copy private). NeoForge launches
    // with java.awt.headless=true and GraphicsEnvironment caches the flag; setAccessible on
    // a java.desktop private field is blocked since JDK 16, so we go through sun.misc.Unsafe
    // (jdk.unsupported is opened for reflection) to overwrite the cache directly.
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
}
