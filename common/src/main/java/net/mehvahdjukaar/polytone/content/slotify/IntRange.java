package net.mehvahdjukaar.polytone.content.slotify;


import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public interface IntRange {
    Iterable<Integer> getValues();

    Codec<IntRange> CODEC = new Codec<>() {

        final Codec<SingleTarget> INT_CODEC = ExtraCodecs.NON_NEGATIVE_INT.xmap(SingleTarget::new, SingleTarget::value);
        final Codec<ListTarget> LIST_CODEC = ExtraCodecs.NON_NEGATIVE_INT.listOf().xmap(ListTarget::new, ListTarget::values);
        final Codec<RangeTarget> RANGE_CODEC = Codec.STRING.comapFlatMap(RangeTarget::read, RangeTarget::toString);

        @Override
        public <T> DataResult<T> encode(IntRange input, DynamicOps<T> ops, T prefix) {
            if (input instanceof SingleTarget t) {
                return INT_CODEC.encode(t, ops, prefix);
            } else if (input instanceof ListTarget t) {
                return LIST_CODEC.encode(t, ops, prefix);
            } else if (input instanceof RangeTarget t) {
                return RANGE_CODEC.encode(t, ops, prefix);
            } else {
                throw new IllegalArgumentException("Unsupported implementation type: " + input.getClass());
            }
        }

        @Override
        public <T> DataResult<Pair<IntRange, T>> decode(DynamicOps<T> ops, T input) {
            IntRange target = null;
            var r1 = INT_CODEC.decode(ops, input).result();
            if (r1.isEmpty()) {
                var r2 = LIST_CODEC.decode(ops, input).result();
                if (r2.isEmpty()) {
                    var r3 = RANGE_CODEC.decode(ops, input).result();
                    if (r3.isPresent()) target = r3.get().getFirst();
                } else target = r2.get().getFirst();
            } else target = r1.get().getFirst();
            if (target != null) return DataResult.success(Pair.of(target, input));
            return DataResult.error(() -> "Failed to decode SlotTarget. Must either be an int, an array or a range (i.e. '3->7')");
        }
    };

    default boolean has(int value) {
        for (int slot : getValues()) {
            if (slot == value) return true;
        }
        return false;
    }

    record RangeTarget(int min, int max) implements IntRange {

        @Override
        public Iterable<Integer> getValues() {
            return IntStream.rangeClosed(min, max)::iterator;
        }

        @Override
        public String toString() {
            return min + "->" + max;
        }

        public static DataResult<RangeTarget> read(String input) {
            String[] parts = input.split("->");
            if (parts.length != 2) {
                return DataResult.error(() -> "Invalid format. Expected format: 'xOffset->yOffset'");
            }
            try {
                int num1 = Integer.parseInt(parts[0]);
                int num2 = Integer.parseInt(parts[1]);
                if (num1 < 0 || num2 < 0) return DataResult.error(() -> "Slots must be positive");
                if (num2 <= num1) return DataResult.error(() -> "Invalid range, min must be smaller than max");
                return DataResult.success(new RangeTarget(num1, num2));
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid number format. Both numbers should be values.");
            }
        }
    }

    record ListTarget(List<Integer> values) implements IntRange {
        @Override
        public Iterable<Integer> getValues() {
            return values;
        }
    }

    record SingleTarget(int value) implements IntRange {

        @Override
        public Iterable<Integer> getValues() {
            return Collections.singleton(value);
        }
    }

    static IntRange merge(IntRange a, IntRange b) {
        List<Integer> list = new ArrayList<>();
        a.getValues().forEach(list::add);
        b.getValues().forEach(list::add);
        return new ListTarget(list);
    }
}