package net.mehvahdjukaar.polytone.content.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicReference;

public class OptionHolder<T> {

    public final OptionInstance<T> option;
    public final Identifier fileId;
    private final AtomicReference<T> lastSavedValue;

    OptionHolder(OptionInstance<T> option, Identifier registryID, AtomicReference<T> updated) {
        this.option = option;
        this.fileId = registryID;
        this.lastSavedValue = updated;
    }

    public T get() {
        return option.get();
    }

    public void saveToJson(JsonObject target) {
        JsonElement je = option.codec().encodeStart(JsonOps.INSTANCE, option.get())
                .getOrThrow();
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

    public boolean hasUnsavedChanges() {
        return !option.get().equals(lastSavedValue.get());
    }

    public static <T> OptionHolder<T> create(PolyConfig<T> config, Identifier id) {
        AtomicReference<T> lastKnownValue = new AtomicReference<>(config.getDefaultValue());

        OptionInstance.CaptionBasedToString<T> toStr = (name, value) -> {
            MutableComponent valueName;
            // Per-value label by convention on the config's own key, no JSON field needed:
            // "config.recrafted.recrafted_gui.true": "Recrafted" / ".false": "Vanilla".
            String perValueKey = id.toLanguageKey("config") + "." + value;
            if (Language.getInstance().has(perValueKey)) {
                valueName = Component.translatable(perValueKey);
            } else if (config.getValueTranslationKey().isPresent()) {
                // Pre-existing value_translation: key used as a format string with the raw value.
                valueName = Component.translatable(config.getValueTranslationKey().get(), value);
            } else {
                // Default: ON/OFF for booleans, numeric/string label otherwise.
                valueName = config.formatValue(value);
            }
            if (!lastKnownValue.get().equals(value)) valueName.withStyle(ChatFormatting.AQUA);
            return Options.genericValueLabel(name, valueName);
        };

        MutableComponent tooltip = Component.translatable(id.toLanguageKey("config", "tooltip"));

        boolean customRendered = !config.getTooltipImages().isEmpty()
                || config.getPerformanceImpact().isPresent();
        OptionInstance.TooltipSupplier<T> tooltipSupplier = customRendered
                ? OptionInstance.noTooltip()
                : OptionInstance.cachedConstantTooltip(tooltip);

        var opt = new OptionInstance<>(id.toLanguageKey("config"),
                tooltipSupplier,
                toStr, config,
                config.codec(), config.getDefaultValue(),
                // lets the config screen re-derive its preset sliders when values change
                (v) -> ConfigScreen.onOptionValueChanged()
        );
        return new OptionHolder<>(opt, id, lastKnownValue);
    }

    public void undoChanges() {
        option.set(lastSavedValue.get());
    }

    public void resetToDefault() {
        if (option.values() instanceof PolyConfig<T> c) {
            option.set(c.getDefaultValue());
        }
    }
}
