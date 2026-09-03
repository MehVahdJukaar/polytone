package net.mehvahdjukaar.polytone.content.tabs;

import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

public final class CreativeTabPreview {

    private static boolean pickingEnabled = false;
    @Nullable
    private static Consumer<ItemStack> pickListener;

    @Nullable
    private static CreativeTabModifier edited;
    private static Set<ResourceLocation> editedTargets = Set.of();

    //picked in game but not written back into the form yet
    private static final Set<ResourceLocation> pending = new LinkedHashSet<>();

    @Nullable
    private static CreativeTabModifier previewed;
    private static Set<ResourceKey<CreativeModeTab>> previewedTargets = Set.of();
    private static final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> restore = new HashMap<>();

    @Nullable
    public static CreativeTabModifier modifierFor(ResourceKey<CreativeModeTab> tab) {
        return previewedTargets.contains(tab) ? previewed : null;
    }

    public static void onApplied(ResourceKey<CreativeModeTab> tab, CreativeTabModifier previous) {
        restore.put(tab, previous);
    }

    public static void clear() {
        previewed = null;
        previewedTargets = Set.of();
        restore.clear();
    }

    public static void pushPreview(@Nullable ResourceLocation fileId, @Nullable CreativeTabModifier mod) {
        Minecraft.getInstance().execute(() -> install(fileId, mod));
    }

    private static void install(@Nullable ResourceLocation fileId, @Nullable CreativeTabModifier mod) {
        //undo the attribute writes of the previous preview before installing the new one
        for (var e : restore.entrySet()) {
            e.getValue().applyAttributes(e.getKey());
        }
        restore.clear();

        previewed = mod;
        previewedTargets = mod == null ? Set.of() : resolveTargets(fileId, mod);
        for (var key : previewedTargets) {
            //a tab nothing modified yet has no event listener. preview needs one to reach it
            PlatStuff.addTabEventForTab(key);
        }
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

    public static void setPending(Set<ResourceLocation> ids) {
        pending.clear();
        pending.addAll(ids);
    }

    public static int pendingCount() {
        return pending.size();
    }

    public static boolean isPending(Item item) {
        return !pending.isEmpty() && pending.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public static void setEdited(@Nullable ResourceLocation fileId, @Nullable CreativeTabModifier mod) {
        edited = mod;
        editedTargets = mod == null ? Set.of()
                : resolveTargets(fileId, mod).stream()
                .map(ResourceKey::location)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    public static CreativeTabModifier tabBeingEdited() {
        return edited;
    }

    public static boolean targets(@Nullable ResourceLocation tabId) {
        return tabId != null && editedTargets.contains(tabId);
    }

    public static boolean targetsOpenTab() {
        return targets(openTab());
    }

    @Nullable
    public static ResourceLocation openTab() {
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
        return Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen
                ? CreativeModeInventoryScreen.selectedTab
                : null;
    }

    private static Set<ResourceKey<CreativeModeTab>> resolveTargets(@Nullable ResourceLocation fileId,
                                                                    CreativeTabModifier mod) {
        ResourceLocation id = fileId != null ? fileId : Polytone.res("editor_preview");
        Targets targets = mod.registerTab() ? Targets.ofIds(id) : mod.targets();
        //editor calls this on every keystroke, on files that are half written by definition.
        //compute would log an error for each one of those
        if (targets.entries().isEmpty() && !BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) return Set.of();

        Set<ResourceKey<CreativeModeTab>> set = new HashSet<>();
        for (var tab : targets.compute(id, BuiltInRegistries.CREATIVE_MODE_TAB.asLookup())) {
            set.add(tab.unwrapKey().get());
        }
        return set;
    }
}
