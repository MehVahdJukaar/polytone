package net.mehvahdjukaar.polytone.content.tabs;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.CsvUtils;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CreativeTabsModifiersManager extends ContentManager<CreativeTabModifier> {

    private Map<ResourceLocation, List<String>> extraTabs = Map.of();

    private final MapRegistry<CreativeModeTab> customTabs = new MapRegistry<>("Custom Creative Tabs");

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = new HashMap<>();
    private final Set<ResourceKey<CreativeModeTab>> needsRefresh = new HashSet<>();

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> vanillaTabs = new HashMap<>();

    public CreativeTabsModifiersManager() {
        super(Spec.of("Creative tab modifier", () -> CreativeTabModifier.CODEC)
                .wikiPage("Creative-Tab-Modifiers")
                .folders("creative_tab_modifiers"));
    }


    @Override
    public AssetsFiles prepare(ResourceManager resourceManager) {
        this.extraTabs = ImmutableMap.copyOf(CsvUtils.parseCsv(resourceManager, "creative_tabs"));
        return super.prepare(resourceManager);
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        CreativeTabPreview.clear();
        for (var id : customTabs.keySet()) {
            PlatStuff.unregisterDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, id);
            if (logOff) {
                Minecraft.getInstance().tell(PlatStuff::sortTabs);
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
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        for (var e : this.extraTabs.entrySet()) {
            for (var str : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(str);
                registerNewTab(id);
            }
        }
        for (var e : Parsed.batchParseOnlyEnabled(resources.jsons(), CreativeTabModifier.CODEC,
                ops, "creative tab modifier")) {
            ResourceLocation id = e.getKey();
            CreativeTabModifier mod = e.getValue();
            if (mod.registerTab()) {
                registerNewTab(id);
            }
            addModifier(e.getKey(), e.getValue());
        }


        if (!customTabs.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Creative Tabs from Resource Packs: {}", customTabs.size(), customTabs + ". Remember to add items to them!");
            Minecraft.getInstance().tell(PlatStuff::sortTabs);
        }

    }

    private void registerNewTab(ResourceLocation id) {
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
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {

        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keySet());
        }
        if (!needsRefresh.isEmpty()) {
            CreativeModeTabs.CACHED_PARAMETERS = null;
            //forces reload on next open screen
            needsRefresh.clear();
        }
    }

    private void addModifier(ResourceLocation fileId, CreativeTabModifier mod) {
        Targets targets = mod.targets();
        if (mod.registerTab()) {
            targets = Targets.ofIds(fileId);
        }
        for (var tab : targets.compute(fileId, BuiltInRegistries.CREATIVE_MODE_TAB.asLookup())) {
            ResourceKey<CreativeModeTab> key = tab.unwrapKey().get();
            modifiers.merge(key, mod, CreativeTabModifier::merge);
            PlatStuff.addTabEventForTab(key);
        }
    }

    public void modifyTab(ItemToTabEvent event) {
        var tab = event.getTab();
        CreativeTabModifier overriding = CreativeTabPreview.modifierFor(tab);
        var mod = overriding != null ? overriding : modifiers.get(tab);
        if (mod != null) {
            RegistryAccess access = PlatStuff.hackyGetRegistryAccess();
            if (access != null) {
                CreativeTabModifier v = mod.applyItemsAndAttributes(event, access);
                if (overriding != null) CreativeTabPreview.onApplied(tab, v);
                    //don't add custom tabs here!
                else if (!customTabs.containsKey(tab.location())) vanillaTabs.put(tab, v);
            }
        }
    }

    public boolean isDynamicTab(ResourceLocation entryId) {
        return customTabs.containsKey(entryId);
    }


}
