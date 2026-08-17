package net.mehvahdjukaar.polytone.platform;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.mixins.neoforge.*;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionBakedModel;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionModel;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.mehvahdjukaar.polytone.mixins.neoforge.BlockColorsAccessor;
import net.mehvahdjukaar.polytone.mixins.neoforge.CreativeTabAccessor;
import net.mehvahdjukaar.polytone.mixins.neoforge.ModifiableBiomeAccessor;
import net.mehvahdjukaar.polytone.mixins.neoforge.ModifiableBiomeInfoBiomeInfoAccessor;
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
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.ColorResolverManager;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
import net.neoforged.neoforge.client.DimensionSpecialEffectsManager;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;
import sereneseasons.api.season.SeasonHelper;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlatStuffImpl {

    // Cached reflective handle to CreativeModeTabRegistry.DEFAULT_TABS; resolved lazily on first re-sort.
    private static Field VANILLA_TABS = null;

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

    public static ParticleType<ParticleOptions> makeParticleType(boolean forceSpawn) {
        AtomicReference<ParticleType<ExtraDataParticleOptions>> ref = new AtomicReference<>();
        ParticleType<ParticleOptions> instance = new ParticleType<>(forceSpawn) {

            @Override
            public MapCodec<ParticleOptions> codec() {
                return (MapCodec) ExtraDataParticleOptions.codec(ref::get);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ParticleOptions> streamCodec() {
                return (StreamCodec) ExtraDataParticleOptions.streamCodec(ref::get);
            }
        };
        ref.set((ParticleType) instance);
        return instance;
    }

    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        return ((BlockColorsAccessor) colors).getBlockColors().get(block);
    }

    public static ItemColor getItemColor(ItemColors colors, Item item) {
        return ((ItemColorsAccessor) colors).getItemColors().get(item);
    }

    public static BakedModel makeExpressionModel(ExpressionModel.Selector selector) {
        return new ExpressionBakedModel(selector);
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
        //we apply to the biome modifier. We don't want to change the original
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

    public static void sortTabs() {
        // NeoForge sortTabs() appends to DEFAULT_TABS without clearing it first, so we must reset
        // before re-sorting when dynamic tabs are registered. Guard against re-sorting when the
        // registry doesn't yet contain all vanilla category tabs (would IndexOutOfBounds inside NeoForge).
        int categoryTabs = 0;
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (tab.getType() == CreativeModeTab.Type.CATEGORY) {
                categoryTabs++;
            }
        }
        if (categoryTabs < 10) {
            Polytone.LOGGER.warn("Skipping creative tab re-sort: expected at least 10 vanilla category tabs, found {}", categoryTabs);
            return;
        }
        if (VANILLA_TABS == null) {
            VANILLA_TABS = ObfuscationReflectionHelper.findField(CreativeModeTabRegistry.class, "DEFAULT_TABS");
        }
        try {
            ((List) VANILLA_TABS.get(null)).clear();
            CreativeModeTabRegistry.sortTabs();
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to re-sort creative tabs", e);
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


    public static void addTabEventForTab(ResourceKey<CreativeModeTab> key) {
        // No-op on NeoForge: a single BuildCreativeModeTabContentsEvent listener registered in
        // PolytoneForge already fires for every tab, so we don't need per-tab registration.
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
                false,
                Targets.EMPTY
        );


    }

    public static Object getRenderType(Block block) {
        return ItemBlockRenderTypes.getRenderLayers(block.defaultBlockState());
    }

    public static boolean dontCheckLoading = false;

    public static void setRenderType(Block block, Object renderType) {
        dontCheckLoading = true;
        if (renderType instanceof RenderType rt) {
            ItemBlockRenderTypes.setRenderLayer(block, rt);
        }else if (renderType instanceof ChunkRenderTypeSet st) {
            ItemBlockRenderTypes.setRenderLayer(block, st);
        }
        dontCheckLoading= false;
    }

    private static final boolean AC = ModList.get().isLoaded("alexscaves");


    public static void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float skyLight, float flicker, int torchX,
                                            int skyY, Vector3f combined) {
        //INSERTION BY AC...
        if (AC) AlexsCavesCompat.applyACLightingColors(level, combined, partialTicks);

        level.effects().adjustLightmapColors(level, partialTicks, skyDarken, skyLight, flicker, torchX, skyY, combined);
    }


    public static float compatACModifyGamma(float partialTicks, float gamma) {
        return AC ? AlexsCavesCompat.modifyGamma(partialTicks, gamma) : gamma;
    }


    private static final boolean SS = ModList.get().isLoaded("sereneseasons");

    public static float compatSSGetSeason(Level level) {
        return SS ? (SeasonHelper.getSeasonState(level).getSubSeason().ordinal() / 11f) : 1f;
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

    public static void registerShaders(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> shaderConsumer) {

        Consumer<RegisterShadersEvent> eventConsumer = event -> {
            try {
                ShaderInstance shader = new ShaderInstance(event.getResourceProvider(), id, format);
                event.registerShader(shader, shaderConsumer);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse shader: " + id, e);
            }
        };
        PolytoneForge.bus.addListener(eventConsumer);
    }

    public static void doAddModels() {
    }

    public static <T> Iterable<Holder<T>> getTagEntries(HolderLookup.RegistryLookup<T> reg, TagKey<T> tag) {
        return ClientTagsImpl.getTagEntries(reg, tag);
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

    public static void matchStencil(RenderTarget main, RenderTarget snapshot) {
        if (main.isStencilEnabled() && !snapshot.isStencilEnabled()) {
            snapshot.enableStencil(); // reallocates the snapshot's attachment to match main's depth+stencil format
        }
    }
}
