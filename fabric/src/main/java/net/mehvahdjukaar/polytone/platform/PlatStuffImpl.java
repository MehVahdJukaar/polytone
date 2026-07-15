package net.mehvahdjukaar.polytone.platform;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorResolverRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.impl.client.rendering.ColorResolverRegistryImpl;
import net.fabricmc.fabric.impl.tag.client.ClientTagsImpl;
import net.fabricmc.fabric.impl.tag.client.ClientTagsLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.mixins.fabric.*;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.content.tabs.ItemToTabEvent;
import net.mehvahdjukaar.polytone.utils.Targets;
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
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import sereneseasons.api.season.SeasonHelper;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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

    @org.jetbrains.annotations.Contract
    public static ItemColor getItemColor(ItemColors colors, Item item) {
        return ((ItemColorsAccessor) colors).getItemColors().byId(BuiltInRegistries.ITEM.getId(item));
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
            var event = ItemGroupEvents.MODIFY_ENTRIES_ALL;
            event.addPhaseOrdering(Event.DEFAULT_PHASE, POLYTONE_PHASE);
            event.register(POLYTONE_PHASE, (tab, entries) -> {
                Polytone.CREATIVE_TABS_MODIFIERS.modifyTab(new ItemToTabEventImpl(
                        BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).get(), entries));
            });

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
                false,
                Targets.EMPTY
        );

    }

    public static void sortTabs() {
        CreativeModeTabs.validate();
    }

    public static CreativeModeTab createCreativeTab(ResourceLocation id) {
        return FabricItemGroup.builder().title(Component.literal(id.toString())).build();
    }

    public static RegistryAccess hackyGetRegistryAccess() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT &&
                RenderSystem.isOnRenderThread()) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) return level.registryAccess();
        }
        return null;
    }

    public static void updateSearchTrees(SessionSearchTrees sessionSearchTrees, List<CreativeModeTab> needsTreeUpdated) {
        List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
        sessionSearchTrees.updateCreativeTags(list);
    }


    public static Object getRenderType(Block block) {
        return ItemBlockRenderTypes.getChunkRenderType(block.defaultBlockState());
    }

    public static void setRenderType(Block block, Object renderType) {
        if (renderType instanceof RenderType rt)
          ItemBlockRenderTypeAccessor.getTypeByBlock().put(block, rt);
    }

    public static void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float skyLight, float flicker, int torchX, int skyY, Vector3f combined) {
    }

    public static float compatACModifyGamma(float partialTicks, float gamma) {
        return gamma;
    }

    private static final boolean SS = FabricLoader.getInstance().isModLoaded("sereneseasons");

    public static float compatSSGetSeason(Level level) {
        return SS ? (SeasonHelper.getSeasonState(level).getSubSeason().ordinal() / 11f) : 1f;
    }

    public static ParticleProvider<?> getParticleProvider(ParticleType<?> type) {
        return ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine)
                .getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getId(type));
    }

    public static void setParticleProvider(ParticleType<?> type, ParticleProvider<?> provider) {
        ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine)
                .getProviders().put(BuiltInRegistries.PARTICLE_TYPE.getId(type), provider);
    }

    public static ParticleType<ParticleOptions> makeParticleType(boolean forceSpawn) {
        AtomicReference<ParticleType<ExtraDataParticleOptions>> ref = new AtomicReference<>();
        MapCodec<ParticleOptions> codec = (MapCodec) ExtraDataParticleOptions.codec(ref::get);
        StreamCodec<RegistryFriendlyByteBuf, ParticleOptions> streamCodec = (StreamCodec) ExtraDataParticleOptions.streamCodec(ref::get);
        ParticleType<ParticleOptions> instance = FabricParticleTypes.complex(forceSpawn, codec, streamCodec);
        ref.set((ParticleType) instance);
        return instance;
    }

    public static void unregisterParticleProvider(ResourceLocation id) {
        var type = BuiltInRegistries.PARTICLE_TYPE.get(id);
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
        ((ParticleEngineAccessor) particleEngine)
                .getProviders()
                .remove(BuiltInRegistries.PARTICLE_TYPE.getId(type));
    }

    public record ItemToTabEventImpl(ResourceKey<CreativeModeTab> tab,
                                     FabricItemGroupEntries entries) implements ItemToTabEvent {

        @Override
        public ResourceKey<CreativeModeTab> getTab() {
            return tab;
        }

        @Override
        public Collection<ItemStack> getAllItems() {
            return entries.getDisplayStacks();
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

    public static RegistryAccess getServerRegistryAccess() {
        return PolytoneFabric.currentServer.registryAccess();
    }

    public static BakedModel getBakedModel(ModelResourceLocation model) {
        var mm = Minecraft.getInstance().getModelManager();
        return mm.getModel(model.id());
    }

    private static ModelLoadingPlugin.Context hack;
    private static Consumer<PlatStuff.SpecialModelEvent> hack2;

    public static void addSpecialModelRegistration(Consumer<PlatStuff.SpecialModelEvent> eventListener) {
        ModelLoadingPlugin.register(pluginContext -> {
            eventListener.accept(new PlatStuff.SpecialModelEvent() {
                @Override
                public void register(ModelResourceLocation id) {
                    pluginContext.addModels(id.id());
                }
            });
        });
    }

    public static void doAddModels(){
       // hack2.accept(id -> hack.addModels(id.id()));
    }

    public static String getVersion() {
        return FabricLoader.getInstance().getModContainer("polytone").get().getMetadata().getVersion().getFriendlyString();
    }


    public static void registerColorResolver(ColorResolver colorResolver) {
        ColorResolverRegistry.register(colorResolver);
        Polytone.LOGGER.info("registered color resolver {}", colorResolver);

        if (colorResolver instanceof Colormap) {
            MY_CUSTOM_RESOLVERS.add(colorResolver);
        }
    }

    //lags behind as multithread bs...
    private static final Set<ColorResolver> MY_CUSTOM_RESOLVERS = new HashSet<>();

    public static void unregisterAllCustomColorResolves() {
        Set<ColorResolver> set = ColorResolverRegistryImpl.getAllResolvers();
        Set<ColorResolver> newSet = new HashSet<>(set);
        newSet.removeAll(MY_CUSTOM_RESOLVERS);
        ColorResolverRegistryAccessor.setAllResolvers(newSet);

        Set<ColorResolver> set2 = ColorResolverRegistryImpl.getCustomResolvers();
        Set<ColorResolver> newSet2 = new HashSet<>(set2);
        newSet2.removeAll(MY_CUSTOM_RESOLVERS);
        ColorResolverRegistryAccessor.setCustomResolvers(newSet2);

        MY_CUSTOM_RESOLVERS.clear();
    }


    public static void registerShaders(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> shaderConsumer) {
        CoreShaderRegistrationCallback.EVENT.register(registrationContext -> {
            registrationContext.register(id, format, shaderConsumer);
        });

    }


    public static <T> Iterable<Holder<T>> getTagEntries(HolderLookup.RegistryLookup<T> reg, TagKey<T> tag) {
        Set<Holder<T>> result = new HashSet<>();
        collectAllClientTags(tag, reg, result, new HashSet<>());
        return result;
    }

    //Thanks fabric api.. this client tag api they have is soo bad
    @SuppressWarnings("unchecked")
    private static <T> void collectAllClientTags(
            TagKey<T> tagKey,
            HolderLookup.RegistryLookup<T> registry,
            Set<Holder<T>> result,
            Set<TagKey<T>> visited
    ) {
        if (!visited.add(tagKey)) return;

        // Prefer synced registry tags if present
        if (registry.get(tagKey).isPresent()) {
            registry.get(tagKey).get().forEach(result::add);
            return;
        }

        // Local fallback (client-only tags)
        ClientTagsLoader.LoadedTag tag =
                ClientTagsImpl.getOrCreatePartiallySyncedTag(tagKey);

        // Direct entries
        for (ResourceLocation id : tag.immediateChildIds()) {
            ResourceKey<T> key = ResourceKey.create((ResourceKey)
                    registry.key().registryKey(), id);
            registry.get(key).ifPresent(result::add);
        }

        // Nested tags
        for (TagKey<?> child : tag.immediateChildTags()) {
            collectAllClientTags((TagKey<T>) child, registry, result, visited);
        }
    }

    public static Path getGamePath() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(v -> v.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    public static String getModLoader() {
        return "Fabric";
    }

    public static void matchStencil(RenderTarget main, RenderTarget snapshot) {
        // Fabric/vanilla has no enableStencil(); the main target is always plain depth, so there is
        // nothing to mirror and copyDepthFrom's depth blit always matches.
    }

}
