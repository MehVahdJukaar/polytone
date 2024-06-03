package net.mehvahdjukaar.polytone.forge;

import net.mehvahdjukaar.polytone.mixins.forge.BlockColorsAccessor;
import net.mehvahdjukaar.polytone.mixins.forge.CreativeTabAccessor;
import net.mehvahdjukaar.polytone.mixins.forge.ModifiableBiomeAccessor;
import net.mehvahdjukaar.polytone.mixins.forge.ModifiableBiomeInfoBiomeInfoAccessor;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.DimensionSpecialEffectsManager;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlatStuffImpl {

    public static boolean isModStateValid() {
        return !ModLoader.hasErrors();
    }

    public static void addClientReloadListener(Supplier<PreparableReloadListener> listener, ResourceLocation location) {
        Consumer<RegisterClientReloadListenersEvent> eventConsumer = (event) -> {
            event.registerReloadListener(listener.get());
        };
        FMLJavaModLoadingContext.get().getModEventBus().addListener(eventConsumer);
    }


    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        return ((BlockColorsAccessor) colors).getBlockColors().get(block);
    }


    public static String maybeRemapName(String s) {
        return s;
    }

    @org.jetbrains.annotations.Contract
    public static boolean isModLoaded(String namespace) {
        return ModList.get().isLoaded(namespace);
    }

    public static DimensionSpecialEffects getDimensionEffects(ResourceLocation id) {
        return DimensionSpecialEffectsManager.getForType(id);
    }

    public static void applyBiomeSurgery(Biome biome, BiomeSpecialEffects newEffects) {
        //forge original biome effect object is never user and redirected by coremod
        //we apply to the biome modifier. We dont want to change the original
        ModifiableBiomeInfo modifiable = biome.modifiableBiomeInfo();
        ModifiableBiomeInfo.BiomeInfo modifiedInfo = modifiable.getModifiedBiomeInfo();
        if (modifiedInfo == null) {
            modifiedInfo = ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(modifiable.getOriginalBiomeInfo()).build();
            //assign modified info
            ((ModifiableBiomeAccessor) modifiable).setModifiedBiomeInfo(modifiedInfo);
        }
        //assign new effects
        ((ModifiableBiomeInfoBiomeInfoAccessor) (Object) modifiedInfo).setEffects(newEffects);
    }

    public static void addTabEventForTab(ResourceKey<CreativeModeTab> key) {

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
        if (mod.search().isPresent()) {
            oldSearch = tab.hasSearchBar();
            acc.setHasSearchBar(mod.search().get());
        }

        Integer oldSearchWidth = null;
        if (mod.searchWidth().isPresent()) {
            oldSearchWidth = tab.getSearchBarWidth();
            acc.setSearchBarWidth(mod.searchWidth().get());
        }

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
        if (mod.tabsImage().isPresent()) {
            oldTabsImage = tab.getTabsImage();
            acc.setTabsImage(mod.tabsImage().get());
        }

        ResourceLocation oldBackgroundLocation = null;
        if (mod.backGroundLocation().isPresent()) {
            oldBackgroundLocation = tab.getBackgroundLocation();
            acc.setBackgroundLocation(mod.backGroundLocation().get());
        }


        List<ResourceLocation> oldBeforeTabs = null;
        if (!mod.beforeTabs().isEmpty()) {
            oldBeforeTabs = tab.tabsBefore;
            acc.setBeforeTabs(mod.beforeTabs());
        }

        List<ResourceLocation> oldAfterTabs = null;
        if (!mod.afterTabs().isEmpty()) {
            oldAfterTabs = tab.tabsAfter;
            acc.setAfterTabs(mod.afterTabs());
        }

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
                Set.of()
        );


    }

}
