package net.mehvahdjukaar.polytone.content.tabs;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.CsvUtils;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CreativeTabsModifiersManager extends ContentManager<CreativeTabModifier> {

    // creative_tabs.csv sidecar - new creative-tab ids to register, keyed by pack namespace
    private Map<Identifier, List<String>> extraTabs = Map.of();

    private final MapRegistry<CreativeModeTab> customTabs = new MapRegistry<>("Custom Creative Tabs");

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = new HashMap<>();
    private final Set<ResourceKey<CreativeModeTab>> needsRefresh = new HashSet<>();

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> vanillaTabs = new HashMap<>();

    @Nullable
    private ModifierOverride override;

    public CreativeTabsModifiersManager() {
        super(Spec.of("Creative tab modifier", () -> SchemaCodec.wrap(CreativeTabModifier.CODEC))
                .wikiPage("Creative-Tab-Modifiers")
                .folders("creative_tab_modifiers"));
    }

    @Override
    public AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        this.extraTabs = ImmutableMap.copyOf(CsvUtils.parseCsv(resourceManager, "creative_tabs"));
        return super.prepare(sharedState);
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        // A reload rebuilds every tab from the packs, so no override can survive it.
        override = null;
        for (var id : customTabs.keySet()) {
            PlatStuff.unregisterDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, id);
            if (logOff) {
                Minecraft.getInstance().schedule(PlatStuff::sortTabs);
            }
        }
        customTabs.clear();
        for (var e : vanillaTabs.entrySet()) {
            e.getValue().applyAttributes(e.getKey());
        }
        vanillaTabs.clear();
        needsRefresh.addAll(modifiers.keySet());
        modifiers.clear();
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var e : this.extraTabs.entrySet()) {
            for (var str : e.getValue()) {
                Identifier id = e.getKey().withPath(str);
                registerNewTab(id);
            }
        }
        for (var e : parseEnabledJsons(resources.jsons(), ops)) {
            Identifier id = e.getKey();
            CreativeTabModifier mod = e.getValue();
            if (mod.registerTab()) {
                registerNewTab(id);
            }
            addModifier(e.getKey(), e.getValue());
        }
        if (!customTabs.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Creative Tabs from Resource Packs: {}", customTabs.size(), customTabs + ". Remember to add items to them!");
            Minecraft.getInstance().schedule(PlatStuff::sortTabs);
        }
    }

    private void registerNewTab(Identifier id) {
        ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
        if (!customTabs.containsKey(id) && !BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(key)) {
            CreativeModeTab tab = PlatStuff.createCreativeTab(id);
            customTabs.register(id, tab);
            PlatStuff.registerDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        } else {
            Polytone.LOGGER.error("Creative Tab with id {} already exists! Ignoring.", id);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keySet());
        }
        if (!needsRefresh.isEmpty()) {
            CreativeModeTabs.CACHED_PARAMETERS = null;
            //forces reload on next open screen
            needsRefresh.clear();
        }
    }

    private void addModifier(Identifier fileId, CreativeTabModifier mod) {
        Targets targets = mod.targets();
        if (mod.registerTab()) {
            targets = Targets.ofIds(fileId);
        }
        for (var tab : targets.compute(fileId, BuiltInRegistries.CREATIVE_MODE_TAB)) {
            ResourceKey<CreativeModeTab> key = tab.unwrapKey().get();
            modifiers.merge(key, mod, CreativeTabModifier::merge);

            PlatStuff.addTabEventForTab(key);
        }
    }

    public void modifyTab(ItemToTabEvent event) {
        var tab = event.getTab();
        CreativeTabModifier overriding = override == null ? null : override.modifierFor(tab);
        var mod = overriding != null ? overriding : modifiers.get(tab);
        if (mod != null) {
            RegistryAccess access = PlatStuff.hackyGetRegistryAccess();
            if (access != null) {
                CreativeTabModifier v = mod.applyItemsAndAttributes(event, access);
                if (overriding != null) override.onApplied(tab, v);
                //don't add custom tabs here!
                else if (!customTabs.containsKey(tab.identifier())) vanillaTabs.put(tab, v);
            }
        }
    }

    // Stands in for the loaded modifier on the tabs it covers, so the editor can try an unsaved file
    // without a reload. Replaces rather than stacks: the edited file is already part of the loaded
    // merge, so applying both would double its item additions. The previous state comes back through
    // onApplied, so whoever installed the override can put the tab back.
    public interface ModifierOverride {

        @Nullable
        CreativeTabModifier modifierFor(ResourceKey<CreativeModeTab> tab);

        void onApplied(ResourceKey<CreativeModeTab> tab, CreativeTabModifier previous);
    }

    public void setOverride(@Nullable ModifierOverride override) {
        this.override = override;
    }

    public boolean isDynamicTab(Identifier entryId) {
        return customTabs.containsKey(entryId);
    }

}
