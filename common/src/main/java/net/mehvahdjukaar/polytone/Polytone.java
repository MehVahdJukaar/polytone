package net.mehvahdjukaar.polytone;

import net.mehvahdjukaar.polytone.biome.BiomeEffectsManager;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapperManager;
import net.mehvahdjukaar.polytone.block.BlockPropertiesManager;
import net.mehvahdjukaar.polytone.block.BlockSetManager;
import net.mehvahdjukaar.polytone.color.ColorManager;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.dimension.DimensionEffectsManager;
import net.mehvahdjukaar.polytone.fluid.FluidPropertiesManager;
import net.mehvahdjukaar.polytone.item.CustomItemModelsManager;
import net.mehvahdjukaar.polytone.item.ItemModelOverrideList;
import net.mehvahdjukaar.polytone.item.ItemModifiersManager;
import net.mehvahdjukaar.polytone.lightmap.LightmapsManager;
import net.mehvahdjukaar.polytone.noise.NoiseManager;
import net.mehvahdjukaar.polytone.particle.CustomParticlesManager;
import net.mehvahdjukaar.polytone.particle.ParticleModifiersManager;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.mehvahdjukaar.polytone.slotify.GuiOverlayManager;
import net.mehvahdjukaar.polytone.sound.SoundTypesManager;
import net.mehvahdjukaar.polytone.tabs.CreativeTabsModifiersManager;
import net.mehvahdjukaar.polytone.texture.VariantTextureManager;
import net.mehvahdjukaar.polytone.utils.BiomeKeysCache;
import net.mehvahdjukaar.polytone.utils.CompoundReloader;
import net.mehvahdjukaar.polytone.utils.GenericDirectorySpriteSource;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class Polytone {
    public static final String MOD_ID = "polytone";

    public static final Logger LOGGER = LogManager.getLogger("Polytone");

    private static CompoundReloader COMPOUND_RELOADER;
    public static final BlockPropertiesManager BLOCK_MODIFIERS = new BlockPropertiesManager();
    public static final FluidPropertiesManager FLUID_MODIFIERS = new FluidPropertiesManager();
    public static final ItemModifiersManager ITEM_MODIFIERS = new ItemModifiersManager();
    public static final CustomItemModelsManager ITEM_MODELS = new CustomItemModelsManager();
    public static final BiomeEffectsManager BIOME_MODIFIERS = new BiomeEffectsManager();
    public static final NoiseManager NOISES = new NoiseManager();
    public static final ColormapsManager COLORMAPS = new ColormapsManager();
    public static final LightmapsManager LIGHTMAPS = new LightmapsManager();
    public static final BiomeIdMapperManager BIOME_ID_MAPPERS = new BiomeIdMapperManager();
    public static final DimensionEffectsManager DIMENSION_MODIFIERS = new DimensionEffectsManager();
    public static final CustomParticlesManager CUSTOM_PARTICLES = new CustomParticlesManager();
    public static final ParticleModifiersManager PARTICLE_MODIFIERS = new ParticleModifiersManager();
    public static final SoundTypesManager SOUND_TYPES = new SoundTypesManager();
    public static final VariantTextureManager VARIANT_TEXTURES = new VariantTextureManager();
    public static final ColorManager COLORS = new ColorManager();
    public static final GuiModifierManager SLOTIFY = new GuiModifierManager();
    public static final GuiOverlayManager OVERLAY_MODIFIERS = new GuiOverlayManager();
    public static final BlockSetManager BLOCK_SET = new BlockSetManager();
    public static final CreativeTabsModifiersManager CREATIVE_TABS_MODIFIERS = new CreativeTabsModifiersManager();

    public static boolean iMessedUp = false;

    public static boolean isDevEnv = false;
    public static boolean isForge = false;
    public static boolean iris = false;

    public static void init(boolean devEnv, boolean forge, boolean iris) {
        COMPOUND_RELOADER = new CompoundReloader(
                NOISES, SOUND_TYPES, BIOME_ID_MAPPERS, COLORMAPS, CUSTOM_PARTICLES, COLORS,
                BLOCK_SET, BLOCK_MODIFIERS, FLUID_MODIFIERS, ITEM_MODIFIERS, ITEM_MODELS,
                BIOME_MODIFIERS, VARIANT_TEXTURES, LIGHTMAPS, DIMENSION_MODIFIERS,
                PARTICLE_MODIFIERS, SLOTIFY, OVERLAY_MODIFIERS,
                CREATIVE_TABS_MODIFIERS);
        PlatStuff.addClientReloadListener(() -> COMPOUND_RELOADER,
                res("polytone_stuff"));
        isDevEnv = devEnv;
        isForge = forge;
        Polytone.iris = iris;

        ItemModelOverrideList.testTrie();
        GenericDirectorySpriteSource.init();

        PlatStuff.addSpecialModelRegistration(Polytone::addSpecialModels);
    }

    public static ResourceLocation res(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }


    public static void onTagsReceived(HolderLookup.Provider registryAccess) {
        try {
            COMPOUND_RELOADER.applyOnLevelLoad(registryAccess, true);
            BiomeKeysCache.clear();
            LazyHolderSet.initializeAll(registryAccess);
        } catch (RuntimeException e) {
            Polytone.LOGGER.error("Failed to apply some Polytone modifiers on world load", e);

            ToastManager toastComponent = Minecraft.getInstance().getToastManager();
            SystemToast.addOrUpdate(toastComponent, SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                    Component.translatable("toast.polytone.lazy_load_fail"),
                    Component.translatable("toast.polytone.load_fail"));
        }

    }

    public static void onLevelUnload() {
        BiomeKeysCache.clear();
    }


    public static void logException(Exception e, String message) {
        // Find the Log4j logging directory
        String logDir = getLog4jDirectory().orElse(Paths.get("logs").toAbsolutePath().toString());

        // Create the full path for the new log file
        String logFilePath = Paths.get(logDir, "polytone.log").toString();

        // Write the exception to the new log file
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, false))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);
            writer.println("[" + timestamp + "] " + message + ". Check lines below to see where the error was:");
            e.printStackTrace(writer);
        } catch (IOException ioException) {
            LOGGER.error("Failed to log onto polytone.log", ioException);
        }
    }

    private static Optional<String> getLog4jDirectory() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        return config.getAppenders().values().stream()
                .filter(appender -> appender instanceof FileAppender)
                .map(appender -> ((FileAppender) appender).getFileName())
                .map(Paths::get)
                .map(Path::getParent)
                .map(Path::toString)
                .findFirst();
    }
}
