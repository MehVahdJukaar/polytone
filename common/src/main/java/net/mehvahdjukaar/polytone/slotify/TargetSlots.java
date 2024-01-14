package net.mehvahdjukaar.polytone.slotify;


import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.ExtraCodecs;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public interface TargetSlots {
    Iterable<Integer> getSlots();

    Codec<TargetSlots> CODEC = new Codec<>() {

        final Codec<SingleTarget> c1 = ExtraCodecs.NON_NEGATIVE_INT.xmap(SingleTarget::new, SingleTarget::slot);
        final Codec<ListTarget> c2 = ExtraCodecs.NON_NEGATIVE_INT.listOf().xmap(ListTarget::new, ListTarget::slots);
        final Codec<RangeTarget> c3 = Codec.STRING.comapFlatMap(RangeTarget::read, RangeTarget::toString);

        @Override
        public <T> DataResult<T> encode(TargetSlots input, DynamicOps<T> ops, T prefix) {
            if (input instanceof SingleTarget t) {
                return c1.encode(t, ops, prefix);
            } else if (input instanceof ListTarget t) {
                return c2.encode(t, ops, prefix);
            } else if (input instanceof RangeTarget t) {
                return c3.encode(t, ops, prefix);
            } else {
                throw new IllegalArgumentException("Unsupported implementation type: " + input.getClass());
            }
        }

        @Override
        public <T> DataResult<Pair<TargetSlots, T>> decode(DynamicOps<T> ops, T input) {
            TargetSlots target = null;
            var r1 = c1.decode(ops, input).result();
            if (r1.isEmpty()) {
                var r2 = c2.decode(ops, input).result();
                if (r2.isEmpty()) {
                    var r3 = c3.decode(ops, input).result();
                    if (r3.isPresent()) target = r3.get().getFirst();
                } else target = r2.get().getFirst();
            } else target = r1.get().getFirst();
            if (target != null) return DataResult.success(Pair.of(target, input));
            return DataResult.error(()->"Failed to decode SlotTarget. Must either be an int, an array or a range (i.e. '3->7')");
        }
    };

    record RangeTarget(int min, int max) implements TargetSlots {

        @Override
        public Iterable<Integer> getSlots() {
            return IntStream.rangeClosed(min, max)::iterator;
        }

        @Override
        public String toString() {
            return min + "->" + max;
        }

        public static DataResult<RangeTarget> read(String input) {
            String[] parts = input.split("->");
            if (parts.length != 2) {
                return DataResult.error(()->"Invalid format. Expected format: 'xOffset->yOffset'");
            }
            try {
                int num1 = Integer.parseInt(parts[0]);
                int num2 = Integer.parseInt(parts[1]);
                if(num1 <0 || num2 < 0) return DataResult.error(()->"Slots must be positive");
                if (num2 <= num1) return DataResult.error(()->"Invalid range, min must be smaller than max");
                return DataResult.success(new RangeTarget(num1, num2));
            } catch (NumberFormatException e) {
                return DataResult.error(()->"Invalid number format. Both numbers should be integers.");
            }
        }
    }

    record ListTarget(List<Integer> slots) implements TargetSlots {
        @Override
        public Iterable<Integer> getSlots() {
            return slots;
        }
    }

    record SingleTarget(int slot) implements TargetSlots {

        @Override
        public Iterable<Integer> getSlots() {
            return Collections.singleton(slot);
        }
    }
}