package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Preconditions;
import com.mojang.serialization.*;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

// String Optional codec
public class StrOpt {

    public static <A> MapCodec<Optional<A>> of(Codec<A> elementCodec, String name) {
        return new StrictOptionalFieldCodec(name, elementCodec);
    }

    public static <A> MapCodec<A> of(Codec<A> elementCodec, String name, A fallback) {
        return of(elementCodec, name).xmap(
                (optional) -> optional.orElse(fallback),
                (object2) -> Objects.equals(object2, fallback) ? Optional.empty() : Optional.of(object2));
    }


    @Deprecated(forRemoval = true, since = "1.20.4")
    static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
        private final String name;
        private final Codec<A> elementCodec;

        public StrictOptionalFieldCodec(String name, Codec<A> elementCodec) {
            this.name = name;
            this.elementCodec = Preconditions.checkNotNull(elementCodec);
        }

        public <T> DataResult<Optional<A>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
            T object = mapLike.get(this.name);
            return object == null ? DataResult.success(Optional.empty()) : this.elementCodec.parse(dynamicOps, object).map(Optional::of);
        }

        public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return input.isPresent() ? prefix.add(this.name, this.elementCodec.encodeStart(ops, input.get())) : prefix;
        }

        public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
            return Stream.of(dynamicOps.createString(this.name));
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof StrictOptionalFieldCodec<?> s)) {
                return false;
            } else {
                return Objects.equals(this.name, s.name) && Objects.equals(this.elementCodec, s.elementCodec);
            }
        }

        public int hashCode() {
            return Objects.hash(this.name, this.elementCodec);
        }

        public String toString() {
            return "StrictOptionalFieldCodec[" + this.name + ": " + this.elementCodec + "]";
        }
    }
}
