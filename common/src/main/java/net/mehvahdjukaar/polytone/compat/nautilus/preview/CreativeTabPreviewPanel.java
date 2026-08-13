package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiStyle;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

// Creative tab modifiers decorate something that only exists in the running game, so this panel is a
// remote control rather than a renderer: apply pushes the edited modifier onto the live tabs with no
// reload, pick turns the creative screen into an item picker writing into removals/additions.
// While picking, the overlay also washes every item the current removals match, which is the only
// practical way to check a tag or regex predicate.
public final class CreativeTabPreviewPanel implements TabPreview {

    private static final String FOLDER = "creative_tab_modifiers";

    private PreviewSurface root;
    private final PreviewStatus status = new PreviewStatus();

    private final JButton applyButton = UiStyle.ctaButton("Apply to game", UiICons.refresh());
    private final JButton undoButton = UiStyle.buttonAsToolbar(new JButton("Undo preview"));

    private final JButton targetButton = UiStyle.primaryButton("Target the open tab", UiICons.search());
    private final JToggleButton pickToggle = new JToggleButton("Pick items in game", UiICons.eye());
    private final JLabel pickActive = StyledLabels.accentSmall("● Overlay active in the game window");

    private final DefaultListModel<String> pickedModel = new DefaultListModel<>();
    private final JList<String> pickedList = new JList<>(pickedModel);
    private final JButton toRemovals = UiStyle.buttonAsToolbar(new JButton("→ removals"));
    private final JButton toAdditions = UiStyle.buttonAsToolbar(new JButton("→ additions"));
    private final JButton clearPicked = UiStyle.buttonAsToolbar(new JButton("Clear"));

    @Nullable
    private final ResourceLocation fileId;
    @Nullable
    private final Consumer<JsonElement> formWriter;

    // Latest form state; the modifier drives "apply", the json is what the pick buttons rewrite.
    @Nullable
    private CreativeTabModifier currentModifier;
    @Nullable
    private JsonElement currentJson;

    // A write-back re-decodes the form asynchronously, so "apply what I just wrote" waits for the value.
    private boolean applyOnNextValue;

    public CreativeTabPreviewPanel(TabPreview.Context ctx) {
        Path file = ctx.file();
        this.fileId = PreviewIds.of(ctx.contentId(), file, FOLDER);
        this.formWriter = ctx.formWriter();

        buildLayout();

        applyButton.setToolTipText("Apply the modifier you're editing to the live tabs instantly (no reload).");
        applyButton.addActionListener(e -> apply());
        applyButton.setEnabled(false);

        undoButton.setToolTipText("Drop the preview and put the tabs back to their loaded state.");
        undoButton.addActionListener(e -> {
            CreativeTabPreview.pushPreview(fileId, null);
            status.info("Preview dropped - tabs restored to their loaded state.");
        });

        targetButton.setToolTipText("Write the creative tab currently open in game into this file's targets.");
        targetButton.addActionListener(e -> targetOpenTab());

        pickToggle.setToolTipText("Mark what this modifier removes and adds on the open creative screen, "
                + "and collect the items you click.");
        pickToggle.setIconTextGap(UiScale.small());
        pickToggle.addActionListener(e -> setPicking(pickToggle.isSelected()));
        pickActive.setVisible(false);

        toRemovals.setToolTipText("Add the picked items to this file's removals, as an items_match predicate.");
        toRemovals.addActionListener(e -> writePicked(true));
        toAdditions.setToolTipText("Add the picked items to this file's additions.");
        toAdditions.addActionListener(e -> writePicked(false));
        clearPicked.addActionListener(e -> setPicked(Set.of()));
        updatePickedButtons();

        status.info("Open the creative screen in game, then use these controls.");
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.currentJson = json;
        this.currentModifier = value instanceof CreativeTabModifier m ? m : null;
        applyButton.setEnabled(currentModifier != null);
        // The in-game overlay highlights against whatever the form currently says.
        CreativeTabPreview.setEdited(fileId, currentModifier);
        if (applyOnNextValue && currentModifier != null) {
            applyOnNextValue = false;
            apply();
        }
    }

    @Override
    public void dispose() {
        CreativeTabPreview.setPickingEnabled(false);
        CreativeTabPreview.setPickListener(null);
        CreativeTabPreview.setEdited(fileId, null);
        CreativeTabPreview.setPending(Set.of());
        CreativeTabPreview.pushPreview(fileId, null);
    }

    private void apply() {
        if (currentModifier == null) {
            status.error("No valid modifier yet - fix the form first.");
            return;
        }
        CreativeTabPreview.pushPreview(fileId, currentModifier);
        // Item contents rebuild on the screen's next tick, so read the count back once it has.
        Minecraft.getInstance().execute(() -> {
            boolean reaches = CreativeTabPreview.targetsOpenTab();
            int removed = CreativeTabPreview.countRemoved();
            SwingUtilities.invokeLater(() -> {
                if (!reaches) {
                    status.error("Applied, but this file doesn't target the open tab - use \"Target the open tab\".");
                } else {
                    status.info(removed > 0
                            ? "Applied - removing " + removed + " items from the open tab."
                            : "Applied to the live tabs.");
                }
            });
        });
    }

    private void targetOpenTab() {
        Minecraft.getInstance().execute(() -> {
            ResourceLocation tab = CreativeTabPreview.openTab();
            SwingUtilities.invokeLater(() -> {
                if (tab == null) {
                    status.error("No creative screen open in the game window.");
                    return;
                }
                if (writeForm(obj -> obj.add("targets", new JsonPrimitive(tab.toString())))) {
                    status.info("Targeting " + tab + ".");
                }
            });
        });
    }

