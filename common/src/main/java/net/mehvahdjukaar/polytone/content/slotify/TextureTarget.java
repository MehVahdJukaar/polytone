package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.resources.Identifier;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface TextureTarget extends Predicate<Identifier> {

    Codec<TextureTarget> CODEC = Codec.lazyInitialized(()-> SchemaCodecs.alternatives(
            "id", OfId.ID_CODEC,
            "regex", ofRegex.REGEX_CODEC));

    record OfId(Identifier id) implements TextureTarget {

        public static final Codec<OfId> ID_CODEC = Identifier.CODEC.xmap(OfId::new, OfId::id);

        @Override
        public boolean test(Identifier identifier) {
            return identifier.equals(id);
        }
    }

    record ofRegex(Pattern regex) implements TextureTarget {
        public static final Codec<ofRegex> REGEX_CODEC = Codec.STRING.xmap(
                s -> new ofRegex(Pattern.compile(s)),
                r -> r.regex.pattern()
        );
        @Override
        public boolean test(Identifier identifier) {
            return regex.matcher(identifier.toString()).matches();
        }
    }

}
