package net.mehvahdjukaar.polytone;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.tabs.CreativeTabModifier;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3f;

import java.util.function.Consumer;
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
    public static void addClientReloadListener(final Supplier<PreparableReloadListener> listener, final ResourceLocation name) {
        throw new AssertionError();
    }

    public interface SpecialModelEvent {
        void register(ModelResourceLocation modelLocation);

        void register(ResourceLocation id);
    }

    @ExpectPlatform
    public static void addSpecialModelRegistration(Consumer<SpecialModelEvent> eventListener) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static BakedModel getModel(ResourceLocation modelLocation) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static ItemColor getItemColor(ItemColors colors, Item item) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static boolean isModLoaded(String namespace) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Contract
    public static <T extends SoundEvent> T registerSoundEvent(ResourceLocation id, T sound) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Contract
    public static <T extends ParticleType<?>> T registerParticleType(ResourceLocation id, T sound) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static DimensionSpecialEffects getDimensionEffects(ResourceLocation type) {
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
    public static CreativeModeTab createCreativeTab(ResourceLocation id) {
        throw new AssertionError();
    }


    @ExpectPlatform
    public static RegistryAccess hackyGetRegistryAccess() {
        throw new AssertionError();

    }

    @Contract
    @ExpectPlatform
    public static RenderType getRenderType(Block block) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static void setRenderType(Block block, RenderType renderType) {
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
    public static ParticleProvider<?> getParticleProvider(ParticleType<?> type) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static void setParticleProvider(ParticleType<?> type, ParticleProvider<?> provider) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void unregisterParticleProvider(ResourceLocation id) {
        throw new AssertionError();
    }


    public static <T> T registerDynamic(Registry<T> reg, ResourceLocation id, T o) {
        if (Polytone.isForge) {
            if (reg.key().equals(Registries.SOUND_EVENT) && o instanceof SoundEvent s) {
                registerSoundEvent(id, s);
                return o;
            } else if (reg.key().equals(Registries.PARTICLE_TYPE) && o instanceof ParticleType<?> t) {
                registerParticleType(id, t);
                return o;
            }
        }
        registerFabric(reg, id, o);

        return o;
    }

    private static <T> void registerFabric(Registry<T> reg, ResourceLocation id, T o) {
        if (reg.containsKey(id)) {
            throw new RuntimeException("Tried to register object with id " + id + " to registry " + reg + " but it already exists");
        }
        ((MappedRegistry) reg).frozen = false;
        Registry.register(reg, id, o);
        reg.freeze();
    }

    public static <T> void unregisterDynamic(Registry<T> reg, ResourceLocation id) {
        if (Polytone.isForge) {
            if (reg.key().equals(Registries.SOUND_EVENT)) {
                unregisterSoundEvent(id);
                return;
            } else if (reg.key().equals(Registries.PARTICLE_TYPE)) {
                unregisterParticleType(id);
                return;
            }
        }
        unRegister((MappedRegistry<T>) reg, ResourceKey.create(reg.key(), id));
    }

    @ExpectPlatform
    public static void unregisterParticleType(ResourceLocation id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static void unregisterSoundEvent(ResourceLocation id) {
        throw new AssertionError();
    }

    private static <T> Holder.Reference<T> unRegister(MappedRegistry<T> reg, ResourceKey<T> key) {

        reg.frozen = false;

        Holder.Reference<T> reference = reg.byKey.remove(key);

        if (reference != null) {
            T value = reference.value();

            reg.byLocation.remove(key.location());
            reg.byValue.remove(value);
            reg.byId.remove(reference);
            reg.toId.removeInt(value);
        } else {
            int aa = 1;
        }
        reg.freeze();

        return reference;
    }


    @ExpectPlatform
    public static SimpleParticleType makeParticleType(boolean forceSpawn) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static RegistryAccess getServerRegistryAccess() {
        throw new AssertionError();
    }

}
