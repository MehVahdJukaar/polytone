package net.mehvahdjukaar.polytone.tabs;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.CsvUtils;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.*;

public class CreativeTabsModifiersManager extends PartialReloader<CreativeTabsModifiersManager.Resources> {

    private final MapRegistry<CreativeModeTab> customTabs = new MapRegistry<>("Custom Creative Tabs");

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

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(types));
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for (var id : customTabs.keySet()) {
            PlatStuff.unregisterDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, id);
            if (logOff) PlatStuff.sortTabs();
        }
        customTabs.clear();
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
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        for (var e : resources.extraTabs.entrySet()) {
            for (var s : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(s);
                ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
                if (!customTabs.containsKey(id) && !BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(key)) {
                    CreativeModeTab tab = PlatStuff.createCreativeTab(id);
                    customTabs.register(id, tab);
                } else {
                    Polytone.LOGGER.error("Creative Tab with id {} already exists! Ignoring.", id);
                }
            }
        }

        for (var e : customTabs.getEntries()) {
            PlatStuff.registerDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, e.getKey(), e.getValue());
        }

        for (var j : resources.tabsModifiers.entrySet()) {

            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            CreativeTabModifier modifier = CreativeTabModifier.CODEC.decode(ops, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.error("Could not decode Creative Mode Tab Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            addModifier(id, modifier);
        }

        if (!customTabs.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Creative Tabs from Resource Packs: {}", customTabs.size(), customTabs + ". Remember to add items to them!");
            PlatStuff.sortTabs();
        }
        //else apply as soon as we load a level
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keySet());
        }
        if (!needsRefresh.isEmpty()) {
            CreativeModeTabs.CACHED_PARAMETERS = null;
            //forces reload on next open screen
            needsRefresh.clear();
        }
        if (true) return;

        //not used. optimized code but can cause issues

        // we must rebuild everything because cache parameter could be from another world
        if (CreativeModeTabs.CACHED_PARAMETERS != null) {
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
        }

        if (!needsRefresh.isEmpty() || !customTabs.isEmpty()) {
            PlatStuff.sortTabs();
        }
        needsRefresh.clear();
    }

    private void addModifier(ResourceLocation fileId, CreativeTabModifier mod) {
        for (var tab : mod.targets().compute(fileId, BuiltInRegistries.CREATIVE_MODE_TAB)) {
            ResourceKey<CreativeModeTab> key = tab.unwrapKey().get();
            modifiers.merge(key, mod, CreativeTabModifier::merge);

            PlatStuff.addTabEventForTab(key);
        }
    }

    public void modifyTab(ItemToTabEvent event) {
        var tab = event.getTab();
        var mod = modifiers.get(tab);
        if (mod != null) {
            RegistryAccess access = PlatStuff.hackyGetRegistryAccess();
            if (access != null) {
                CreativeTabModifier v = mod.applyItemsAndAttributes(event, access);
                //dont add custom tabs here!
                if (!customTabs.containsKey(tab.location())) vanillaTabs.put(tab, v);
            }
        }
    }

    public record Resources(Map<ResourceLocation, JsonElement> tabsModifiers,
                            Map<ResourceLocation, List<String>> extraTabs) {
    }

}
