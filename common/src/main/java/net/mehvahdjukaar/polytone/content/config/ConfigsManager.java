package net.mehvahdjukaar.polytone.content.config;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.FilesUtil;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO: make these per pack instead? since we load configs per pack. only issue is that for expressions these arent per pack...
public class ConfigsManager extends JsonPartialReloader {

    public final OptionHolder<Boolean> lenientLoading = builtinConfig("lenient_loading", false);
    public final OptionHolder<Boolean> legacyParsing = builtinConfig("legacy_parsing", true);
    public final OptionHolder<Float> particlesThrottle = builtinConfig("particles_throttle", 1);

    private static @NonNull OptionHolder<Boolean> builtinConfig(String id, boolean def) {
        return OptionHolder.create(new BoolConfig(Optional.empty(), Map.of(), 1, def), Polytone.res(id));
    }

    private static @NonNull OptionHolder<Float> builtinConfig(String id, float def) {
        return OptionHolder.create(new NumberConfig(Optional.empty(), Map.of(), 1,
                def, 0, 1, 0), Polytone.res(id));
    }

    private final MapRegistry<OptionHolder<?>> configs = new MapRegistry<>("Configs");
    private final ThreadLocal<MapRegistry<OptionHolder<?>>> activeLoadConfigs = new ThreadLocal<>(); // from active packs
    private final File optionsFile;
    private final Gson gson;

    private JsonObject configFileSnapshot;

    private final AtomicBoolean needsPackReload = new AtomicBoolean(false); //mega ugly

    public ConfigsManager() {
        super("config_entries");
        this.optionsFile = PlatStuff.getGamePath().resolve("polytone_options.json").toFile();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        // Only time we read disk automatically
        loadConfigFromDisk();
    }

    private static void addConfig(Identifier id, PolyConfig<?> config,
                                  MapRegistry<OptionHolder<?>> reg,
                                  JsonObject dataJson) {
        OptionHolder<?> instance = OptionHolder.create(config, id);

        // Initialize from last saved state (not disk every time!)
        instance.loadFromJson(dataJson);

        reg.unregister(id);
        reg.register(id, instance);
    }

    public boolean checkAndClearNeedsPackReload() {
        return needsPackReload.getAndSet(false);
    }

    public Screen createScreen(PackSelectionScreen parent) {
        return new ConfigScreen(parent, configs.getValues(), () -> {
            boolean anyChanged = configs.getValues()
                    .stream().anyMatch(OptionHolder::checkAndClearUpdated);
            if (anyChanged) {
                needsPackReload.set(true);
                saveConfigsToDisk();
                //save values we just set so we can read them again right here
                parent.reload();
                //we cant just reload packs here as this would cause a double reload.
            }
        });
    }

    private void saveConfigsToDisk() {
        try {
            JsonObject jsonObject = new JsonObject();

            for (var option : configs.getValues()) {
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
    public void loadCurrentPackConfigs(PackResources packResources, PackType packType) {
        //gets called every time the pack repository list is updated
        if (packType != PackType.CLIENT_RESOURCES) return;
        if (packResources.location().source() != PackSource.DEFAULT) return;
        //this is overall still quite fast. we shouldnt't have overhead at all, not more than loading these normally
        MultiPackResourceManager resourceManager = new MultiPackResourceManager(packType, List.of(packResources));

        var jsons = this.getJsonsInDirectories(resourceManager);

        MapRegistry<OptionHolder<?>> activePackReg = new MapRegistry<>("Active Pack Configs");
        activeLoadConfigs.set(activePackReg);
        for (var j : Parsed.batchParseOnlyEnabled(jsons, PolyConfig.CODEC,
                JsonOps.INSTANCE, "Configs")) {
            if (j != null) {
                Identifier id = j.getKey();
                PolyConfig<?> config = j.getValue();
                addConfig(id, config, activePackReg, configFileSnapshot);
            }
        }
    }

    public boolean isLenientLoading() {
        return lenientLoading.get();
    }

    @Override
    protected void applyNormal(Map<Identifier, JsonElement> obj) {
        ConfigScreen.clearPresetCache();
        saveConfigsToDisk();

        activeLoadConfigs.remove();
        configs.clear();
        configs.register(lenientLoading.fileId, lenientLoading);
        configs.register(legacyParsing.fileId, legacyParsing);
        configs.register(particlesThrottle.fileId, particlesThrottle);

        Map<Identifier, PolyConfig<?>> parsed = new HashMap<>();
        //ignoring conditions here purposefully
        for (var j : Parsed.batchParseOnlyEnabled(obj, PolyConfig.CODEC,
                JsonOps.INSTANCE, "Configs")) {
            PolyConfig<?> p = j.getValue();
            parsed.put(j.getKey(), p);
        }
        //first parse all to prevent recursive configs altering ourselves. Will throw a warn if a condition is used inside a condition json
        for (var entry : parsed.entrySet()) {
            addConfig(entry.getKey(), entry.getValue(), configs, configFileSnapshot);
        }
        Polytone.LOGGER.info("Loaded {} Polytone config entries", configs.size());
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
        if (configs.isEmpty()) return ButtonPosition.NONE;
        return (CompatHandler.EMF || CompatHandler.ETF) ? ButtonPosition.LEFT : ButtonPosition.RIGHT;
    }
}
