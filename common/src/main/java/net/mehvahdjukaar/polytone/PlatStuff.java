package net.mehvahdjukaar.polytone;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabModifier;
import net.minecraft.client.Camera;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PlatStuff {
    @ExpectPlatform
    public static String maybeRemapName(String s) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static boolean isModStateValid() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addClientReloadListener(final Supplier<PreparableReloadListener> listener, final Identifier name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getVersion() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerColorResolver(ColorResolver colormap) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void unregisterAllCustomColorResolves() {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static boolean isModLoaded(String namespace) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void applyBiomeSurgery(Biome biome, BiomeSpecialEffects newEffects) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addTabEventForTab(ResourceKey<CreativeModeTab> key) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static CreativeTabModifier modifyTab(CreativeTabModifier mod, CreativeModeTab tab) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static void sortTabs() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static CreativeModeTab createCreativeTab(Identifier id) {
        throw new AssertionError();
    }


    @ExpectPlatform
    public static RegistryAccess hackyGetRegistryAccess() {
        throw new AssertionError();

    }

    @Contract
    @ExpectPlatform
    public static ChunkSectionLayer getRenderType(Block block) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static void setRenderType(Block block, ChunkSectionLayer renderType) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float skyLight, float flicker, int torchX, int skyY, Vector3f combined) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static float compatACModifyGamma(float partialTicks, float gamma) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void updateSearchTrees(SessionSearchTrees sessionSearchTrees, List<CreativeModeTab> needsTreeUpdated) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ParticleProvider<?> getParticleProvider(ParticleType<?> type) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static void setParticleProvider(ParticleType<?> type, ParticleProvider<?> provider) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void unregisterParticleProvider(Identifier id) {
        throw new AssertionError();
    }


    public static <T> T registerDynamic(Registry<T> reg, Identifier id, T o) {
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
            Polytone.LOGGER.error("Unknown registry type {}", reg);
        }
        return o;
    }

    public static <T> void unregisterDynamic(Registry<T> reg, Identifier id) {
        if (reg instanceof MappedRegistry<T> mapped) {
            mapped.frozen = false;
            unRegister((MappedRegistry<T>) reg, ResourceKey.create(reg.key(), id));
            mapped.frozen = true;
        } else {
            Polytone.LOGGER.error("Unknown registry type {}", reg);
        }
    }

    private static <T> Holder.Reference<T> unRegister(MappedRegistry<T> reg, ResourceKey<T> key) {

        Holder.Reference<T> reference = reg.byKey.remove(key);

        if (reference != null) {
            T value = reference.value();

            reg.byLocation.remove(key.identifier());
            reg.byValue.remove(value);
            reg.byId.remove(reference);
            reg.toId.removeInt(value);
            reg.registrationInfos.remove(key);
        } else {
            Polytone.LOGGER.error("Tried to unregister object with key {} from registry {} but it does not exist", key, reg);
        }
        return reference;
    }


    @ExpectPlatform
    public static ParticleType<ExtraDataParticleOptions> makeParticleType(boolean forceSpawn) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static float getCamRoll(Camera camera) {
        throw new AssertionError();
    }

    public interface RegParticleGroup{
        void register(ParticleRenderType type, Function<ParticleEngine, ParticleGroup<?>> factory);
    }

    @ExpectPlatform
    public static void registerParticleGroup( Consumer<RegParticleGroup> eventConsumer)  {
        throw new AssertionError();
    }
}
