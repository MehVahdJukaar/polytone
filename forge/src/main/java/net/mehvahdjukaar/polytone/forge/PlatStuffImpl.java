package net.mehvahdjukaar.polytone.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import cpw.mods.modlauncher.api.INameMappingService;
import net.mehvahdjukaar.polytone.mixins.forge.CreativeTabAccessor;
import net.mehvahdjukaar.polytone.mixins.forge.ModifiableBiomeAccessor;
import net.mehvahdjukaar.polytone.mixins.forge.ModifiableBiomeInfoBiomeInfoAccessor;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.DimensionSpecialEffectsManager;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlatStuffImpl {

    public static boolean isModStateValid() {
        return ModLoader.isLoadingStateValid();
    }

    public static void addClientReloadListener(Supplier<PreparableReloadListener> listener, ResourceLocation location) {
        Consumer<RegisterClientReloadListenersEvent> eventConsumer = (event) -> {
            event.registerReloadListener(listener.get());
        };
        FMLJavaModLoadingContext.get().getModEventBus().addListener(eventConsumer);
    }

    private static final Field f;

    static {
        try {
            f = BlockColors.class.getDeclaredField("f_92571_");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        try {
            return ((Map<Holder.Reference<Block>, BlockColor>) f.get(colors)).get(ForgeRegistries.BLOCKS.getDelegateOrThrow(block));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SoundEvent registerSoundEvent(ResourceLocation id) {
        SoundEvent variableRangeEvent = SoundEvent.createVariableRangeEvent(id);
        ForgeRegistry<SoundEvent> reg = (ForgeRegistry<SoundEvent>) ForgeRegistries.SOUND_EVENTS;
        boolean wasLocked = reg.isLocked();
        if (wasLocked) reg.unfreeze();
        ForgeRegistries.SOUND_EVENTS.register(id, variableRangeEvent);
        if (wasLocked) reg.freeze();
        return variableRangeEvent;
    }

    public static String maybeRemapName(String s) {
        return ObfuscationReflectionHelper.remapName(INameMappingService.Domain.CLASS, s);
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

    public static void sortTabs() {
        CreativeModeTabRegistry.sortTabs();
    }

    public static RegistryAccess hackyGetRegistryAccess() {
        if (FMLEnvironment.dist ==  Dist.CLIENT &&
                RenderSystem.isOnRenderThread()) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) return level.registryAccess();
        }
        return null;
    }


    public static CreativeModeTab registerTab(ResourceLocation id) {
        CreativeModeTab tab = CreativeModeTab.builder().title(Component.translatable(id.toString())).build();
        ((MappedRegistry) BuiltInRegistries.CREATIVE_MODE_TAB).frozen = false;
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        BuiltInRegistries.CREATIVE_MODE_TAB.freeze();
        return tab;
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

        if (mod.search().isPresent()) {
            oldSearch = tab.hasSearchBar();
            acc.setHasSearchBar(mod.search().get());
        }
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
        if (mod.beforeTabs().isPresent()) {
            oldBeforeTabs = tab.tabsBefore;
            acc.setBeforeTabs(mod.beforeTabs().get());
        }

        List<ResourceLocation> oldAfterTabs = null;
        if (mod.afterTabs().isPresent()) {
            oldAfterTabs = tab.tabsAfter;
            acc.setAfterTabs(mod.afterTabs().get());
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