    private void setPicking(boolean on) {
        CreativeTabPreview.setPickingEnabled(on);
        pickActive.setVisible(on);
        pickToggle.setText(on ? "Stop picking" : "Pick items in game");
        if (on) {
            CreativeTabPreview.setPickListener(stack -> SwingUtilities.invokeLater(() -> togglePicked(stack)));
            status.info("Overlay active: red items are removed, green added. Click items to select them.");
            warnIfTabNotTargeted();
        } else {
            CreativeTabPreview.setPickListener(null);
            status.setText("");
        }
    }

    // Picking and writing are useless if the file doesn't reach the tab being looked at; say so early
    // rather than letting the author collect items that end up doing nothing.
    private void warnIfTabNotTargeted() {
        Minecraft.getInstance().execute(() -> {
            boolean reaches = CreativeTabPreview.targetsOpenTab();
            if (!reaches) {
                SwingUtilities.invokeLater(() ->
                        status.error("This file doesn't target the open tab - use \"Target the open tab\"."));
            }
        });
    }

    // Clicking an item toggles it, so a misclick is undone the same way it was made.
    private void togglePicked(ItemStack stack) {
        String id = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (!pickedModel.removeElement(id)) pickedModel.addElement(id);
        syncPending();
    }

    private void setPicked(Set<String> ids) {
        pickedModel.clear();
        ids.forEach(pickedModel::addElement);
        syncPending();
    }

    private void syncPending() {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (int i = 0; i < pickedModel.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(pickedModel.get(i));
            if (id != null) ids.add(id);
        }
        CreativeTabPreview.setPending(ids);
        updatePickedButtons();
    }

    private void updatePickedButtons() {
        boolean any = !pickedModel.isEmpty();
        toRemovals.setEnabled(any);
        toAdditions.setEnabled(any);
        clearPicked.setEnabled(any);
    }

    // Picked items become one items_match removal / one addition entry, merged into an existing one when
    // there is a matching one already, so repeated picking doesn't grow a wall of one-item entries.
    private void writePicked(boolean removals) {
        if (pickedModel.isEmpty()) return;
        JsonArray items = new JsonArray();
        for (int i = 0; i < pickedModel.size(); i++) {
            items.add(pickedModel.get(i));
        }
        boolean written = writeForm(obj -> {
            JsonArray list = array(obj, removals ? "removals" : "additions");
            JsonObject entry = firstMergeable(list, removals);
            if (entry == null) {
                entry = new JsonObject();
                if (removals) entry.addProperty("type", "items_match");
                entry.add("items", new JsonArray());
                list.add(entry);
            }
            JsonArray existing = entry.getAsJsonArray("items");
            for (JsonElement item : items) {
                if (!existing.contains(item)) existing.add(item);
            }
        });
        if (!written) return;
        status.info("Wrote " + pickedModel.size() + " items into " + (removals ? "removals" : "additions") + ".");
        setPicked(Set.of());
        // Close the loop: the items should disappear from the tab without a second button press.
        applyOnNextValue = true;
    }

    // An entry this panel wrote before: a plain items list (additions) or an items_match (removals).
    @Nullable
    private static JsonObject firstMergeable(JsonArray list, boolean removals) {
        for (JsonElement e : list) {
            if (!(e instanceof JsonObject o) || !(o.get("items") instanceof JsonArray)) continue;
            JsonElement type = o.get("type");
            boolean isItemsMatch = type != null && "items_match".equals(type.getAsString());
            if (isItemsMatch == removals && (!removals || o.size() == 2)) return o;
        }
        return null;
    }

    private static JsonArray array(JsonObject obj, String field) {
        if (obj.get(field) instanceof JsonArray existing) return existing;
        JsonArray created = new JsonArray();
        obj.add(field, created);
        return created;
    }

    private boolean writeForm(Consumer<JsonObject> mutation) {
        if (formWriter == null) {
            status.error("This view is read-only - open the file to edit it.");
            return false;
        }
        if (!(currentJson instanceof JsonObject obj)) {
            status.error("The form isn't a valid object yet - fix it first.");
            return false;
        }
        JsonObject copy = obj.deepCopy();
        mutation.accept(copy);
        formWriter.accept(copy);
        return true;
    }

    private void buildLayout() {
        Box toolbar = PreviewPanels.header("Live Creative Tabs", status);

        Box content = Box.createVerticalBox();
        PreviewPanels.addRow(content, PreviewPanels.ellipsizing(
                StyledLabels.small("Edits preview on the live creative screen.")));
        content.add(Box.createVerticalStrut(UiScale.small()));
        PreviewPanels.addCtaWithUndo(content, applyButton, undoButton);

        JPanel group = PreviewPanels.outlinedGroup("Target & pick");
        PreviewPanels.addRow(group, targetButton);
        group.add(Box.createVerticalStrut(UiScale.small()));
        PreviewPanels.addRow(group, pickToggle);
        PreviewPanels.addRow(group, PreviewPanels.ellipsizing(pickActive));

        pickedList.setVisibleRowCount(5);
        JScrollPane scroll = new JScrollPane(pickedList);
        scroll.setPreferredSize(new Dimension(UiScale.px(180), UiScale.px(90)));
        PreviewPanels.addRow(group, scroll);

        Box pickedRow = Box.createHorizontalBox();
        pickedRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pickedRow.add(toRemovals);
        pickedRow.add(Box.createHorizontalStrut(UiScale.small()));
        pickedRow.add(toAdditions);
        pickedRow.add(Box.createHorizontalGlue());
        pickedRow.add(clearPicked);
        PreviewPanels.addRow(group, pickedRow);
        content.add(group);

        root = new PreviewSurface(toolbar, content);
        root.setMinimumSize(new Dimension(UiScale.px(180), UiScale.px(140)));
    }
}
