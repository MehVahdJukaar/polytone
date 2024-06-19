package net.mehvahdjukaar.polytone.fabric;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.mixins.fabric.BlockColorsAccessor;
import net.mehvahdjukaar.polytone.mixins.fabric.CreativeTabAccessor;
import net.mehvahdjukaar.polytone.mixins.fabric.FabricItemGroupEntriesAccessor;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.tabs.ItemToTabEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PlatStuffImpl {
    public static boolean isModStateValid() {
        return true;
    }

    public static void addClientReloadListener(final Supplier<PreparableReloadListener> listener, final ResourceLocation name) {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            private final Supplier<PreparableReloadListener> inner = Suppliers.memoize(listener::get);

            public ResourceLocation getFabricId() {
                return name;
            }

            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return this.inner.get().reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
            }
        });
    }

    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        return ((BlockColorsAccessor) colors).getBlockColors().byId(BuiltInRegistries.BLOCK.getId(block));
    }

    public static SoundEvent registerSoundEvent(ResourceLocation id) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        ((MappedRegistry) BuiltInRegistries.SOUND_EVENT).frozen = false;
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
        BuiltInRegistries.SOUND_EVENT.freeze();

        return event;
    }

    public static String maybeRemapName(String s) {
        return FabricLoader.getInstance().getMappingResolver().mapClassName("official", s);

    }

    @org.jetbrains.annotations.Contract
    public static boolean isModLoaded(String namespace) {
        return FabricLoader.getInstance().isModLoaded(namespace);
    }

    public static DimensionSpecialEffects getDimensionEffects(ResourceLocation id) {
        return DimensionRenderingRegistry.getDimensionEffects(id);
    }

    public static void applyBiomeSurgery(Biome biome, BiomeSpecialEffects newEffects) {
        try {
            field.setAccessible(true);
            field.set(biome, newEffects);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Field field;

    static {
        for (var f : Biome.class.getDeclaredFields()) {
            if (f.getType() == BiomeSpecialEffects.class) {
                field = f;
                break;
            }
        }
    }

    private static final Set<ResourceKey<CreativeModeTab>> addedCallbacks = new HashSet<>();
    private static final ResourceLocation POLYTONE_PHASE = Polytone.res("modify_tabs");

    public static void addTabEventForTab(ResourceKey<CreativeModeTab> key) {
        if (!addedCallbacks.contains(key)) {
            addedCallbacks.add(key);
            Event<ItemGroupEvents.ModifyEntries> event = ItemGroupEvents.modifyEntriesEvent(key);
            event.addPhaseOrdering(Event.DEFAULT_PHASE, POLYTONE_PHASE);
            event.register(POLYTONE_PHASE, f ->
                    Polytone.CREATIVE_TABS_MODIFIERS.modifyTab(new ItemToTabEventImpl(key, f)));
        }
    }


    public static CreativeTabModifier modifyTab(CreativeTabModifier mod, CreativeModeTab tab) {
        CreativeTabAccessor acc = (CreativeTabAccessor) tab;
        Component oldName = null;
        if (mod.name().isPresent()) {
            oldName = tab.getDisplayName();
            acc.setDisplayName(mod.name().get());
        }

        ItemStack oldIcon = null;
        if (mod.icon().isPresent()) {
            oldIcon = tab.getIconItem();
            acc.setIcon(mod.icon().get());
        }

        Boolean oldSearch = null;
        Integer oldSearchWidth = null;
        /*
        if (mod.search().isPresent()) {
            oldSearch = tab.hasSearchBar();
            acc.setHasSearchBar(mod.search().get());
        }
        if (mod.searchWidth().isPresent()) {
            oldSearchWidth = tab.getSearchBarWidth();
            acc.setSearchBarWidth(mod.searchWidth().get());
        }*/

        Boolean oldCanScroll = null;
        if (mod.canScroll().isPresent()) {
            oldCanScroll = tab.canScroll();
            acc.setCanScroll(mod.canScroll().get());
        }

        Boolean oldShowTitle = null;
        if (mod.showTitle().isPresent()) {
            oldShowTitle = tab.showTitle();
            acc.setShowTitle(mod.showTitle().get());
        }

        ResourceLocation oldTabsImage = null;

        ResourceLocation oldBackgroundLocation = null;

        List<ResourceLocation> oldBeforeTabs = null;

        List<ResourceLocation> oldAfterTabs = null;

        return new CreativeTabModifier(
                Optional.ofNullable(oldIcon),
                Optional.ofNullable(oldSearch),
                Optional.ofNullable(oldSearchWidth),
                Optional.ofNullable(oldCanScroll),
                Optional.ofNullable(oldShowTitle),
                Optional.ofNullable(oldName),
                Optional.ofNullable(oldBackgroundLocation),
                Optional.ofNullable(oldTabsImage),
                Optional.ofNullable(oldBeforeTabs),
                Optional.ofNullable(oldAfterTabs),
                List.of(),
                List.of(),
                Set.of());
    }

    public static void sortTabs() {
        CreativeModeTabs.validate();
    }

    public static CreativeModeTab registerTab(ResourceLocation id) {
        CreativeModeTab tab = FabricItemGroup.builder().title(Component.literal(id.toString())).build();
        ((MappedRegistry) BuiltInRegistries.CREATIVE_MODE_TAB).frozen = false;
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        BuiltInRegistries.CREATIVE_MODE_TAB.freeze();
        return tab;
    }

    public static RegistryAccess hackyGetRegistryAccess() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT &&
                RenderSystem.isOnRenderThread()) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) return level.registryAccess();
        }
        return null;
    }

    public record ItemToTabEventImpl(ResourceKey<CreativeModeTab> tab,
                                     FabricItemGroupEntries entries) implements ItemToTabEvent {

        @Override
        public ResourceKey<CreativeModeTab> getTab() {
            return tab;
        }

        @Override
        public void addItems(@Nullable Predicate<ItemStack> target, boolean after, List<ItemStack> items) {
            if (target == null) {
                entries.acceptAll(items);
            } else {
                if (after) {
                    entries.addAfter(target, items, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                } else {
                    entries.addBefore(target, items, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
        }

        @Override
        public void removeItems(Predicate<ItemStack> target) {
            FabricItemGroupEntriesAccessor acc = ((FabricItemGroupEntriesAccessor) entries);
            acc.getDisplayStacks().removeIf(target);
            acc.getSearchTabStacks().removeIf(target);
        }

    }
}
