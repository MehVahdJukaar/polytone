package net.mehvahdjukaar.polytone.content.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.client.Options.genericValueLabel;

public class OptionHolder<T> {

    public final OptionInstance<T> option;
    public final ResourceLocation fileId;
    /** Value last written to / read from disk; drives the "unsaved change" highlight + undo. */
    private final AtomicReference<T> lastSavedValue;

    private OptionHolder(OptionInstance<T> option, ResourceLocation registryID, AtomicReference<T> lastSavedValue) {
        this.option = option;
        this.fileId = registryID;
        this.lastSavedValue = lastSavedValue;
    }

    public T get() {
        return option.get();
    }

    public void saveToJson(JsonObject target) {
        JsonElement je = option.codec().encodeStart(JsonOps.INSTANCE, option.get()).getOrThrow();
        target.add(fileId.toString(), je);
        lastSavedValue.set(option.get());
    }

    public void loadFromJson(JsonObject json) {
        JsonElement element = json.get(fileId.toString());
        if (element == null) return;
        DataResult<T> result = option.codec().parse(JsonOps.INSTANCE, element);
        if (result.isSuccess()) {
            option.set(result.getOrThrow());
        } else {
            Polytone.LOGGER.error("Failed to load config option {}: {}", fileId, result.error().get().message());
        }
        lastSavedValue.set(option.get());
    }

    /** True while the current value differs from what's on disk (drives save-on-close + Undo). */
    public boolean hasUnsavedChanges() {
        return !option.get().equals(lastSavedValue.get());
    }

    /** Revert to the value on disk (the last saved/loaded state). */
    public void undoChanges() {
        option.set(lastSavedValue.get());
    }

    /** Reset to the config's declared default. */
    public void resetToDefault() {
        if (option.values() instanceof PolyConfig<T> c) {
            option.set(c.getDefaultValue());
        }
    }

    public static <T> OptionHolder<T> create(PolyConfig<T> config, ResourceLocation id) {
        String valueTranslationKey = config.getValueTranslationKey();
        AtomicReference<T> lastSaved = new AtomicReference<>(config.getDefaultValue());
        var opt = new OptionInstance<>(id.toLanguageKey("config"),
                OptionInstance.cachedConstantTooltip(Component.translatable(id.toLanguageKey("config", "tooltip"))),
                (component, value) -> {
                    MutableComponent valueName = formatValue(id, valueTranslationKey, value);
                    // Vanilla-style: an unsaved value reads in aqua until saved (or undone).
                    if (!lastSaved.get().equals(value)) valueName.withStyle(ChatFormatting.AQUA);
                    return genericValueLabel(component, valueName);
                },
                config,
                config.codec(), config.getDefaultValue(), (v) -> {});
        return new OptionHolder<>(opt, id, lastSaved);
    }

    /**
     * Value label shown on the option button, in precedence order:
     * per-value lang key ({@code config.<ns>.<path>.<value>}) → the {@code value_translation} key used
     * as a format string → the vanilla ON/OFF constants for booleans → the raw value as a literal.
     */
    private static MutableComponent formatValue(ResourceLocation id, @Nullable String valueTranslationKey, Object value) {
        String perValueKey = id.toLanguageKey("config") + "." + value;
        if (I18n.exists(perValueKey)) {
            return Component.translatable(perValueKey);
        }
        if (valueTranslationKey != null) {
            return Component.translatable(valueTranslationKey, value);
        }
        if (value instanceof Boolean b) {
            // Same constants vanilla uses for every boolean toggle (e.g. "Music: ON").
            return (b ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF).copy();
        }
        return Component.literal(String.valueOf(value));
    }
}
