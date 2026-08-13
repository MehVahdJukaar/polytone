package net.mehvahdjukaar.polytone.content.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.client.Options.genericValueLabel;

public class OptionHolder<T> {

    public final OptionInstance<T> option;
    public final ResourceLocation fileId;
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

    public boolean hasUnsavedChanges() {
        return !option.get().equals(lastSavedValue.get());
    }

    public void undoChanges() {
        option.set(lastSavedValue.get());
    }

    public void resetToDefault() {
        if (option.values() instanceof PolyConfig<T> c) {
            option.set(c.getDefaultValue());
        }
    }

    public static <T> OptionHolder<T> create(PolyConfig<T> config, ResourceLocation id) {
        String valueTranslationKey = config.getValueTranslationKey().orElse(null);
        AtomicReference<T> lastSaved = new AtomicReference<>(config.getDefaultValue());

        // Entries with a preview image or impact line get their full tooltip rendered by
        // ConfigScreen; suppress the built-in text tooltip for those so the two don't stack.
        boolean customRendered = !config.getTooltipImages().isEmpty()
                || config.getPerformanceImpact().isPresent();
        OptionInstance.TooltipSupplier<T> tooltipSupplier = customRendered
                ? OptionInstance.noTooltip()
                : OptionInstance.cachedConstantTooltip(Component.translatable(id.toLanguageKey("config", "tooltip")));

        var opt = new OptionInstance<>(id.toLanguageKey("config"),
                tooltipSupplier,
                (component, value) -> {
                    MutableComponent valueName = formatValue(id, config, valueTranslationKey, value);
                    // Vanilla-style: an unsaved value reads in aqua until saved (or undone).
                    if (!lastSaved.get().equals(value)) valueName.withStyle(ChatFormatting.AQUA);
                    return genericValueLabel(component, valueName);
                },
                config,
                config.codec(), config.getDefaultValue(), (v) -> {});
        return new OptionHolder<>(opt, id, lastSaved);
    }

    // precedence: per-value lang key (config.<ns>.<path>.<value>), then value_translation used as a
    // format string, then the config type's own formatting
    private static <T> MutableComponent formatValue(ResourceLocation id, PolyConfig<T> config,
                                                    @Nullable String valueTranslationKey, T value) {
        String perValueKey = id.toLanguageKey("config") + "." + value;
        if (I18n.exists(perValueKey)) {
            return Component.translatable(perValueKey);
        }
        if (valueTranslationKey != null) {
            return Component.translatable(valueTranslationKey, value);
        }
        return config.formatValue(value);
    }
}
