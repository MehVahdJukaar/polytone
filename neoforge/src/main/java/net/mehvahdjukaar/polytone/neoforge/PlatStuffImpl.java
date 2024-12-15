package net.mehvahdjukaar.polytone.neoforge;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.mixins.neoforge.*;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
import net.neoforged.neoforge.client.DimensionSpecialEffectsManager;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.List;
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
        PolytoneForge.bus.addListener(eventConsumer);
    }

    public static ParticleProvider<?> getParticleProvider(ParticleType<?> type) {
        return ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine)
                .getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getKey(type));
    }

    public static void setParticleProvider(ParticleType<?> type, ParticleProvider<?> provider) {
        ParticleEngineAccessor engine = (ParticleEngineAccessor) Minecraft.getInstance().particleEngine;
        engine.getProviders().put(BuiltInRegistries.PARTICLE_TYPE.getKey(type), provider);
    }

    public static void unregisterParticleProvider(ResourceLocation id) {
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
        ((ParticleEngineAccessor) particleEngine).getProviders().remove(id);
    }

    public static SimpleParticleType makeParticleType(boolean forceSpawn) {
        return new SimpleParticleType(forceSpawn);
    }

    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        return ((BlockColorsAccessor) colors).getBlockColors().get(block);
    }

    public static ItemColor getItemColor(ItemColors colors, Item item) {
        return ((ItemColorsAccessor) colors).getItemColors().get(item);
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

    private static Field VANILLA_TABS = null;

    public static void sortTabs() {
        //needs to clear vanilla tabs cause neo is stupid
        if (VANILLA_TABS == null) {
            VANILLA_TABS = ObfuscationReflectionHelper.findField(CreativeModeTabRegistry.class, "DEFAULT_TABS");
        }
        try {
            ((List) VANILLA_TABS.get(null)).clear();
            CreativeModeTabRegistry.sortTabs();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void updateSearchTrees(SessionSearchTrees searchTrees, List<CreativeModeTab> needsTreeUpdated) {
        needsTreeUpdated.forEach((tab) -> {
            List<ItemStack> list = List.copyOf(tab.getDisplayItems());
            searchTrees.updateCreativeTags(list, CreativeModeTabSearchRegistry.getTagSearchKey(tab));
        });
    }

    public static RegistryAccess hackyGetRegistryAccess() {
        if (FMLEnvironment.dist == Dist.CLIENT &&
                RenderSystem.isOnRenderThread()) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) return level.registryAccess();
        }
        return null;
    }


    public static CreativeModeTab createCreativeTab(ResourceLocation id) {
        return CreativeModeTab.builder().title(Component.translatable(id.toString())).build();
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
            oldBackgroundLocation = tab.getBackgroundTexture();
            acc.setBackgroundTexture(mod.backGroundLocation().get());
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

    public static RenderType getRenderType(Block block) {
        return null;
    }

    public static void setRenderType(Block block, RenderType renderType) {
    }

    private static final boolean AC = ModList.get().isLoaded("alexscaves");


    public static void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float skyLight, float flicker, int torchX,
                                            int skyY, Vector3f combined) {
        //INSERTION BY AC...
        if (AC) AlexsCavesCompat.applyACLightingColors(level, combined, partialTicks);

        //removed in 1.20.2
        //level.effects().adjustLightmapColors(level, partialTicks, skyDarken, skyLight, flicker, torchX, skyY, combined);
    }


    public static float compatACModifyGamma(float partialTicks, float gamma) {
        return AC ? AlexsCavesCompat.modifyGamma(partialTicks, gamma) : gamma;
    }


    public static RegistryAccess getServerRegistryAccess() {
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }

    public static BakedModel getBakedModel(ModelResourceLocation id) {
        ModelManager mm = Minecraft.getInstance().getModelManager();
        return mm.getModel(id);
    }

    public static void addSpecialModelRegistration(Consumer<PlatStuff.SpecialModelEvent> eventListener) {
        Consumer<ModelEvent.RegisterAdditional> eventConsumer = event -> {
            eventListener.accept(event::register);
        };
        PolytoneForge.bus.addListener(eventConsumer);
    }

}
