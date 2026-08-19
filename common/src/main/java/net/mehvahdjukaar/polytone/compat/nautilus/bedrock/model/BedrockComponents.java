package net.mehvahdjukaar.polytone.compat.nautilus.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.DiagnosticSink;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// Keys have the minecraft: prefix stripped. Values stay Dynamic until someone asks for a specific type, so a
// broken component becomes a diagnostic instead of failing the whole file.
public record BedrockComponents(Map<String, Dynamic<?>> raw) {

    public static final BedrockComponents EMPTY = new BedrockComponents(Map.of());

    public static final Codec<BedrockComponents> CODEC = Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
            .xmap(BedrockComponents::stripNamespaces, BedrockComponents::raw);

    private static BedrockComponents stripNamespaces(Map<String, Dynamic<?>> map) {
        Map<String, Dynamic<?>> out = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            int colon = key.indexOf(':');
            out.put(colon < 0 ? key : key.substring(colon + 1), value);
        });
        return new BedrockComponents(out);
    }

    public boolean has(BedrockComponentType<?> type) {
        return raw.containsKey(type.id());
    }

    public <T> Optional<T> get(BedrockComponentType<T> type, DiagnosticSink sink) {
        Dynamic<?> value = raw.get(type.id());
        if (value == null) return Optional.empty();
        DataResult<T> parsed = type.codec().parse(value);
        parsed.error().ifPresent(error -> sink.error(type.id(), error.message()));
        return parsed.result();
    }

    public void reportUnknown(DiagnosticSink sink) {
        for (String key : raw.keySet()) {
            if (!BedrockComponentTypes.isKnown(key)) {
                sink.warn(key, "Unknown component, ignored");
            }
        }
    }
}
