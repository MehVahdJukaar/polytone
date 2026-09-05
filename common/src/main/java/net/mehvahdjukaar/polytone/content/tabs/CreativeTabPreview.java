package net.mehvahdjukaar.polytone.content.tabs;

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

// Preview installs the edited modifier as a ModifierOverride, so it stands in for the saved file with no
// reload: attributes land at once, item contents on the next tab rebuild. Picking turns the creative
// screen into an item picker.
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

    // null drops a previous preview; marshals to the render thread
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
        // No per-tab listener registration needed: CreativeModeTabMixin hooks buildContents itself,
        // so every tab (modified or not) already runs through us.
        CreativeModeTabs.CACHED_PARAMETERS = null; // makes the open screen rebuild its contents next tick
    }

    public static boolean isPickingEnabled() {
        return pickingEnabled;
    }

    public static void setPickingEnabled(boolean enabled) {
        pickingEnabled = enabled;
    }

    public static void setPickListener(@Nullable Consumer<ItemStack> listener) {
        pickListener = listener;
    }

    public static void onPick(ItemStack stack) {
        Consumer<ItemStack> l = pickListener;
        if (l != null) l.accept(stack);
    }

    public static void setPending(Set<Identifier> ids) {
        pending.clear();
        pending.addAll(ids);
    }

    public static int pendingCount() {
        return pending.size();
    }

    public static boolean isPending(Item item) {
        return !pending.isEmpty() && pending.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public static void setEdited(@Nullable Identifier fileId, @Nullable CreativeTabModifier mod) {
        edited = mod;
        editedTargets = mod == null ? Set.of()
                : resolveTargets(fileId, mod).stream()
                .map(ResourceKey::identifier)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    public static CreativeTabModifier edited() {
        return edited;
    }

    public static boolean targets(@Nullable Identifier tabId) {
        return tabId != null && editedTargets.contains(tabId);
    }

    public static boolean targetsOpenTab() {
        return targets(openTab());
    }

    @Nullable
    public static Identifier openTab() {
        CreativeModeTab tab = selectedTab();
        return tab == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
    }

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

    public static boolean matchesRemoval(List<ItemPredicate> removals, ItemStack stack) {
        for (ItemPredicate p : removals) {
            if (p.test(stack)) return true;
        }
        return false;
    }

    @Nullable
    private static CreativeModeTab selectedTab() {
        return Minecraft.getInstance().gui.screen() instanceof CreativeModeInventoryScreen
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
