package net.mehvahdjukaar.polytone.content.tabs;

import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Client-side bridge for the editor's creative tab tooling.
 *
 * <p>Two independent halves. <b>Preview</b> installs the modifier being edited as a
 * {@link CreativeTabsModifiersManager.ModifierOverride}, so it stands in for the saved file on the tabs it
 * targets with no resource reload - attribute changes land at once, item contents on the next tab
 * rebuild (the open creative screen re-checks every tick). <b>Picking</b> turns the creative screen
 * into an item picker: the overlay marks what the edited modifier removes and adds, and clicks report
 * the item back to the editor instead of grabbing it.
 */
public final class CreativeTabPreview implements CreativeTabsModifiersManager.ModifierOverride {

    private static final CreativeTabPreview INSTANCE = new CreativeTabPreview();

    private static boolean pickingEnabled = false;
    @Nullable
    private static Consumer<ItemStack> pickListener;

    // Latest value decoded from the editor form, plus the tabs it resolves to. Drives the overlay; only
    // the copy handed to pushPreview is ever applied to the game.
    @Nullable
    private static CreativeTabModifier edited;
    private static Set<Identifier> editedTargets = Set.of();

    // Items the author has picked but not yet written into the form, so a long picking session stays
    // readable in game.
    private static final Set<Identifier> pending = new LinkedHashSet<>();

    // ---- the installed override ------------------------------------------------------------------

    @Nullable
    private CreativeTabModifier previewed;
    private Set<ResourceKey<CreativeModeTab>> previewedTargets = Set.of();
    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> restore = new HashMap<>();

    private CreativeTabPreview() {
    }

    @Override
    @Nullable
    public CreativeTabModifier modifierFor(ResourceKey<CreativeModeTab> tab) {
        return previewedTargets.contains(tab) ? previewed : null;
    }

    @Override
    public void onApplied(ResourceKey<CreativeModeTab> tab, CreativeTabModifier previous) {
        restore.put(tab, previous);
    }

    /**
     * Apply {@code mod} to the live game as if it were the saved file {@code fileId}, or drop a previous
     * preview with a null modifier. Marshals to the render thread.
     */
    public static void pushPreview(@Nullable Identifier fileId, @Nullable CreativeTabModifier mod) {
        Minecraft.getInstance().execute(() -> INSTANCE.install(fileId, mod));
    }

    private void install(@Nullable Identifier fileId, @Nullable CreativeTabModifier mod) {
        // Undo the attribute writes of the previous preview before installing the new one.
        for (var e : restore.entrySet()) {
            e.getValue().applyAttributes(e.getKey());
        }
        restore.clear();

        previewed = mod;
        previewedTargets = mod == null ? Set.of() : resolveTargets(fileId, mod);
        Polytone.CREATIVE_TABS_MODIFIERS.setOverride(mod == null ? null : this);
        for (var key : previewedTargets) {
            // A tab nothing modified yet has no event listener; the preview needs one to reach it.
            PlatStuff.addTabEventForTab(key);
        }
        CreativeModeTabs.CACHED_PARAMETERS = null; // makes the open screen rebuild its contents next tick
    }

    // ---- picking ---------------------------------------------------------------------------------

    public static boolean isPickingEnabled() {
        return pickingEnabled;
    }

    public static void setPickingEnabled(boolean enabled) {
        pickingEnabled = enabled;
    }

    /** The editor registers a listener here; the overlay fires it when an item is clicked. */
    public static void setPickListener(@Nullable Consumer<ItemStack> listener) {
        pickListener = listener;
    }

    /** Called by the overlay (render thread) when the user clicks an item while picking. */
    public static void onPick(ItemStack stack) {
        Consumer<ItemStack> l = pickListener;
        if (l != null) l.accept(stack);
    }

    public static void setPending(Set<Identifier> ids) {
        pending.clear();
        pending.addAll(ids);
    }

    static int pendingCount() {
        return pending.size();
    }

    static boolean isPending(Item item) {
        return !pending.isEmpty() && pending.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    // ---- what the overlay draws from -------------------------------------------------------------

    /** Tracks the editor form so the overlay can show what the modifier currently matches. */
    public static void setEdited(@Nullable Identifier fileId, @Nullable CreativeTabModifier mod) {
        edited = mod;
        editedTargets = mod == null ? Set.of()
                : resolveTargets(fileId, mod).stream()
                .map(ResourceKey::identifier)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    static CreativeTabModifier edited() {
        return edited;
    }

    static boolean targets(@Nullable Identifier tabId) {
        return tabId != null && editedTargets.contains(tabId);
    }

    /** Whether the edited modifier reaches the tab currently open - nothing it says applies if not. */
    public static boolean targetsOpenTab() {
        return targets(openTab());
    }

    /** Id of the creative tab currently selected in the open creative screen. */
    @Nullable
    public static Identifier openTab() {
        CreativeModeTab tab = selectedTab();
        return tab == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
    }

    /** Items of the open tab that the edited modifier's removals match - the count the editor shows. */
    public static int countRemoved() {
        CreativeModeTab tab = selectedTab();
        CreativeTabModifier mod = edited;
        if (tab == null || mod == null || mod.removals().isEmpty()) return 0;
        int count = 0;
        for (ItemStack stack : tab.getDisplayItems()) {
            if (matchesRemoval(mod.removals(), stack)) count++;
        }
        return count;
    }

    static boolean matchesRemoval(List<ItemPredicate> removals, ItemStack stack) {
        for (ItemPredicate p : removals) {
            if (p.test(stack)) return true;
        }
        return false;
    }

    @Nullable
    private static CreativeModeTab selectedTab() {
        return Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen
                ? CreativeModeInventoryScreen.selectedTab
                : null;
    }

    // The tabs a modifier applies to. This runs on every keystroke in the editor, on files that are
    // half written by definition, so it stays quiet: no implicit target simply means no tabs yet.
    private static Set<ResourceKey<CreativeModeTab>> resolveTargets(@Nullable Identifier fileId,
                                                                    CreativeTabModifier mod) {
        Identifier id = fileId != null ? fileId : Polytone.res("editor_preview");
        Targets targets = mod.registerTab() ? Targets.ofIds(id) : mod.targets();
        boolean implicitTarget = BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id);
        if (targets.entries().isEmpty() && !implicitTarget) return Set.of();

        Set<ResourceKey<CreativeModeTab>> set = new HashSet<>();
        try {
            for (var holder : targets.compute(id, BuiltInRegistries.CREATIVE_MODE_TAB)) {
                holder.unwrapKey().ifPresent(set::add);
            }
        } catch (Exception e) {
            Polytone.LOGGER.debug("Could not resolve creative tab targets for the editor: {}", e.getMessage());
        }
        return set;
    }
}
