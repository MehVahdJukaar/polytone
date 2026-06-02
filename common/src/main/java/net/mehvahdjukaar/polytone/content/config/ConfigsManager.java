package net.mehvahdjukaar.polytone.content.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * 1.21.1 stub of the 1.21.11 config system.
 *
 * <p>Parses pack-defined configs at {@code polytone/config_entries/*.json} so other JSONs in the
 * same pack that reference them (e.g. via expression {@code config("ns:id")}) can load. Always
 * returns the JSON-declared default — there is no UI and no persistence on this branch.</p>
 */
public class ConfigsManager extends JsonPartialReloader {

    public final OptionHolder<Boolean> lenientLoading = builtinConfig("lenient_loading", false);
    public final OptionHolder<Boolean> legacyParsing = builtinConfig("legacy_parsing", true);
    public final OptionHolder<Float> particlesThrottle = builtinConfig("particles_throttle", 1f);
    public final OptionHolder<Boolean> autoParticleRateLimit = builtinConfig("auto_particle_rate_limit", false);
    public final OptionHolder<Boolean> particlesOffThread = builtinConfig("custom_particles_async", false);

    private final MapRegistry<OptionHolder<?>> configs = new MapRegistry<>("Configs");

    public ConfigsManager() {
        super("config_entries");
    }

    private static OptionHolder<Boolean> builtinConfig(String id, boolean def) {
        return OptionHolder.create(new BoolConfig(Optional.empty(), Map.of(), 1, def), Polytone.res(id));
    }

    private static OptionHolder<Float> builtinConfig(String id, float def) {
        return OptionHolder.create(new NumberConfig(Optional.empty(), Map.of(), 1, def, 0, 1, 0.01f), Polytone.res(id));
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        configs.clear();
        configs.register(lenientLoading.fileId, lenientLoading);
        configs.register(legacyParsing.fileId, legacyParsing);
        configs.register(particlesThrottle.fileId, particlesThrottle);
        configs.register(autoParticleRateLimit.fileId, autoParticleRateLimit);
        configs.register(particlesOffThread.fileId, particlesOffThread);

        for (var e : jsons.entrySet()) {
            ResourceLocation id = e.getKey();
            var result = PolyConfig.CODEC.parse(JsonOps.INSTANCE, e.getValue());
            if (result.isError()) {
                Polytone.LOGGER.warn("Failed to parse config '{}': {}", id, result.error().get().message());
                continue;
            }
            registerParsed(id, result.getOrThrow());
        }

        Polytone.LOGGER.info("Loaded {} Polytone config entries (stub - defaults only)", configs.size());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerParsed(ResourceLocation id, PolyConfig<?> config) {
        OptionHolder<?> holder = OptionHolder.create((PolyConfig) config, id);
        configs.unregister(id);
        configs.register(id, holder);
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
    }

    public Object getValue(ResourceLocation configKey) {
        OptionHolder<?> value = configs.getValue(configKey);
        if (value != null) return value.get();
        Polytone.LOGGER.warn("Tried to get config value for unknown key: {}", configKey);
        return 0;
    }

    public boolean isLenientLoading() {
        return lenientLoading.get();
    }
}
