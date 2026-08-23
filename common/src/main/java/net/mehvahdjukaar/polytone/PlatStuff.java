package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionModel;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabModifier;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlatStuff {
    @PlatformImpl
    public static String maybeRemapName(String s) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static boolean isModStateValid() {
        throw new AssertionError();
    }

    @PlatformImpl
    public static <T> Iterable<Holder<T>> getTagEntries(HolderLookup.RegistryLookup<T> reg, TagKey<T> tag) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void addClientReloadListener(final Supplier<PreparableReloadListener> listener, final ResourceLocation name) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static String getVersion() {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void registerColorResolver(ColorResolver colormap) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void unregisterAllCustomColorResolves() {
        throw new AssertionError();
    }

    public interface SpecialModelEvent {
        void register(ModelResourceLocation id);
    }

    @Contract
    @PlatformImpl
    public static void addSpecialModelRegistration(Consumer<SpecialModelEvent> eventListener) {
        throw new AssertionError();
    }
    @PlatformImpl
    public static void doAddModels() {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void removeBlockColor(BlockColors colors, Block block) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static ItemColor getItemColor(ItemColors colors, Item item) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static BakedModel makeExpressionModel(ExpressionModel.Selector selector) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static boolean isModLoaded(String namespace) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static DimensionSpecialEffects getDimensionEffects(ResourceLocation type) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void applyBiomeSurgery(Biome biome, BiomeSpecialEffects newEffects) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void addTabEventForTab(ResourceKey<CreativeModeTab> key) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static CreativeTabModifier modifyTab(CreativeTabModifier mod, CreativeModeTab tab) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static void sortTabs() {
        throw new AssertionError();
    }

    @PlatformImpl
    public static CreativeModeTab createCreativeTab(ResourceLocation id) {
        throw new AssertionError();
    }


    @PlatformImpl
    public static RegistryAccess hackyGetRegistryAccess() {
        throw new AssertionError();

    }

    @Contract
    @PlatformImpl
    public static Object getRenderType(Block block) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static void setRenderType(Block block, Object renderType) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float skyLight, float flicker, int torchX, int skyY, Vector3f combined) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static float compatACModifyGamma(float partialTicks, float gamma) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static float compatSSGetSeason(Level level) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void updateSearchTrees(SessionSearchTrees sessionSearchTrees, List<CreativeModeTab> needsTreeUpdated) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static ParticleProvider<?> getParticleProvider(ParticleType<?> type) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static void setParticleProvider(ParticleType<?> type, ParticleProvider<?> provider) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void unregisterParticleProvider(ResourceLocation id) {
        throw new AssertionError();
    }


    public static <T> T registerDynamic(Registry<T> reg, ResourceLocation id, T o) {
        if (reg.containsKey(id)) {
            throw new RuntimeException("Tried to register object with id " + id + " to registry " + reg + " but it already exists");
        }
        if (reg instanceof MappedRegistry<T> mapped) {
            mapped.frozen = false;
            Registry.register(reg, id, o);
            var holder = reg.wrapAsHolder(o);
            //bind holder
            if (holder instanceof Holder.Reference<T> ref) {
                ref.bindTags(List.of());
            }
            mapped.frozen = true;
        } else {
            Polytone.LOGGER.error("Unknown registry type{}", reg);
        }
        return o;
    }

    public static <T> void unregisterDynamic(Registry<T> reg, ResourceLocation id) {
        if (reg instanceof MappedRegistry<T> mapped) {
            mapped.frozen = false;
            unRegister((MappedRegistry<T>) reg, ResourceKey.create(reg.key(), id));
            mapped.frozen = true;
        } else {
            Polytone.LOGGER.error("Unknown registry type{}", reg);
        }

        if (reg.containsKey(id)) {
            int aaa = 1;
        }

    }

    private static <T> Holder.Reference<T> unRegister(MappedRegistry<T> reg, ResourceKey<T> key) {

        Holder.Reference<T> reference = reg.byKey.remove(key);

        if (reference != null) {
            T value = reference.value();

            reg.byLocation.remove(key.location());
            reg.byValue.remove(value);
            reg.byId.remove(reference);
            reg.toId.removeInt(value);
            reg.registrationInfos.remove(key);
        } else {
            Polytone.LOGGER.error("Tried to unregister object with key {} from registry {} but it does not exist", key, reg);
        }
        return reference;
    }

    //must be weakest generic otherwise we could have crashes when people assume the old generic due to implicit methods
    @PlatformImpl
    public static ParticleType<ParticleOptions> makeParticleType(boolean forceSpawn) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static RegistryAccess getServerRegistryAccess() {
        throw new AssertionError();
    }

    @PlatformImpl
    public static BakedModel getBakedModel(ModelResourceLocation model) {
        throw new AssertionError();
    }


    // core shader by namespaced id; vanilla's String constructor only takes minecraft-namespace names
    @PlatformImpl
    public static ShaderInstance createShader(ResourceProvider provider, ResourceLocation id, VertexFormat format) throws IOException {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void registerShaders(ResourceLocation event, VertexFormat format,
                                       Consumer<ShaderInstance> shaderConsumer) {
        throw new AssertionError();

    }

    // forge's enableStencil() changes the depth format, and copyDepthFrom between mismatched
    // targets fails with INVALID_OPERATION. No-op on fabric, where depth is always plain.
    @PlatformImpl
    public static void matchStencil(RenderTarget main, RenderTarget snapshot) {
        throw new AssertionError();
    }
}
