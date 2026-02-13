package net.mehvahdjukaar.polytone;

import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.reloader.CompoundReloader;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.compat.IrisCompat;
import net.mehvahdjukaar.polytone.content.biome.BiomeEffectsManager;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapperManager;
import net.mehvahdjukaar.polytone.content.biome.BiomeKeysCache;
import net.mehvahdjukaar.polytone.content.block.BlockPropertiesManager;
import net.mehvahdjukaar.polytone.content.block.BlockSetManager;
import net.mehvahdjukaar.polytone.content.color.ColorManager;
import net.mehvahdjukaar.polytone.content.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager;
import net.mehvahdjukaar.polytone.content.dimension.DimensionEffectsManager;
import net.mehvahdjukaar.polytone.content.entity.EntityModifiersManager;
import net.mehvahdjukaar.polytone.content.fluid.FluidPropertiesManager;
import net.mehvahdjukaar.polytone.content.global_expressions.GlobalExpressionsManager;
import net.mehvahdjukaar.polytone.content.item.CustomItemModelsManager;
import net.mehvahdjukaar.polytone.content.item.ItemModifiersManager;
import net.mehvahdjukaar.polytone.content.lightmap.LightmapsManager;
import net.mehvahdjukaar.polytone.content.noise.NoiseManager;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticlesManager;
import net.mehvahdjukaar.polytone.content.particle.modifiers.ParticleModifiersManager;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierManager;
import net.mehvahdjukaar.polytone.content.slotify.GuiOverlayManager;
import net.mehvahdjukaar.polytone.content.sound.SoundTypesManager;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabsModifiersManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

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
    public static final EntityModifiersManager ENTITY_MODIFIERS = new EntityModifiersManager();
    public static final SoundTypesManager SOUND_TYPES = new SoundTypesManager();
    public static final ColorManager COLORS = new ColorManager();
    public static final GuiModifierManager SLOTIFY = new GuiModifierManager();
    public static final GuiOverlayManager OVERLAY_MODIFIERS = new GuiOverlayManager();
    public static final BlockSetManager BLOCK_SET = new BlockSetManager();
    public static final CreativeTabsModifiersManager CREATIVE_TABS_MODIFIERS = new CreativeTabsModifiersManager();
    public static final GlobalExpressionsManager GLOBAL_EXPRESSION = new GlobalExpressionsManager();
    public static final ConfigsManager CONFIGS = new ConfigsManager();

    private static final Future<Set<Identifier>> FUTURE_IDS = CompletableFuture.supplyAsync(Polytone::loadFutureIds);

    public static boolean iMessedUp = false;

    public static boolean isDevEnv = false;
    public static boolean isForge = false;

    public static void init(boolean devEnv, boolean forge) {
        COMPOUND_RELOADER = new CompoundReloader(
                CONFIGS, GLOBAL_EXPRESSION,
                NOISES, SOUND_TYPES, BIOME_ID_MAPPERS, COLORMAPS, CUSTOM_PARTICLES, COLORS,
                BLOCK_SET, BLOCK_MODIFIERS, FLUID_MODIFIERS, ITEM_MODIFIERS, ITEM_MODELS,
                BIOME_MODIFIERS,//LIGHTMAPS,
                DIMENSION_MODIFIERS,
                PARTICLE_MODIFIERS, SLOTIFY, OVERLAY_MODIFIERS, ENTITY_MODIFIERS,
                CREATIVE_TABS_MODIFIERS);
        PlatStuff.addClientReloadListener(() -> COMPOUND_RELOADER,
                res("polytone_stuff"));
        isDevEnv = devEnv;
        isForge = forge;

        //ItemModelOverrideList.testTrie();
        //GenericDirectorySpriteSource.init();

        PolytoneRenderTypes.init();
        if (CompatHandler.IRIS) IrisCompat.init();

        //TODO: server resource pack
        //TODO: custom block breaking particles
        //TODO: custom place particles
        //TODO: block mobs from emitting particles. like blaze
    }

    public static Identifier res(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }


    public static void onTick(Level level) {
        GLOBAL_EXPRESSION.tick(level);
        ClientFrameTicker.onTick(level);
        ExpTicker.onTick(level);
        ENTITY_MODIFIERS.onTick(level);
    }

    public static void onTagsReceived(HolderLookup.Provider registryAccess) {
        try {
            if (isDevEnv) {
                scanAllRegistries(registryAccess);
            }//always clear as fabric fires tag loaded even on reload command
            COMPOUND_RELOADER.applyWithLevel(registryAccess, false);
            BiomeKeysCache.clear();

            if (CONFIGS.lenientLoading.get()) {
                displayLenientLoadToast();
            }

        } catch (RuntimeException e) {
            Polytone.LOGGER.error("Failed to apply some Polytone modifiers on world load", e);
            displayLateReloadFailedToast();
        }

    }


    public static void onLogOut() {
        COMPOUND_RELOADER.resetWithLevel(true);
        BiomeKeysCache.clear();
    }

    public static void onEarlyPackLoad(ResourceManager manager) {
        SpecialModelsHandler.clear();
        COMPOUND_RELOADER.earlyProcess(new PreparableReloadListener.SharedState(manager));
    }

    public static void logException(Exception e, String message) {
        // Find the Log4j logging directory
        String logDir = getLog4jDirectory().orElse(Paths.get("logs").toAbsolutePath().toString());

        // Create the full path for the new log file
        String logFilePath = Paths.get(logDir, "polytone.log").toString();

        // Write the exception to the new log file
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, false))) {
            writer.println("Polytone version: " + PlatStuff.getVersion());
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

    public static boolean isEntryDynamic(Registry<?> reg, Identifier entryId) {
        if (reg == BuiltInRegistries.CREATIVE_MODE_TAB) {
            return CREATIVE_TABS_MODIFIERS.isDynamicTab(entryId);
        }
        if (reg == BuiltInRegistries.PARTICLE_TYPE) {
            return CUSTOM_PARTICLES.isDynamicParticle(entryId);
        }
        if (reg == BuiltInRegistries.SOUND_EVENT) {
            return SOUND_TYPES.isDynamicSound(entryId);
        }
        return false;
    }

    public static boolean isFutureId(Identifier id) {
        try {
            // Blocks until the future is complete, or throws if there was an error
            var isFuture = FUTURE_IDS.get().contains(id);
            if (isFuture) {
                Polytone.LOGGER.error("Found an ID from a future Minecraft version ({}). Polytone will skip it but this is remains a bug of the Resource Pack. Optional entries or resource conditions should be used to maintain backward compatibility instead", id);
            }
            return isFuture;
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to check if ID is from a future version", e);
        }
        return false;
    }

    private static void scanAllRegistries(HolderLookup.Provider provider) {
        //open a file and save all IDS to it
        var regs = List.of(
                Registries.SOUND_EVENT, Registries.BIOME,
                Registries.BLOCK, Registries.ITEM,
                Registries.BLOCK_TYPE, Registries.PARTICLE_TYPE, Registries.FLUID,
                Registries.POTION, Registries.MOB_EFFECT
        );
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("registry_dump.txt"))) {
            for (var v : regs) {

                var reg = provider.lookupOrThrow(v);
                for (var e : reg.listElements().toList()) {
                    writer.write(e.getRegisteredName());
                    writer.newLine(); // writes a newline character
                }
            }
            System.out.println("File written successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static Set<Identifier> loadFutureIds() {
        var res = Polytone.class.getClassLoader().getResourceAsStream("future_ids.txt");
        //read
        Set<Identifier> futureIds = new HashSet<>();
        if (res != null) {
            try (var reader = new BufferedReader(new InputStreamReader(res))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Identifier id = Identifier.tryParse(line);
                    if (id != null) {
                        futureIds.add(id);
                    } else {
                        Polytone.LOGGER.warn("Invalid Identifier in future_ids.txt: {}", line);
                    }
                }
            } catch (IOException e) {
                Polytone.LOGGER.error("Failed to read future_ids.txt", e);
            }
        }
        return futureIds;
    }


    public static void onDimChanged(Level to) {
        DIMENSION_MODIFIERS.onDimensionChanged(to.dimensionTypeRegistration(), to.registryAccess());
    }

    public static void maybeThrow(RuntimeException e) {
        if (CONFIGS.isLenientLoading()) {
            LOGGER.error("Pack loading error occurred. Ignoring due to lenient loading config.", e);
        } else {
            throw e;
        }
    }

    public static final SystemToast.SystemToastId LENIENT_LOAD_WARN = new SystemToast.SystemToastId(1500);

    public static void displayLenientLoadToast() {
        ToastManager toastComponent = Minecraft.getInstance().getToastManager();
        SystemToast.addOrUpdate(toastComponent, LENIENT_LOAD_WARN,
                Component.translatable("toast.polytone.lenient_load"),
                Component.translatable("toast.polytone.lenient_load_desc"));
    }

    public static void displayEarlyReloadFailedToast() {
        ToastManager toastComponent = Minecraft.getInstance().getToastManager();
        SystemToast.addOrUpdate(toastComponent, SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                Component.translatable("toast.polytone.early_load_fail"),
                Component.translatable("toast.polytone.load_fail"));
    }

    public static void displayLateReloadFailedToast() {
        ToastManager toastComponent = Minecraft.getInstance().getToastManager();
        SystemToast.addOrUpdate(toastComponent, SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                Component.translatable("toast.polytone.lazy_load_fail"),
                Component.translatable("toast.polytone.load_fail"));
    }

}
