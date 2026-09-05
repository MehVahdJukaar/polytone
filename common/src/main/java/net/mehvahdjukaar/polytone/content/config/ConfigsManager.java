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
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.util.GsonHelper;
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
    public final OptionHolder<Boolean> postChainsAfterHand = builtinConfig("post_chains_after_hand", null, true);
    public final OptionHolder<Boolean> skyDepthWrite = builtinConfig("sky_depth_write", null, false);

    public final ConfigBubbleManager bubbleManager = new ConfigBubbleManager();

    private static @NonNull OptionHolder<Boolean> builtinConfig(String id, @Nullable String section, boolean def) {
        return OptionHolder.create(new BoolConfig(Optional.empty(), Map.of(), Map.of(), 1,
                Optional.ofNullable(section), Optional.empty(), Optional.empty(), false, Map.of(), def), Polytone.res(id));
    }

    private static @NonNull OptionHolder<Float> builtinConfig(String id, @Nullable String section, float def) {
        return OptionHolder.create(new NumberConfig(Optional.empty(), Map.of(), Map.of(), 1,
                Optional.ofNullable(section), Optional.empty(), Optional.empty(), false, Map.of(), def, 0, 1, 0.01f), Polytone.res(id));
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

    private void registerBuiltins(MapRegistry<OptionHolder<?>> reg) {
        for (OptionHolder<?> b : List.of(lenientLoading, legacyParsing, particlesThrottle, autoParticleRateLimit,
                particlesOffThread, showConfigButton, postChainsAfterHand, skyDepthWrite)) {
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
                            Minecraft.getInstance().gui.setScreen(createScreenForPack(parent, packReload));
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
        if (value != null) {
            return value.get();
        } else {
            Polytone.LOGGER.warn("Tried to get config value for unknown key: {}", configKey);
            return 0;
        }
    }

    //is this pack active or not? we dont know
    //called one pack at the time. we cant do IO there, we rely on the cache
    public void loadCurrentPackConfigs(PackResources primary, Pack.ResourcesSupplier resources,
                                       PackLocationInfo location, PackFormat version, PackType packType) {
        //gets called every time the pack repository list is updated
        if (packType != PackType.CLIENT_RESOURCES) return;
        // server packs and world packs need this just as much as local ones (#372); only the
        // vanilla/mod-provided packs are worth skipping, they can't carry config entries
        PackSource source = primary.location().source();
        if (source == PackSource.BUILT_IN || source == PackSource.FEATURE) return;

        //this is overall still quite fast. we shouldnt't have overhead at all, not more than loading these normally
        MapRegistry<OptionHolder<?>> activePackReg = new MapRegistry<>("Active Pack Configs");
        registerBuiltins(activePackReg);
        activeLoadConfigs.set(activePackReg);
        parsePackConfigsInto(primary, packType, activePackReg);

        // reading the overlay section evaluates every require_config on it, so it has to happen after
        // the registry above is live. don't reorder these.
        List<String> overlays = collectFormatOverlays(primary, packType, version);
        if (overlays.isEmpty()) return;

        // config entries living inside an overlay directory are invisible to openPrimary, so re-open
        // the pack with its format overlays applied and parse those too
        PackResources fullPack = resources.openFull(location, new Pack.Metadata(Component.empty(),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), overlays));
        try {
            parsePackConfigsInto(fullPack, packType, activePackReg);
        } finally {
            fullPack.close();
        }
    }

    // The resource manager is deliberately not closed: closing it would close the pack we were handed,
    // which the caller still owns and goes on using.
    private void parsePackConfigsInto(PackResources pack, PackType packType, MapRegistry<OptionHolder<?>> reg) {
        MultiPackResourceManager resourceManager = new MultiPackResourceManager(packType, List.of(pack));
        var jsons = this.getJsonsInDirectories(resourceManager);
        for (var j : parseEnabledJsons(jsons, JsonOps.INSTANCE)) {
            if (j != null) {
                addConfig(j.getKey(), (PolyConfig<?>) j.getValue(), reg, configFileSnapshot);
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
        registerDevTestConfigs();
        Polytone.LOGGER.info("Loaded {} Polytone config entries", configs.size());
    }

    private void registerDevTestConfigs() {
        if (!Polytone.isDevEnv) return;

        Map<String, Boolean> boolPresets = Map.of("enabled", true, "disabled", false);
        Map<String, Float> floatPresets = Map.of("low", 0.25f, "high", 0.75f);

        addConfig(Identifier.fromNamespaceAndPath("test_pack_alpha", "dev_alpha_toggle"),
                new BoolConfig(Optional.empty(), boolPresets, Map.of(), 0,
                        Optional.of("alpha_group_a"), Optional.of(0), Optional.empty(), false, Map.of(), true),
                configs, configFileSnapshot);
        addConfig(Identifier.fromNamespaceAndPath("test_pack_alpha", "dev_alpha_throttle"),
                new NumberConfig(Optional.empty(), floatPresets, Map.of("section_low", 0.1f), 1,
                        Optional.of("alpha_group_a"), Optional.of(0), Optional.of(PolyConfig.PerformanceImpact.MEDIUM),
                        false, Map.of(), 0.5f, 0, 1, 0.05f),
                configs, configFileSnapshot);
        addConfig(Identifier.fromNamespaceAndPath("test_pack_alpha", "dev_wide_note"),
                new StringConfig(Optional.empty(), Map.of(), Map.of(), 0,
                        Optional.of("alpha_group_b"), Optional.of(1), Optional.empty(), true, Map.of(),
                        "balanced", List.of("balanced", "fast", "fancy")),
                configs, configFileSnapshot);

        addConfig(Identifier.fromNamespaceAndPath("test_pack_beta", "dev_sectionless"),
                new BoolConfig(Optional.empty(), Map.of(), Map.of(), 0,
                        Optional.empty(), Optional.empty(), Optional.of(PolyConfig.PerformanceImpact.LOW),
                        false, Map.of(), false),
                configs, configFileSnapshot);
        addConfig(Identifier.fromNamespaceAndPath("test_pack_beta", "dev_grouped_flag"),
                new BoolConfig(Optional.empty(), boolPresets, Map.of("off", false), 0,
                        Optional.of("beta_misc"), Optional.of(0), Optional.of(PolyConfig.PerformanceImpact.HIGH),
                        false, Map.of(), true),
                configs, configFileSnapshot);

        addConfig(Polytone.res("dev_polytone_extra"),
                new BoolConfig(Optional.empty(), Map.of(), Map.of(), 99,
                        Optional.of("particles"), Optional.empty(), Optional.empty(), false, Map.of(), false),
                configs, configFileSnapshot);
    }

    public void beforeRepositoryRefresh() {
        loadConfigFromDisk();
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
