package net.mehvahdjukaar.polytone.content.config;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.FilesUtil;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigsManager extends ContentManager<PolyConfig<?>> {

    public final OptionHolder<Boolean> lenientLoading = builtinConfig("lenient_loading", "loading", false);
    public final OptionHolder<Boolean> legacyParsing = builtinConfig("legacy_parsing", "loading", true);
    public final OptionHolder<Float> particlesThrottle = builtinConfig("particles_throttle", "particles", 1);
    public final OptionHolder<Boolean> autoParticleRateLimit = builtinConfig("auto_particle_rate_limit", "particles", false);
    public final OptionHolder<Boolean> particlesOffThread = builtinConfig("custom_particles_async", "particles", false);
    public final OptionHolder<Boolean> showConfigButton = builtinConfig("show_config_button", null, true);
    public final OptionHolder<Boolean> postChainsAfterHand = builtinConfig("post_chains_after_hand", "shaders", true);
    public final OptionHolder<Float> attributeTimeSmoothing = builtinConfig("attribute_time_smoothing", null, 0.4f, 2, 0.05f);
    public final OptionHolder<ColoredLightsBackend> coloredLightsBackend = builtinConfig("colored_lights_backend", "shaders", ColoredLightsBackend.BUILT_IN);
    public final OptionHolder<Float> voxelVolumeWidth = builtinConfig("voxel_volume_width", "shaders", 128, 32, 384, 16);
    public final OptionHolder<Float> voxelVolumeHeight = builtinConfig("voxel_volume_height", "shaders", 128, 64, 256, 16);
    public final OptionHolder<Float> farLightRange = builtinConfig("far_light_range", "shaders", 512, 0, 1024, 64);
    public final OptionHolder<Float> lightSpreadSteps = builtinConfig("light_spread_steps", "shaders", 3, 1, 15, 1);
    public final OptionHolder<Boolean> textureLightColors = builtinConfig("texture_light_colors", "shaders", false);
    public final OptionHolder<SmoothEntityLighting> smoothEntityLighting = builtinConfig("smooth_entity_lighting", "shaders", SmoothEntityLighting.LOCAL_PLAYER);

    private final List<OptionHolder<?>> builtins = new ArrayList<>(List.of(lenientLoading, legacyParsing, particlesThrottle, autoParticleRateLimit, particlesOffThread, showConfigButton, postChainsAfterHand, attributeTimeSmoothing, coloredLightsBackend, voxelVolumeWidth, voxelVolumeHeight, farLightRange, lightSpreadSteps, textureLightColors, smoothEntityLighting));

    public final ConfigBubbleManager bubbleManager = new ConfigBubbleManager();

    public static @NonNull OptionHolder<Boolean> builtinConfig(String id, @Nullable String section, boolean def) {
        return OptionHolder.create(new BoolConfig(Optional.empty(), Map.of(), Map.of(), 1,
                Optional.ofNullable(section), Optional.empty(), Optional.empty(), false, Map.of(), def), Polytone.res(id));
    }

    private static @NonNull OptionHolder<Float> builtinConfig(String id, @Nullable String section, float def) {
        return builtinConfig(id, section, def, 1, 0.01f);
    }

    private static @NonNull OptionHolder<Float> builtinConfig(String id, @Nullable String section, float def,
                                                              float max, float step) {
        return builtinConfig(id, section, def, 0, max, step);
    }

    public static @NonNull OptionHolder<Float> builtinConfig(String id, @Nullable String section, float def,
                                                             float min, float max, float step) {
        return OptionHolder.create(new NumberConfig(Optional.empty(), Map.of(), Map.of(), 1,
                Optional.ofNullable(section), Optional.empty(), Optional.empty(), false, Map.of(), def, min, max, step), Polytone.res(id));
    }

    private static <E extends Enum<E> & StringRepresentable> @NonNull OptionHolder<E> builtinConfig(String id, @Nullable String section, E def) {
        return OptionHolder.create(new EnumConfig<>(def, Optional.ofNullable(section)), Polytone.res(id));
    }

    private final MapRegistry<OptionHolder<?>> configs = new MapRegistry<>("Configs");
    private final ThreadLocal<MapRegistry<OptionHolder<?>>> activeLoadConfigs = new ThreadLocal<>(); // from active packs
    private final File optionsFile;
    private final Gson gson;

    private JsonObject configFileSnapshot;

    private final AtomicBoolean needsPackReload = new AtomicBoolean(false); //mega ugly

    public ConfigsManager() {
        super(Spec.of("Config entry", () -> SchemaCodec.wrap(PolyConfig.CODEC))
                .wikiPage("Polytone-Configs")
                .folders("config_entries"));
        this.optionsFile = PlatStuff.getGamePath().resolve("config/polytone_options.json").toFile();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        loadConfigFromDisk();
        registerBuiltins(configs);
    }

    public <T> OptionHolder<T> addBuiltin(OptionHolder<T> option) {
        builtins.add(option);
        option.loadFromJson(configFileSnapshot);
        configs.unregister(option.fileId);
        configs.register(option.fileId, option);
        return option;
    }

    private void registerBuiltins(MapRegistry<OptionHolder<?>> reg) {
        for (OptionHolder<?> b : builtins) {
            b.loadFromJson(configFileSnapshot);
            reg.unregister(b.fileId);
            reg.register(b.fileId, b);
        }
    }

    private static void addConfig(Identifier id, PolyConfig<?> config,
                                  MapRegistry<OptionHolder<?>> reg,
                                  JsonObject dataJson) {
        OptionHolder<?> instance = OptionHolder.create(config, id);
        instance.loadFromJson(dataJson);

        reg.unregister(id);
        reg.register(id, instance);
    }

    public boolean checkAndClearNeedsPackReload() {
        return needsPackReload.getAndSet(false);
    }

    public boolean hasPackConfigs() {
        for (var option : configs.getValues()) {
            if (!option.fileId.getNamespace().equals(Polytone.MOD_ID)) return true;
        }
        return false;
    }

    public Screen createScreenForMainMenu(Screen parent) {
        bubbleManager.onConfigOpened(hasPackConfigs());
        List<OptionHolder<?>> shown = shownOptions();
        return new ConfigScreen(parent, shown, () -> {
            if (!hasUnsavedChanges(shown)) return;

            saveConfigsToDisk(shown);
            Minecraft.getInstance().reloadResourcePacks();
        });
    }

    public Screen createScreenForPack(PackSelectionScreen parent) {
        return createScreenForPack(parent, parent::reload);
    }

    public Screen createScreenForPack(Screen parent, Runnable packReload) {
        bubbleManager.onConfigOpened(hasPackConfigs());
        List<OptionHolder<?>> shown = shownOptions();

        return new ConfigScreen(parent, shown, () -> {
            if (!hasUnsavedChanges(shown)) return;

            needsPackReload.set(true);
            saveConfigsToDisk(shown);
            //save values we just set so we can read them again right here
            packReload.run();
            //we cant just reload packs here as this would cause a double reload.
        });
    }

    public SpriteIconButton makeConfigButton(int width, Screen parent, Runnable packReload) {
        return SpriteIconButton.builder(Component.translatable("options.accessibility"),
                        b -> {
                            bubbleManager.onConfigButtonClicked();
                            Minecraft.getInstance().setScreen(createScreenForPack(parent, packReload));
                        }, true).width(width)
                .sprite(Polytone.res("paint_brush"), 16, 16).build();
    }

    private List<OptionHolder<?>> shownOptions() {
        return List.copyOf(configs.getValues());
    }

    private static boolean hasUnsavedChanges(Collection<OptionHolder<?>> options) {
        for (var option : options) {
            if (option.hasUnsavedChanges()) return true;
        }
        return false;
    }

    private void saveConfigsToDisk(Collection<OptionHolder<?>> edited) {
        try {
            JsonObject jsonObject = configFileSnapshot.deepCopy();

            for (var option : configs.getValues()) {
                option.saveToJson(jsonObject);
            }
            for (var option : edited) {
                option.saveToJson(jsonObject);
            }

            Path target = this.optionsFile.toPath();

            FilesUtil.writeTextAtomically(target, writer ->
                    GsonHelper.writeValue(gson.newJsonWriter(writer), jsonObject, null)
            );

            this.configFileSnapshot = jsonObject;

            Polytone.LOGGER.info("Saved Polytone config options to {}", this.optionsFile.getCanonicalPath());
        } catch (Exception e) {
            Polytone.LOGGER.error("Error saving config options to file", e);
        }
    }

    private void loadConfigFromDisk() {
        JsonObject jo = new JsonObject();

        if (this.optionsFile.exists()) {
            try (BufferedReader reader = Files.newReader(this.optionsFile, StandardCharsets.UTF_8)) {
                jo = GsonHelper.fromJson(gson, reader, JsonObject.class);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error loading config options from file", e);
            }
        }

        this.configFileSnapshot = jo;
    }

    public boolean getBooleanConfig(Identifier id) {
        var value = getValue(id);
        if (value instanceof Boolean b) {
            return b;
        }
        return false;
    }

    public MapRegistry<OptionHolder<?>> getActiveRegistry() {
        return Objects.requireNonNullElse(activeLoadConfigs.get(), configs);
    }

    public Object getValue(Identifier configKey) {
        var reg = getActiveRegistry();
        OptionHolder<?> value = reg.getValue(configKey);
        if (value == null) value = configs.getValue(configKey);
        if (value != null) {
            //special case for our enums
            return value.get() instanceof StringRepresentable sr ? sr.getSerializedName() : value.get();
        } else {
            Polytone.LOGGER.warn("Tried to get config value for unknown key: {}", configKey);
            return 0;
        }
    }

    //is this pack active or not? we dont know
    //called one pack at the time. we cant do IO there, we rely on the cache
    public void loadCurrentPackConfigs(PackResources primary, Pack.ResourcesSupplier resources,
                                       PackLocationInfo location, PackFormat version, PackType packType) {
        if (packType != PackType.CLIENT_RESOURCES) return;
        PackSource source = primary.location().source();
        if (source == PackSource.BUILT_IN || source == PackSource.FEATURE) return;

        MapRegistry<OptionHolder<?>> activePackReg = new MapRegistry<>("Active Pack Configs");
        registerBuiltins(activePackReg);
        activeLoadConfigs.set(activePackReg);
        parsePackConfigsInto(primary, packType, activePackReg);

        List<String> overlays = collectFormatOverlays(primary, packType, version);
        if (overlays.isEmpty()) return;

        try (PackResources fullPack = resources.openFull(location, new Pack.Metadata(Component.empty(),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), overlays))) {
            parsePackConfigsInto(fullPack, packType, activePackReg);
        }
    }

    //for stuff that loads before us..
    public void loadCurrentReloadConfigs(ResourceManager resourceManager) {
        MapRegistry<OptionHolder<?>> reloadReg = new MapRegistry<>("Active Reload Configs");
        registerBuiltins(reloadReg);
        activeLoadConfigs.set(reloadReg);
        parseConfigsInto(resourceManager, reloadReg);
    }

    private void parsePackConfigsInto(PackResources pack, PackType packType, MapRegistry<OptionHolder<?>> reg) {
        parseConfigsInto(new MultiPackResourceManager(packType, List.of(pack)), reg);
    }

    private void parseConfigsInto(ResourceManager resourceManager, MapRegistry<OptionHolder<?>> reg) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        for (var j : parseEnabledJsons(jsons, JsonOps.INSTANCE)) {
            if (j != null) {
                addConfig(j.getKey(), j.getValue(), reg, configFileSnapshot);
            }
        }
    }

    private static List<String> collectFormatOverlays(PackResources primary, PackType packType, PackFormat version) {
        List<String> overlays = new ArrayList<>();
        try {
            OverlayMetadataSection section = primary.getMetadataSection(OverlayMetadataSection.forPackType(packType));
            if (section != null) overlays.addAll(section.overlaysForVersion(version));
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to read overlay metadata while loading configs for pack {}",
                    primary.location().id(), e);
        }
        return overlays;
    }

    public void clearCurrentPackConfigs() {
        activeLoadConfigs.remove();
    }

    public boolean isLenientLoading() {
        return lenientLoading.get();
    }

    @Override
    protected void applyNormal(AssetsFiles resources) {
        Map<Identifier, JsonElement> obj = resources.jsons();
        activeLoadConfigs.remove();
        configs.clear();
        registerBuiltins(configs);

        Map<Identifier, PolyConfig<?>> parsed = new HashMap<>();
        Iterable<Map.Entry<Identifier, PolyConfig<?>>> parsedConfigs = parseEnabledJsons(obj, JsonOps.INSTANCE);
        for (var j : parsedConfigs) {
            PolyConfig<?> p = j.getValue();
            parsed.put(j.getKey(), p);
        }
        for (var entry : parsed.entrySet()) {
            addConfig(entry.getKey(), entry.getValue(), configs, configFileSnapshot);
        }
        Polytone.LOGGER.info("Loaded {} Polytone config entries", configs.size());
    }

    public void beforeRepositoryRefresh() {
        loadConfigFromDisk();
    }

    public enum ColoredLightsBackend implements StringRepresentable {
        OFF, BUILT_IN, HYBRID, VEIL;

        public boolean tintsVanillaBlockLight() {
            return this == BUILT_IN || this == HYBRID;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum SmoothEntityLighting implements StringRepresentable {
        OFF, LOCAL_PLAYER, ALL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum ButtonPosition {
        NONE,
        LEFT,
        RIGHT
    }

    public ButtonPosition getButtonPos() {
        if (!showConfigButton.get()) return ButtonPosition.NONE;
        if (configs.isEmpty()) return ButtonPosition.NONE;
        return (CompatHandler.EMF || CompatHandler.ETF) ? ButtonPosition.LEFT : ButtonPosition.RIGHT;
    }
}
