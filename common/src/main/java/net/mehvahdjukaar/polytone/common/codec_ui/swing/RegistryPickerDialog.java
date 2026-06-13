package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(new JLabel("Search:"), BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(UiScale.dim(400, 400));
        content.add(scroll, BorderLayout.CENTER);

        if (allEntries.isEmpty()) {
            JLabel warn = new JLabel("(registry not available — no entries to pick from)");
            warn.setForeground(java.awt.Color.RED);
            content.add(warn, BorderLayout.NORTH);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> confirm());
        cancel.addActionListener(e -> dispose());
        buttons.add(ok);
        buttons.add(cancel);
        content.add(buttons, BorderLayout.SOUTH);

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
