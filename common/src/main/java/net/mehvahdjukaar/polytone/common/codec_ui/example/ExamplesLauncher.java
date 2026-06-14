package net.mehvahdjukaar.polytone.common.codec_ui.example;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.swing.SwingSchemaEditor;
import net.mehvahdjukaar.polytone.common.codec_ui.swing.UiScale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
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
 * in the order each group is first encountered. A live substring filter sits above the
 * scrollable button column.
 *
 * <p>Scaling is fully automatic. To override, pass {@code -Dflatlaf.uiScale=1.5x}
 * (or 150%, 1.5, etc) on the JVM command line.</p>
 */
public final class ExamplesLauncher {

    private ExamplesLauncher() {}

    public static void open() {
        forceNonHeadless();
        SwingUtilities.invokeLater(() -> {
            // Install L&F + DPI-scaled fonts BEFORE constructing any Swing component.
            SwingSchemaEditor.bootstrapLF();

            JFrame frame = new JFrame("Polytone Codec Editor");
            // Launcher stays open across editor sessions; closing it here exits the tool.
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            JPanel content = new JPanel(new BorderLayout(0, UiScale.med()));
            content.setBorder(BorderFactory.createEmptyBorder(
                    UiScale.large(), UiScale.large(), UiScale.large(), UiScale.large()));

            // ----- North: header + separator + search box -----
            JPanel north = new JPanel();
            north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
            north.setAlignmentX(Component.LEFT_ALIGNMENT);

            north.add(buildHeader());
            north.add(Box.createVerticalStrut(UiScale.med()));

            JSeparator headerSep = new JSeparator();
            headerSep.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiScale.px(1)));
            north.add(headerSep);
            north.add(Box.createVerticalStrut(UiScale.med()));

            JTextField searchField = new JTextField();
            searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
            searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchField.getPreferredSize().height));
            searchField.putClientProperty("JTextField.placeholderText", "Search codecs...");
            searchField.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
            north.add(searchField);

            content.add(north, BorderLayout.NORTH);

            // ----- Center: grouped button column -----
            JPanel buttonColumn = new JPanel();
            buttonColumn.setLayout(new BoxLayout(buttonColumn, BoxLayout.Y_AXIS));
            buttonColumn.setBorder(BorderFactory.createEmptyBorder(
                    UiScale.small(), 0, UiScale.small(), 0));

            List<GroupBlock> groups = buildGroupedButtons(buttonColumn);
            buttonColumn.add(Box.createVerticalGlue());

            JScrollPane scroll = new JScrollPane(buttonColumn);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(UiScale.px(16));
            scroll.getViewport().setOpaque(false);
            content.add(scroll, BorderLayout.CENTER);

            // ----- South: auto-scale footer -----
            JLabel footer = new JLabel("Auto-scaled: " + UiScale.scaleAsPercent()
                    + "   (override with -Dflatlaf.uiScale=...)");
            footer.setForeground(mutedColor());
            footer.setFont(UiScale.deriveFont(footer.getFont(), Font.PLAIN, -1f));
            footer.setBorder(BorderFactory.createEmptyBorder(UiScale.med(), 0, 0, 0));
            content.add(footer, BorderLayout.SOUTH);

            // ----- Wire search filtering -----
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
                private void applyFilter() {
                    filter(groups, searchField.getText(), buttonColumn);
                }
            });

            frame.setContentPane(content);
            // Generous logical-px baselines so it's clearly readable at 1x and
            // very comfortable at 2x. 540x720 at 1x becomes 1080x1440 at 2x.
            frame.pack();
            Dimension packed = frame.getSize();
            int w = Math.max(packed.width, UiScale.px(540));
            int h = Math.max(packed.height, UiScale.px(720));
            frame.setSize(w, h);
            frame.setMinimumSize(new Dimension(UiScale.px(480), UiScale.px(560)));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Grab focus on the search field after the frame is realized so the user
            // can immediately type to filter.
            SwingUtilities.invokeLater(searchField::requestFocusInWindow);
        });
    }

    // -------------------- Header --------------------

    private static JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Polytone Codec Editor");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Derive font from L&F-scaled base — adds +6 logical pt; FlatLaf already scales the base.
        title.setFont(UiScale.deriveFont(title.getFont(), Font.BOLD, 6f));

        JLabel subtitle = new JLabel("Pick a value type to edit");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setForeground(mutedColor());
        subtitle.setBorder(BorderFactory.createEmptyBorder(UiScale.px(2), 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        return header;
    }

    // -------------------- Grouped buttons --------------------

    /** Per-group block in the button column: a header label plus its button rows. */
    private static final class GroupBlock {
        final String groupName;
        final JLabel header;
        final JSeparator separator;
        final List<JComponent> rows = new ArrayList<>();
        final List<String> labels = new ArrayList<>();

        GroupBlock(String groupName, JLabel header, JSeparator separator) {
            this.groupName = groupName;
            this.header = header;
            this.separator = separator;
        }
    }

    private static List<GroupBlock> buildGroupedButtons(JPanel buttonColumn) {
        // Force-load VanillaCodecs to guarantee companion registration runs before any
        // schema is resolved. Static-init via static-final-field access has been unreliable
        // in some loader configurations.
        VanillaCodecs.bootstrap();
        System.out.println("[codec_ui] >>> ExamplesLauncher: CodecRegistry.all() about to fire <<<");

        // Bucket entries by group in first-appearance order.
        Map<String, List<CodecRegistry.Entry>> buckets = new LinkedHashMap<>();
        for (CodecRegistry.Entry entry : CodecRegistry.all()) {
            buckets.computeIfAbsent(entry.group(), g -> new ArrayList<>()).add(entry);
        }

        List<GroupBlock> blocks = new ArrayList<>();
        boolean firstGroup = true;
        for (Map.Entry<String, List<CodecRegistry.Entry>> e : buckets.entrySet()) {
            if (!firstGroup) {
                // LARGE gap between groups (sections).
                buttonColumn.add(Box.createVerticalStrut(UiScale.large()));
            }
            firstGroup = false;

            JLabel header = sectionHeader(e.getKey());
            buttonColumn.add(header);

            JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
            sep.setAlignmentX(Component.LEFT_ALIGNMENT);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiScale.px(1)));
            buttonColumn.add(sep);
            buttonColumn.add(Box.createVerticalStrut(UiScale.small()));

            GroupBlock block = new GroupBlock(e.getKey(), header, sep);

            for (CodecRegistry.Entry entry : e.getValue()) {
                JButton button = makeButton(entry);
                // MED gap between buttons in the same group.
                Component strut = Box.createVerticalStrut(UiScale.small());
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
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Let the L&F decide the actual height (FlatLaf + Button.minimumHeight handles it);
        // we only constrain the row to a full-width strip with a reasonable height ceiling.
        int rowH = Math.max(button.getPreferredSize().height, UiScale.px(34));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowH));
        button.setPreferredSize(new Dimension(UiScale.px(280), rowH));
        Consumer<T> onSave = v -> log(label, v);
        button.addActionListener(e -> {
            // Use the labeled overload so the editor's title bar reads "Edit: <label>".
            // The editor reuses one persistent JFrame across calls (see SwingSchemaEditor).
            new SwingSchemaEditor().open(codec, label, null, onSave);
        });
        return button;
    }

    // ---- Theming helpers ----

    /** Subtitle / footer / section-header color — adapts to dark/light L&F. */
    private static Color mutedColor() {
        Color c = UIManager.getColor("Label.disabledForeground");
        if (c != null) return c;
        return new Color(0x999999);
    }

    private static JLabel sectionHeader(String text) {
        JLabel label = new JLabel(text.toUpperCase(Locale.ROOT));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Smaller, bold, dim — no manual scaling. -1pt logical from L&F base.
        label.setFont(UiScale.deriveFont(label.getFont(), Font.BOLD, -1f));
        label.setForeground(mutedColor());
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, UiScale.px(2), 0));
        return label;
    }

    // -------------------- Filtering --------------------

    private static void filter(List<GroupBlock> groups, String query, JPanel container) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean empty = q.isEmpty();

        for (GroupBlock block : groups) {
            boolean anyVisible = false;
            // rows: [button, strut, button, strut, ...]
            for (int i = 0; i < block.labels.size(); i++) {
                String lbl = block.labels.get(i).toLowerCase(Locale.ROOT);
                boolean show = empty || lbl.contains(q);
                if (show) anyVisible = true;
                int base = i * 2;
                if (base < block.rows.size()) block.rows.get(base).setVisible(show);
                if (base + 1 < block.rows.size()) block.rows.get(base + 1).setVisible(show);
            }
            block.header.setVisible(anyVisible);
            block.separator.setVisible(anyVisible);
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
