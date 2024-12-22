package net.mehvahdjukaar.polytone.forge;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.systems.RenderSystem;
import cpw.mods.modlauncher.api.INameMappingService;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.mixins.forge.*;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.DimensionSpecialEffectsManager;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;
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
        try {
            return ((BlockColorsAccessor) colors).getBlockColors()
                    .get(ForgeRegistries.BLOCKS.getDelegateOrThrow(block));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ItemColor getItemColor(ItemColors colors, Item block) {
        try {
            return ((ItemColorsAccessor) colors).getItemColors()
                    .get(ForgeRegistries.ITEMS.getDelegateOrThrow(block));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static <T extends ParticleType<?>> T  registerParticleType(ResourceLocation id, T sound) {
        ForgeRegistry<ParticleType<?>> reg = (ForgeRegistry<ParticleType<?>>) ForgeRegistries.PARTICLE_TYPES;
        registerDyn(id, sound, reg);
        return sound;
    }

    public static <T extends SoundEvent>  T registerSoundEvent(ResourceLocation id, T sound) {
        ForgeRegistry<SoundEvent> reg = (ForgeRegistry<SoundEvent>) ForgeRegistries.SOUND_EVENTS;
        registerDyn(id, sound, reg);
        return sound;
    }

    private static <T> void registerDyn(ResourceLocation id, T sound, ForgeRegistry<T> reg) {
        boolean wasLocked = reg.isLocked();
        if (wasLocked) reg.unfreeze();
        reg.register(id, sound);
        if (wasLocked) reg.freeze();
    }


    public static void unregisterParticleType(ResourceLocation id) {
        unregisterDyn(id, ForgeRegistries.PARTICLE_TYPES);
    }

    public static void unregisterSoundEvent(ResourceLocation id) {
        unregisterDyn(id, ForgeRegistries.SOUND_EVENTS);
    }

    private static void unregisterDyn(ResourceLocation id, IForgeRegistry<?> registry) {
        //No op. too bad. forge registry shit is too complicated
        try {
            remove((ForgeRegistry) registry, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Field NAMES;
    private static final Field IDS;
    private static final Field AVAILABILITYMAP;
    private static final Field KEYS;
    private static final Field OWNERS;
    private static final Field HASWRAPPER;
    private static final Field OVERRIDES;
    static{
        try {
            NAMES = ForgeRegistry.class.getDeclaredField("names");
            IDS = ForgeRegistry.class.getDeclaredField("ids");
            AVAILABILITYMAP = ForgeRegistry.class.getDeclaredField("availabilityMap");
            KEYS = ForgeRegistry.class.getDeclaredField("keys");
            OWNERS = ForgeRegistry.class.getDeclaredField("owners");
            HASWRAPPER = ForgeRegistry.class.getDeclaredField("hasWrapper");
            OVERRIDES = ForgeRegistry.class.getDeclaredField("overrides");
            NAMES.setAccessible(true);
            IDS.setAccessible(true);
            AVAILABILITYMAP.setAccessible(true);
            KEYS.setAccessible(true);
            OWNERS.setAccessible(true);
            HASWRAPPER.setAccessible(true);
            OVERRIDES.setAccessible(true);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }

    static <V> int remove(ForgeRegistry<V> registry, ResourceLocation key) throws Exception {

        // Check if the key exists in the registry
        V value = ((BiMap<ResourceLocation, V>)NAMES.get(registry)).remove(key);
        if (value == null) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "The name %s is not registered in the registry.", key));
        }

        // Get the associated ID and remove it
        int idToRemove = registry.getID(value);
        ((BiMap<Integer, V>)IDS.get(registry)).remove(idToRemove);
        ((BitSet)AVAILABILITYMAP.get(registry)).clear(idToRemove);

        // Remove from keys map
        ResourceKey<V> rkey = ResourceKey.create(registry.getRegistryKey(), key);
        ((BiMap<ResourceKey, V>)KEYS.get(registry)).remove(rkey);

        // Remove from owners
        ((BiMap<?, V>) OWNERS.get(registry)).inverse().remove(value);

        // Remove from overrides if applicable
        if ((Boolean) HASWRAPPER.get(registry)) {
            var overrides = ((Multimap<ResourceLocation, V>)OVERRIDES.get(registry));
            if (overrides.containsKey(key)) {
                overrides.get(key).add(value);
                if (overrides.get(key).isEmpty()) {
                    overrides.remove(key, value);
                }
            }
        }

        return idToRemove;
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
                Targets.EMPTY
        );


    }

    public static RenderType getRenderType(Block block) {
        return null;
    }

    public static void setRenderType(Block block, RenderType renderType) {
    }

    private static final boolean AC = ModList.get().isLoaded("alexscaves");


    public static void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float skyLight, float flicker, int torchX, int skyY, Vector3f combined) {
        //INSERTION BY AC...
        if (AC) AlexsCavesCompat.applyACLightingColors(level, combined);

        level.effects().adjustLightmapColors(level, partialTicks, skyDarken, skyLight, flicker, torchX, skyY, combined);
    }


    public static float compatACModifyGamma(float partialTicks, float gamma) {
        return AC ? AlexsCavesCompat.modifyGamma(partialTicks, gamma) : gamma;
    }

    public static ParticleProvider<?> getParticleProvider(ParticleType<?> type) {
        return ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).getProviders()
                .get(BuiltInRegistries.PARTICLE_TYPE.getKey(type));
    }


    public static RegistryAccess getServerRegistryAccess() {
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }

    public static void addSpecialModelRegistration(Consumer<PlatStuff.SpecialModelEvent> eventListener) {
        Consumer<ModelEvent.RegisterAdditional> eventConsumer = event -> {
            eventListener.accept(new PlatStuff.SpecialModelEvent() {
                @Override
                public void register(ModelResourceLocation modelLocation) {
                    event.register(modelLocation);
                }

                @Override
                public void register(ResourceLocation id) {
                    event.register((id));
                }
            });
        };
        FMLJavaModLoadingContext.get().getModEventBus().addListener(eventConsumer);
    }

    public static BakedModel getModel(ResourceLocation modelLocation) {
        return Minecraft.getInstance().getModelManager().getModel(modelLocation);
    }



}
