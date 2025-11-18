package net.mehvahdjukaar.polytone.neoforge;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.mixins.neoforge.*;
import net.mehvahdjukaar.polytone.particle.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.ColorResolverManager;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
import net.neoforged.neoforge.client.DimensionSpecialEffectsManager;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlatStuffImpl {

    public static boolean isModStateValid() {
        return !ModLoader.hasErrors();
    }

    public static void addClientReloadListener(Supplier<PreparableReloadListener> listener, ResourceLocation location) {
        Consumer<AddClientReloadListenersEvent> eventConsumer = (event) -> {
            event.addListener(location, listener.get());
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

    public static ParticleType<ExtraDataParticleOptions> makeParticleType(boolean forceSpawn) {
        AtomicReference<ParticleType<ExtraDataParticleOptions>> ref = new AtomicReference<>();
        ParticleType<ExtraDataParticleOptions> instance = new ParticleType<>(forceSpawn) {

            @Override
            public MapCodec<ExtraDataParticleOptions> codec() {
                return ExtraDataParticleOptions.codec(ref::get);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ExtraDataParticleOptions> streamCodec() {
                return ExtraDataParticleOptions.streamCodec(ref::get);
            }
        };
        ref.set(instance);
        return instance;
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
        if (FMLEnvironment.getDist() == Dist.CLIENT &&
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
                Targets.EMPTY
        );


    }

    public static ChunkSectionLayer getRenderType(Block block) {
        return null;
    }

    public static void setRenderType(Block block, ChunkSectionLayer renderType) {
        ItemBlockRenderTypes.setRenderLayer(block, renderType);
    }

    private static final boolean AC = ModList.get().isLoaded("alexscaves");


    public static void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float skyLight, float flicker, int torchX,
                                            int skyY, Vector3f combined) {
        //INSERTION BY AC...
   //     if (AC) AlexsCavesCompat.applyACLightingColors(level, combined, partialTicks);
        //TODO: add back
        //removed in 1.20.2
        //level.effects().adjustLightmapColors(level, partialTicks, skyDarken, skyLight, flicker, torchX, skyY, combined);
    }


    public static float compatACModifyGamma(float partialTicks, float gamma) {
       return gamma; //TODO: add back
        // return AC ? AlexsCavesCompat.modifyGamma(partialTicks, gamma) : gamma;
    }


    public static RegistryAccess getServerRegistryAccess() {
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }

    public static String getVersion() {
        return ModList.get().getModContainerById(Polytone.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("unknown");
    }

    public static void unregisterAllCustomColorResolves() {
        for (ColorResolver resolver : MY_CUSTOM_RESOLVERS) {
            try {
                ImmutableList<ColorResolver> resolvers = (ImmutableList<ColorResolver>) COLOR_RESOLVERS.get(null);
                List<ColorResolver> temp = new ArrayList<>(resolvers);
                temp.remove(resolver);
                ImmutableList<ColorResolver> newList = ImmutableList.copyOf(temp);
                COLOR_RESOLVERS.set(null, newList);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        MY_CUSTOM_RESOLVERS.clear();
    }

    public static void registerColorResolver(ColorResolver colorResolver) {
        MY_CUSTOM_RESOLVERS.add(colorResolver);
        try {
            ImmutableList<ColorResolver> resolvers = (ImmutableList<ColorResolver>) COLOR_RESOLVERS.get(null);
            ImmutableList<ColorResolver> newList = ImmutableList.<ColorResolver>builder()
                    .addAll(resolvers).add(colorResolver).build();
            COLOR_RESOLVERS.set(null, newList);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void doAddModels() {
    }

    private static final Set<ColorResolver> MY_CUSTOM_RESOLVERS = new HashSet<>();
    private static final Field COLOR_RESOLVERS;

    static {
        Field found = null;
        var fields = ColorResolverManager.class.getDeclaredFields();
        for (var f : fields) {
            if (f.getType() == ImmutableList.class) {
                f.setAccessible(true);
                found = f;
                break;
            }
        }
        COLOR_RESOLVERS = found;
    }
}
