package net.mehvahdjukaar.polytone.tabs;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.CsvUtils;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.*;

public class CreativeTabsModifiersManager extends PartialReloader<CreativeTabsModifiersManager.Resources> {

    private final Set<ResourceKey<CreativeModeTab>> customTabs = new HashSet<>();

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = new HashMap<>();
    private final Set<ResourceKey<CreativeModeTab>> needsRefresh = new HashSet<>();

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> vanillaTabs = new HashMap<>();


    public CreativeTabsModifiersManager() {
        super("creative_tab_modifiers");
    }


    @Override
    public Resources prepare(ResourceManager resourceManager) {
        var jsons = getJsonsInDirectories(resourceManager);
        this.checkConditions(jsons);

        var types = CsvUtils.parseCsv(resourceManager, "creative_tabs");

        return new Resources(jsons, types);
    }

    @Override
    protected void reset() {
        for (var e : vanillaTabs.entrySet()) {
            e.getValue().applyAttributes(e.getKey());
        }
        vanillaTabs.clear();
        needsRefresh.addAll(modifiers.keySet());
        modifiers.clear();
        //unregister here todo
        customTabs.clear();
    }

    @Override
    protected void process(Resources resources) {
        for (var e : resources.extraTabs.entrySet()) {
            for (var s : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(s);
                ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
                if (!customTabs.contains(key) && !BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(key)) {
                    CreativeModeTab tab = PlatStuff.registerTab(id);
                    customTabs.add(key);
                } else {
                    Polytone.LOGGER.error("Creative Tab with id {} already exists! Ignoring.", id);
                }
            }
        }

        if (!customTabs.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Creative Tabs from Resource Packs: {}", customTabs.size(), customTabs + ". Remember to add them to sounds.json!");
        }

        var jsons = resources.tabsModifiers;
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            CreativeTabModifier modifier = CreativeTabModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Creative Mode Tab Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            addModifier(id, modifier);
        }
        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keySet());
        }
    }


    @Override
    protected void apply() {
        if (!needsRefresh.isEmpty() && CreativeModeTabs.CACHED_PARAMETERS != null) {
            //this only happens if they have already been built

            //same as rebuild content. Internally fires the events. Just rebuilds whats needed (old+new)
            for (var key : needsRefresh) {
                CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
                if (tab != null) {
                    tab.buildContents(CreativeModeTabs.CACHED_PARAMETERS);
                    CreativeTabModifier mod = modifiers.get(key);
                    if (mod != null && mod.search().orElse(false)) {
                        tab.rebuildSearchTree();
                    }
                }
            }

            PlatStuff.sortTabs();
        }
        needsRefresh.clear();
    }


    private void addModifier(ResourceLocation fileId, CreativeTabModifier mod) {
        for (ResourceLocation id : mod.getTargetsKeys(fileId)) {
            ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
            modifiers.merge(key, mod, CreativeTabModifier::merge);

            PlatStuff.addTabEventForTab(key);
        }
    }

    public void modifyTab(ItemToTabEvent event) {
        var tab = event.getTab();
        var mod = modifiers.get(tab);
        if (mod != null) {
            vanillaTabs.put(tab, mod.applyItemsAndAttributes(event));
        }
    }

    public record Resources(Map<ResourceLocation, JsonElement> tabsModifiers,
                            Map<ResourceLocation, List<String>> extraTabs) {
    }

}
