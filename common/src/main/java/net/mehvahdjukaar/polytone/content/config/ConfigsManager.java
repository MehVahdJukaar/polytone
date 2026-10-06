package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.common.FilesUtil;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigsManager extends ContentManager<PolyConfig<?>> {

    public final OptionHolder<Boolean> lenientLoading = builtinConfig("lenient_loading", false, null);
    public final OptionHolder<Boolean> legacyParsing = builtinConfig("legacy_parsing", true, null);
    public final OptionHolder<Float> particlesThrottle = builtinConfig("particles_throttle", 1f, "particles");
    public final OptionHolder<Boolean> autoParticleRateLimit = builtinConfig("auto_particle_rate_limit", false, "particles");
    public final OptionHolder<Boolean> particlesOffThread = builtinConfig("custom_particles_async", false, "particles");
    public final OptionHolder<Boolean> showConfigButton = builtinConfig("show_config_button", true, null);
    public final OptionHolder<Boolean> postShadersOccludeHeldItems = builtinConfig("post_shaders_occlude_held_items", true, "shaders");
    public final OptionHolder<ColoredLightsBackend> coloredLightsBackend = builtinConfig("colored_lights_backend", ColoredLightsBackend.BUILT_IN, "shaders");
    public final OptionHolder<Float> voxelVolumeWidth = builtinConfig("voxel_volume_width", 128, 32, 384, 16, "shaders");
    public final OptionHolder<Float> voxelVolumeHeight = builtinConfig("voxel_volume_height", 128, 64, 256, 16, "shaders");
    public final OptionHolder<Float> farLightRange = builtinConfig("far_light_range", 512, 0, 1024, 64, "shaders");
    public final OptionHolder<Float> lightSpreadSteps = builtinConfig("light_spread_steps", 3, 1, 15, 1, "shaders");
    public final OptionHolder<SmoothEntityLighting> smoothEntityLighting = builtinConfig("smooth_entity_lighting", SmoothEntityLighting.LOCAL_PLAYER, "shaders");

    public final ConfigBubbleManager bubbleManager = new ConfigBubbleManager();

    private final MapRegistry<OptionHolder<?>> configs = new MapRegistry<>("Configs");
    private final ThreadLocal<MapRegistry<OptionHolder<?>>> activeLoadConfigs = new ThreadLocal<>();
    private final File optionsFile;
    private final Gson gson;
    private JsonObject configFileSnapshot = new JsonObject();
    private final AtomicBoolean needsPackReload = new AtomicBoolean(false);

    public ConfigsManager() {
        super(Spec.of("Config entry", () -> PolyConfig.CODEC)
                .wikiPage("Polytone-Configs")
                .folders("config_entries"));
        this.optionsFile = Minecraft.getInstance().gameDirectory.toPath().resolve("polytone_options.json").toFile();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadConfigFromDisk();
        registerBuiltins(configs);
    }

    private static OptionHolder<Boolean> builtinConfig(String id, boolean def, String section) {
        return OptionHolder.create(new BoolConfig(Optional.empty(), def, Optional.ofNullable(section)), Polytone.res(id));
    }

    private static OptionHolder<Float> builtinConfig(String id, float def, String section) {
        return builtinConfig(id, def, 0, 1, 0.01f, section);
    }

    private static OptionHolder<Float> builtinConfig(String id, float def, float min, float max, float step, String section) {
        return OptionHolder.create(new NumberConfig(Optional.empty(), def, min, max, step, Optional.ofNullable(section)), Polytone.res(id));
    }

    private static <E extends Enum<E> & StringRepresentable> OptionHolder<E> builtinConfig(String id, E def, String section) {
        return OptionHolder.create(new EnumConfig<>(def, Optional.ofNullable(section)), Polytone.res(id));
    }

    private void registerBuiltins(MapRegistry<OptionHolder<?>> reg) {
        for (OptionHolder<?> b : List.of(lenientLoading, legacyParsing, particlesThrottle, autoParticleRateLimit, particlesOffThread, showConfigButton, postShadersOccludeHeldItems, coloredLightsBackend, voxelVolumeWidth, voxelVolumeHeight, farLightRange, lightSpreadSteps, smoothEntityLighting)) {
            b.loadFromJson(configFileSnapshot);
            reg.unregister(b.fileId);
            reg.register(b.fileId, b);
        }
    }

    private static void addConfig(ResourceLocation id, PolyConfig<?> config, MapRegistry<OptionHolder<?>> reg, JsonObject dataJson) {
        OptionHolder<?> instance = OptionHolder.create(config, id);
        instance.loadFromJson(dataJson);
        reg.unregister(id);
        reg.register(id, instance);
    }

    public boolean checkAndClearNeedsPackReload() {
        return needsPackReload.getAndSet(false);
    }

    public boolean isEmpty() {
        return configs.isEmpty();
    }

    public boolean hasPackConfigs() {
        for (var option : configs.getValues()) {
            if (!option.fileId.getNamespace().equals(Polytone.MOD_ID)) return true;
        }
        return false;
    }

    public Screen createScreenForPack(PackSelectionScreen parent) {
        bubbleManager.onConfigOpened(hasPackConfigs());
        List<OptionHolder<?>> shown = shownOptions();
        return new ConfigScreen(parent, shown, () -> {
            if (shown.stream().noneMatch(OptionHolder::hasUnsavedChanges)) return;
            needsPackReload.set(true);
            saveConfigsToDisk(shown);
            // reloading packs here too would make it a double reload
            parent.reload();
        });
    }

    // for the mod list config buttons (neoforge mod menu, fabric mod menu), where there's no pack screen to reload
    public Screen createScreenForMainMenu(Screen parent) {
        bubbleManager.onConfigOpened(hasPackConfigs());
        List<OptionHolder<?>> shown = shownOptions();
        return new ConfigScreen(parent, shown, () -> {
            if (shown.stream().noneMatch(OptionHolder::hasUnsavedChanges)) return;
            saveConfigsToDisk(shown);
            Minecraft.getInstance().reloadResourcePacks();
        });
    }

    private List<OptionHolder<?>> shownOptions() {
        return List.copyOf(configs.getValues());
    }

    private void saveConfigsToDisk(Collection<OptionHolder<?>> edited) {
        try {
            JsonObject jsonObject = configFileSnapshot.deepCopy();
            for (var option : configs.getValues()) option.saveToJson(jsonObject);
            for (var option : edited) option.saveToJson(jsonObject);
            Path target = this.optionsFile.toPath();
            FilesUtil.writeTextAtomically(target, writer -> GsonHelper.writeValue(gson.newJsonWriter(writer), jsonObject, null));
            this.configFileSnapshot = jsonObject;
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

    public boolean getBooleanConfig(ResourceLocation id) {
        return getValue(id) instanceof Boolean b && b;
    }

    private MapRegistry<OptionHolder<?>> getActiveRegistry() {
        return Objects.requireNonNullElse(activeLoadConfigs.get(), configs);
    }

    public Object getValue(ResourceLocation configKey) {
        OptionHolder<?> value = getActiveRegistry().getValue(configKey);
        if (value == null) value = configs.getValue(configKey);
        if (value != null) {
            //special case for our enums
            return value.get() instanceof StringRepresentable sr ? sr.getSerializedName() : value.get();
        }
        Polytone.LOGGER.warn("Tried to get config value for unknown key: {}", configKey);
        return 0;
    }

    public void loadCurrentPackConfigs(PackResources primary, Pack.ResourcesSupplier resources, PackLocationInfo location, int version) {
        PackSource source = primary.location().source();
        if (source == PackSource.BUILT_IN || source == PackSource.FEATURE) return;

        MapRegistry<OptionHolder<?>> activePackReg = new MapRegistry<>("Active Pack Configs");
        registerBuiltins(activePackReg);
        activeLoadConfigs.set(activePackReg);
        parsePackConfigsInto(primary, activePackReg);

        List<String> overlays = collectFormatOverlays(primary, version);
        if (overlays.isEmpty()) return;

        try (PackResources fullPack = resources.openFull(location, new Pack.Metadata(Component.empty(),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), overlays))) {
            parsePackConfigsInto(fullPack, activePackReg);
        }
    }

    private void parsePackConfigsInto(PackResources pack, MapRegistry<OptionHolder<?>> reg) {
        MultiPackResourceManager resourceManager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(pack));
        var jsons = this.getJsonsInDirectories(resourceManager);
        for (var j : Parsed.batchParseOnlyEnabled(jsons, PolyConfig.CODEC, JsonOps.INSTANCE, "Configs")) {
            if (j != null) addConfig(j.getKey(), j.getValue(), reg, configFileSnapshot);
        }
    }

    public void clearCurrentPackConfigs() {
        activeLoadConfigs.remove();
    }

    private static List<String> collectFormatOverlays(PackResources primary, int version) {
        List<String> overlays = new ArrayList<>();
        try {
            OverlayMetadataSection section = primary.getMetadataSection(OverlayMetadataSection.TYPE);
            if (section != null) overlays.addAll(section.overlaysForVersion(version));
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to read overlay metadata while loading configs for pack {}", primary.location().id(), e);
        }
        return overlays;
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        parseConfigs(jsons);
    }

    @Override
    protected void parseWithoutLevel(AssetsFiles resources) {
        var jsons = resources.jsons();
        parseConfigs(jsons);
    }

    private void parseConfigs(Map<ResourceLocation, JsonElement> jsons) {
        configs.clear();
        registerBuiltins(configs);
        for (var j : Parsed.batchParseOnlyEnabled(jsons, PolyConfig.CODEC, JsonOps.INSTANCE, "Configs")) {
            if (j != null) addConfig(j.getKey(), j.getValue(), configs, configFileSnapshot);
        }
        Polytone.LOGGER.info("Loaded {} Polytone config entries", configs.size());
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
    }

    public void beforeRepositoryRefresh() {
        loadConfigFromDisk();
    }

    public boolean isLenientLoading() {
        return lenientLoading.get();
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
        return (CompatHandler.EMF || CompatHandler.ETF) ? ButtonPosition.LEFT : ButtonPosition.RIGHT;
    }
}
