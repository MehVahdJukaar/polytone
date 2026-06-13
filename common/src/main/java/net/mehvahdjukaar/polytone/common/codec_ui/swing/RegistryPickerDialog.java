package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class RegistryPickerDialog extends JDialog {

    private final List<Identifier> allEntries;
    private final DefaultListModel<Identifier> listModel = new DefaultListModel<>();
    private final JList<Identifier> list = new JList<>(listModel);
    private final JTextField search = new JTextField();
    private @Nullable Identifier picked;
    private final Consumer<Identifier> onPick;

    public RegistryPickerDialog(@Nullable Frame owner,
                                ResourceKey<? extends Registry<?>> registryKey,
                                @Nullable Identifier initial,
                                Consumer<Identifier> onPick) {
        super(owner, "Pick: " + registryKey.identifier(), true);
        this.onPick = onPick;
        this.allEntries = collectEntries(registryKey);

        JPanel content = new JPanel(new BorderLayout(0, UiScale.px(10)));
        content.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(14), UiScale.px(14), UiScale.px(14), UiScale.px(14)));

        // ----- Header: title + search -----
        JPanel north = new JPanel(new BorderLayout(0, UiScale.px(8)));
        JLabel title = new JLabel(registryKey.identifier().toString());
        title.setFont(UiScale.deriveFont(title.getFont(), Font.BOLD, 2f));
        north.add(title, BorderLayout.NORTH);

        search.putClientProperty("JTextField.placeholderText", "Filter...");
        search.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        north.add(search, BorderLayout.CENTER);
        content.add(north, BorderLayout.NORTH);

        // ----- Center: list -----
        list.setVisibleRowCount(20);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        scroll.setPreferredSize(UiScale.dim(440, 460));
        content.add(scroll, BorderLayout.CENTER);

        if (allEntries.isEmpty()) {
            JLabel warn = new JLabel("(registry not available — no entries to pick from)");
            warn.setForeground(new Color(0xC0392B));
            warn.setBorder(BorderFactory.createEmptyBorder(UiScale.px(4), 0, 0, 0));
            north.add(warn, BorderLayout.SOUTH);
        }

        // ----- Footer: right-aligned action row -----
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.putClientProperty("JButton.buttonType", "default");
        ok.addActionListener(e -> confirm());
        cancel.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new BorderLayout());
        Box right = Box.createHorizontalBox();
        right.add(cancel);
        right.add(Box.createHorizontalStrut(UiScale.px(8)));
        right.add(ok);
        footer.add(right, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) confirm();
            }
        });

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refilter(); }
            @Override public void removeUpdate(DocumentEvent e) { refilter(); }
            @Override public void changedUpdate(DocumentEvent e) { refilter(); }
        });

        refilter();
        if (initial != null) {
            list.setSelectedValue(initial, true);
        }

        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private void confirm() {
        picked = list.getSelectedValue();
        if (picked != null) {
            onPick.accept(picked);
        }
        dispose();
    }

    private void refilter() {
        String q = search.getText().toLowerCase(Locale.ROOT);
        listModel.clear();
        for (Identifier id : allEntries) {
            if (q.isEmpty() || id.toString().toLowerCase(Locale.ROOT).contains(q)) {
                listModel.addElement(id);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<Identifier> collectEntries(ResourceKey<? extends Registry<?>> registryKey) {
        List<Identifier> out = new ArrayList<>();
        // Try the active client level registry first; fall back to BuiltInRegistries; else empty.
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                var access = mc.level.registryAccess();
                var lookupOpt = access.lookup((ResourceKey) registryKey);
                if (lookupOpt.isPresent()) {
                    Registry registry = (Registry) access.lookupOrThrow((ResourceKey) registryKey);
                    for (Object key : registry.keySet()) {
                        out.add((Identifier) key);
                    }
                    return out;
                }
            }
        } catch (Throwable ignored) {}
        try {
            var holderOpt = BuiltInRegistries.REGISTRY.get(registryKey.identifier());
            if (holderOpt.isPresent()) {
                Registry registry = (Registry) holderOpt.get().value();
                for (Object key : registry.keySet()) {
                    out.add((Identifier) key);
                }
            }
        } catch (Throwable ignored) {}
        return out;
    }
}
