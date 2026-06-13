package net.mehvahdjukaar.polytone.common.codec_ui.example;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaEditor;
import net.mehvahdjukaar.polytone.common.codec_ui.swing.SwingSchemaEditor;
import net.mehvahdjukaar.polytone.common.codec_ui.swing.UiScale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Production launcher: a Swing tool window with one button per SchemaCodec example.
 * Buttons are sourced from {@link CodecRegistry#all()} and grouped by {@code entry.group()}
 * in the order each group is first encountered. A FlatLaf scale picker and a live
 * substring filter sit above the scrollable button column.
 */
public final class ExamplesLauncher {

    private static final String[] SCALE_OPTIONS = {
            "100%", "125%", "150%", "175%", "200%", "250%", "300%"
    };
    private static final String DEFAULT_SCALE = "100%";

    private ExamplesLauncher() {}

    public static void open() {
        forceNonHeadless();
        SwingUtilities.invokeLater(() -> {
            // Install L&F + DPI-scaled fonts BEFORE constructing any Swing component.
            SwingSchemaEditor.bootstrapLF();

            JFrame frame = new JFrame("Polytone Codec Editor");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            JPanel content = new JPanel(new BorderLayout(0, UiScale.px(8)));
            content.setBorder(BorderFactory.createEmptyBorder(
                    UiScale.px(12), UiScale.px(12), UiScale.px(12), UiScale.px(12)));

            // ----- North: header + toolbar (scale combo) + search box -----
            JPanel north = new JPanel();
            north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

            north.add(buildHeaderWithScalePicker(frame));
            north.add(Box.createVerticalStrut(UiScale.px(6)));
            JTextField searchField = new JTextField();
            searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiScale.px(28)));
            searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
            searchField.putClientProperty("JTextField.placeholderText", "Search...");
            north.add(searchField);

            content.add(north, BorderLayout.NORTH);

            // ----- Center: grouped button column -----
            JPanel buttonColumn = new JPanel();
            buttonColumn.setLayout(new BoxLayout(buttonColumn, BoxLayout.Y_AXIS));

            List<GroupBlock> groups = buildGroupedButtons(buttonColumn);

            buttonColumn.add(Box.createVerticalGlue());

            JScrollPane scroll = new JScrollPane(buttonColumn);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(UiScale.px(16));
            content.add(scroll, BorderLayout.CENTER);

            // ----- Wire search filtering -----
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
                private void applyFilter() {
                    String q = searchField.getText();
                    filter(groups, q, buttonColumn);
                }
            });

            frame.setContentPane(content);
            frame.setSize(UiScale.px(420), UiScale.px(620));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -------------------- Header + scale picker --------------------

    private static JPanel buildHeaderWithScalePicker(JFrame frame) {
        JPanel header = new JPanel(new BorderLayout(UiScale.px(8), 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Polytone Codec Editor");
        title.setFont(title.getFont().deriveFont(Font.BOLD, UiScale.px(20)));

        JLabel subtitle = new JLabel("Pick a value type to edit.");
        subtitle.setForeground(new Color(0x666666));
        subtitle.setBorder(BorderFactory.createEmptyBorder(UiScale.px(4), 0, UiScale.px(4), 0));

        titles.add(title);
        titles.add(subtitle);

        // Scale combo on the right.
        JPanel scaleBar = new JPanel();
        scaleBar.setLayout(new BoxLayout(scaleBar, BoxLayout.X_AXIS));
        JLabel scaleLabel = new JLabel("Scale:");
        scaleLabel.setForeground(new Color(0x666666));
        scaleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, UiScale.px(6)));

        JComboBox<String> scaleCombo = new JComboBox<>(SCALE_OPTIONS);
        scaleCombo.setSelectedItem(currentScaleAsPercent());
        scaleCombo.setMaximumSize(new Dimension(UiScale.px(90), UiScale.px(28)));
        scaleCombo.addActionListener(e -> {
            String pct = (String) scaleCombo.getSelectedItem();
            if (pct == null) return;
            applyFlatLafScale(pct);
            SwingUtilities.invokeLater(() -> {
                frame.pack();
                frame.setSize(UiScale.px(420), UiScale.px(620));
            });
        });

        scaleBar.add(scaleLabel);
        scaleBar.add(scaleCombo);

        header.add(titles, BorderLayout.WEST);
        header.add(scaleBar, BorderLayout.EAST);
        return header;
    }

    private static String currentScaleAsPercent() {
        String current = System.getProperty("flatlaf.uiScale");
        if (current == null || current.isBlank()) return DEFAULT_SCALE;
        try {
            // Accept "1.5", "1.5x", "150", "150%".
            String s = current.trim().toLowerCase(Locale.ROOT);
            if (s.endsWith("x")) s = s.substring(0, s.length() - 1);
            if (s.endsWith("%")) s = s.substring(0, s.length() - 1);
            float f = Float.parseFloat(s);
            int pct = f < 10f ? Math.round(f * 100f) : Math.round(f);
            String candidate = pct + "%";
            for (String opt : SCALE_OPTIONS) {
                if (opt.equals(candidate)) return opt;
            }
        } catch (Throwable ignored) {}
        return DEFAULT_SCALE;
    }

    private static void applyFlatLafScale(String pct) {
        String val = pct.replace("%", "");
        String scale;
        try {
            scale = (Float.parseFloat(val) / 100f) + "x";
        } catch (NumberFormatException ex) {
            scale = "1.0x";
        }
        System.setProperty("flatlaf.uiScale", scale);
        UIManager.put("flatlaf.uiScale", scale);
        try {
            com.formdev.flatlaf.FlatLaf.updateUI();
        } catch (Throwable t) {
            if (Polytone.LOGGER != null) {
                Polytone.LOGGER.warn("FlatLaf updateUI failed: {}", t.toString());
            }
        }
    }

    // -------------------- Grouped buttons --------------------

    /** Per-group block in the button column: a header label plus its button rows. */
    private static final class GroupBlock {
        final String groupName;
        final JLabel header;
        final List<JComponent> rows = new ArrayList<>();
        final List<String> labels = new ArrayList<>();

        GroupBlock(String groupName, JLabel header) {
            this.groupName = groupName;
            this.header = header;
        }
    }

    private static List<GroupBlock> buildGroupedButtons(JPanel buttonColumn) {
        // Bucket entries by group in first-appearance order.
        Map<String, List<CodecRegistry.Entry>> buckets = new LinkedHashMap<>();
        for (CodecRegistry.Entry entry : CodecRegistry.all()) {
            buckets.computeIfAbsent(entry.group(), g -> new ArrayList<>()).add(entry);
        }

        List<GroupBlock> blocks = new ArrayList<>();
        for (Map.Entry<String, List<CodecRegistry.Entry>> e : buckets.entrySet()) {
            JLabel header = sectionHeader(e.getKey());
            buttonColumn.add(header);
            GroupBlock block = new GroupBlock(e.getKey(), header);

            for (CodecRegistry.Entry entry : e.getValue()) {
                JButton button = makeButton(entry);
                Component strut = Box.createVerticalStrut(UiScale.px(6));
                buttonColumn.add(button);
                buttonColumn.add(strut);

                block.rows.add(button);
                if (strut instanceof JComponent jc) {
                    block.rows.add(jc);
                }
                block.labels.add(entry.label());
            }
            blocks.add(block);
        }
        return blocks;
    }

    private static <T> JButton makeButton(CodecRegistry.Entry entry) {
        @SuppressWarnings("unchecked")
        SchemaCodec<T> codec = (SchemaCodec<T>) entry.codec();
        String label = entry.label();
        JButton button = new JButton(label);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiScale.px(36)));
        button.setPreferredSize(new Dimension(UiScale.px(280), UiScale.px(36)));
        Consumer<T> onSave = v -> log(label, v);
        button.addActionListener(e -> {
            SchemaEditor editor = new SwingSchemaEditor();
            editor.open(codec, null, onSave);
        });
        return button;
    }

    private static JLabel sectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setFont(label.getFont().deriveFont(Font.ITALIC, UiScale.px(11)));
        label.setForeground(new Color(0x666666));
        label.setBorder(BorderFactory.createEmptyBorder(UiScale.px(8), 0, UiScale.px(2), 0));
        return label;
    }

    // -------------------- Filtering --------------------

    private static void filter(List<GroupBlock> groups, String query, JPanel container) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean empty = q.isEmpty();

        for (GroupBlock block : groups) {
            boolean anyVisible = false;
            // Each label has one button + one strut row (2 components).
            for (int i = 0; i < block.labels.size(); i++) {
                String lbl = block.labels.get(i).toLowerCase(Locale.ROOT);
                boolean show = empty || lbl.contains(q);
                if (show) anyVisible = true;
                // rows are stored as [button0, strut0, button1, strut1, ...]
                int base = i * 2;
                if (base < block.rows.size()) block.rows.get(base).setVisible(show);
                if (base + 1 < block.rows.size()) block.rows.get(base + 1).setVisible(show);
            }
            block.header.setVisible(anyVisible);
        }

        container.revalidate();
        container.repaint();
    }

    // -------------------- Misc --------------------

    private static void log(String label, Object value) {
        if (Polytone.LOGGER != null) {
            Polytone.LOGGER.info("Saved {}: {}", label, value);
        }
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
