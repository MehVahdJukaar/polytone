package net.mehvahdjukaar.polytone.content.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.client.Options.genericValueLabel;

public class OptionHolder<T> {

    public final OptionInstance<T> option;
    public final Identifier fileId;
    private final AtomicBoolean updated;

    OptionHolder(OptionInstance<T> option, Identifier registryID, AtomicBoolean updated) {
        this.option = option;
        this.fileId = registryID;
        this.updated = updated;
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

    public boolean checkAndClearUpdated() {
        return updated.getAndSet(false);
    }

    public static <T> OptionHolder<T> create(PolyConfig<T> config, Identifier id) {
        String valueTranslationKey = config.getValueTranslationKey();
        AtomicBoolean updated = new AtomicBoolean(false);
        var opt = new OptionInstance<>(id.toLanguageKey("config"),
                OptionInstance.cachedConstantTooltip(
                        Component.translatable(id.toLanguageKey("config", "tooltip"))),
                (component, value) -> genericValueLabel(component,
                        valueTranslationKey == null ? Component.literal(value + "") :
                                Component.translatable(valueTranslationKey, value)),
                config,
                config.codec(), config.getDefaultValue(), (v) -> {
            updated.set(true);
        }
        );
        return new OptionHolder<>(opt, id, updated);
    }
}
