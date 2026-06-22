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
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Parses pack-defined configs at {@code polytone/config_entries/*.json}, persists user-set values to
 * {@code polytone_options.json}, and exposes them to pack overlay conditions and {@code config("ns:id")}
 * expressions. Configs are loaded eagerly per-pack at pack-discovery time (see PackMixin) so overlay
 * conditions can be evaluated before the resource reload.
 */
public class ConfigsManager extends JsonPartialReloader {

    public final OptionHolder<Boolean> lenientLoading = builtinConfig("lenient_loading", false);
    public final OptionHolder<Boolean> legacyParsing = builtinConfig("legacy_parsing", true);
    public final OptionHolder<Float> particlesThrottle = builtinConfig("particles_throttle", 1f);
    public final OptionHolder<Boolean> autoParticleRateLimit = builtinConfig("auto_particle_rate_limit", false);
    public final OptionHolder<Boolean> particlesOffThread = builtinConfig("custom_particles_async", false);

    public final ConfigBubbleManager bubbleManager = new ConfigBubbleManager();

    private final MapRegistry<OptionHolder<?>> configs = new MapRegistry<>("Configs");
    private final ThreadLocal<MapRegistry<OptionHolder<?>>> activeLoadConfigs = new ThreadLocal<>();
    private final File optionsFile;
    private final Gson gson;
    private JsonObject configFileSnapshot = new JsonObject();
    private final AtomicBoolean needsPackReload = new AtomicBoolean(false);

    public ConfigsManager() {
        super("config_entries");
        this.optionsFile = Minecraft.getInstance().gameDirectory.toPath().resolve("polytone_options.json").toFile();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadConfigFromDisk();
    }

    private static OptionHolder<Boolean> builtinConfig(String id, boolean def) {
        return OptionHolder.create(new BoolConfig(Optional.empty(), def), Polytone.res(id));
    }

    private static OptionHolder<Float> builtinConfig(String id, float def) {
        return OptionHolder.create(new NumberConfig(Optional.empty(), def, 0, 1, 0.01f), Polytone.res(id));
    }

    private void registerBuiltins(MapRegistry<OptionHolder<?>> reg) {
        for (OptionHolder<?> b : List.of(lenientLoading, legacyParsing, particlesThrottle, autoParticleRateLimit, particlesOffThread)) {
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

    public Screen createScreen(PackSelectionScreen parent) {
        bubbleManager.onConfigOpened(hasPackConfigs());
        return new ConfigScreen(parent, configs.getValues(), () -> {
            boolean anyChanged = configs.getValues().stream().anyMatch(OptionHolder::checkAndClearUpdated);
            if (anyChanged) {
                needsPackReload.set(true);
                saveConfigsToDisk();
                parent.reload();
            }
        });
    }

    private void saveConfigsToDisk() {
        try {
            JsonObject jsonObject = new JsonObject();
            for (var option : configs.getValues()) option.saveToJson(jsonObject);
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
    public void loadCurrentPackConfigs(PackResources packResources) {
        if (packResources.location().source() != PackSource.DEFAULT) return;
        MultiPackResourceManager resourceManager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(packResources));
        var jsons = this.getJsonsInDirectories(resourceManager);

        MapRegistry<OptionHolder<?>> activePackReg = new MapRegistry<>("Active Pack Configs");
        registerBuiltins(activePackReg);
        activeLoadConfigs.set(activePackReg);
        for (var j : Parsed.batchParseOnlyEnabled(jsons, PolyConfig.CODEC, JsonOps.INSTANCE, "Configs")) {
            if (j != null) addConfig(j.getKey(), j.getValue(), activePackReg, configFileSnapshot);
        }
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
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
        if (configs.isEmpty()) return ButtonPosition.NONE;
        return (CompatHandler.EMF || CompatHandler.ETF) ? ButtonPosition.LEFT : ButtonPosition.RIGHT;
    }
}
