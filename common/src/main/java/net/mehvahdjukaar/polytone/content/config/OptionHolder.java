package net.mehvahdjukaar.polytone.content.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Optional;
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
    }

    public void updateLastSavedData() {
        lastSavedValue.set(option.get());
    }

    public boolean hasUnsavedChanges() {
        return !option.get().equals(lastSavedValue.get());
    }

    public static <T> OptionHolder<T> create(PolyConfig<T> config, Identifier id) {
        AtomicReference<T> updated = new AtomicReference<T>(config.getDefaultValue());

        OptionInstance.CaptionBasedToString<T> toStr = (name, value) -> {
            Optional<String> valueTranslationKey = config.getValueTranslationKey();

            MutableComponent valueName = valueTranslationKey.map(s -> Component.translatable(s, value))
                    .orElseGet(() -> Component.literal(value + ""));
            if (updated.get() != value) valueName.withStyle(ChatFormatting.AQUA);
            return Options.genericValueLabel(name, valueName);
        };

        var opt = new OptionInstance<>(id.toLanguageKey("config"),
                OptionInstance.cachedConstantTooltip(Component.translatable(id.toLanguageKey("config", "tooltip"))),
                toStr, config,
                config.codec(), config.getDefaultValue(), (v) ->  {}
        );
        return new OptionHolder<>(opt, id, updated);
    }

}
