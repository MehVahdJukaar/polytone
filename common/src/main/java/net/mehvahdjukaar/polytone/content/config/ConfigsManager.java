package net.mehvahdjukaar.polytone.content.config;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.utils.FilesUtil;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.Parsed;
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
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

// Loaded eagerly per pack at discovery time (see PackMixin) so overlay conditions can be evaluated
// before the reload, and unlike the other reloaders these also parse with no world open, since that's
// where the pack screen reads them.
public class ConfigsManager extends JsonPartialReloader<PolyConfig<?>> {

    public final OptionHolder<Boolean> lenientLoading = builtinConfig("lenient_loading", false);
    public final OptionHolder<Boolean> legacyParsing = builtinConfig("legacy_parsing", true);
    public final OptionHolder<Float> particlesThrottle = builtinConfig("particles_throttle", 1f);
    public final OptionHolder<Boolean> autoParticleRateLimit = builtinConfig("auto_particle_rate_limit", false);
    public final OptionHolder<Boolean> particlesOffThread = builtinConfig("custom_particles_async", false);
    public final OptionHolder<Boolean> showConfigButton = builtinConfig("show_config_button", true);
    public final OptionHolder<Boolean> postShadersOccludeHeldItems = builtinConfig("post_shaders_occlude_held_items", true);

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

    private static OptionHolder<Boolean> builtinConfig(String id, boolean def) {
        return OptionHolder.create(new BoolConfig(Optional.empty(), def), Polytone.res(id));
    }

    private static OptionHolder<Float> builtinConfig(String id, float def) {
        return OptionHolder.create(new NumberConfig(Optional.empty(), def, 0, 1, 0.01f), Polytone.res(id));
    }

    private void registerBuiltins(MapRegistry<OptionHolder<?>> reg) {
        for (OptionHolder<?> b : List.of(lenientLoading, legacyParsing, particlesThrottle, autoParticleRateLimit, particlesOffThread, showConfigButton, postShadersOccludeHeldItems)) {
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

    // Detached from the live registry on purpose: a reload while the screen is open clears configs and
    // rebuilds every holder, while the screen's widgets keep writing into these. Iterating the registry
    // at save time would then look at holders nobody touched and drop the user's edits.
    private List<OptionHolder<?>> shownOptions() {
        return List.copyOf(configs.getValues());
    }

    private void saveConfigsToDisk(Collection<OptionHolder<?>> edited) {
        try {
            // Start from what's already on disk: entries whose pack isn't currently loaded (disabled,
            // temporarily removed, or failed to parse) have no holder here, and writing a fresh object
            // would wipe their saved values for good.
            JsonObject jsonObject = configFileSnapshot.deepCopy();
            for (var option : configs.getValues()) option.saveToJson(jsonObject);
            // last, so edits made on holders the registry has since replaced win
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
        this.configFileSnapshot = jo == null ? new JsonObject() : jo;
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
        if (value != null) return value.get();
        Polytone.LOGGER.warn("Tried to get config value for unknown key: {}", configKey);
        return 0;
    }

    // Called per-pack during pack discovery, before the resource reload happens, so overlay
    // conditions referencing this pack's configs can be evaluated.
    // Two passes, and the order matters: reading the overlay metadata below already decodes the pack's
    // overlay entries, which evaluates their polytone_condition and so looks configs up. Seeding the
    // registry from the pack's base directory first is what makes those lookups resolve. Only then can
    // we re-open the pack with its applicable overlays, to also pick up config entries declared inside
    // one (e.g. a version overlay), which are invisible to the first pass.
    public void loadCurrentPackConfigs(PackResources primary, Pack.ResourcesSupplier resources, PackLocationInfo location, int version) {
        // Server packs and world packs need this just as much as local ones (#372); only the
        // vanilla/mod-provided packs are worth skipping, they can't carry config entries.
        PackSource source = primary.location().source();
        if (source == PackSource.BUILT_IN || source == PackSource.FEATURE) return;

        MapRegistry<OptionHolder<?>> activePackReg = new MapRegistry<>("Active Pack Configs");
        registerBuiltins(activePackReg);
        activeLoadConfigs.set(activePackReg);
        parsePackConfigsInto(primary, activePackReg);

        List<String> overlays = collectFormatOverlays(primary, version);
        if (overlays.isEmpty()) return;

        PackResources fullPack = resources.openFull(location, new Pack.Metadata(Component.empty(),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), overlays));
        try {
            parsePackConfigsInto(fullPack, activePackReg);
        } finally {
            fullPack.close();
        }
    }

    // The resource manager is deliberately not closed: closing it would close the pack we were handed,
    // which the caller still owns and goes on using.
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

    // Overlay directories that apply for this pack version. Config definitions almost always live in
    // plain format/version overlays, so this is enough to make them visible before the real reload.
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
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        parseConfigs(jsons);
    }

    // Config entries decode with plain JsonOps and never touch registries, so unlike every other
    // reloader they can load with no world open - which they must, since the pack screen and pack
    // overlay conditions both read them from the main menu.
    @Override
    protected void parseWithoutLevel(Map<ResourceLocation, JsonElement> jsons) {
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
