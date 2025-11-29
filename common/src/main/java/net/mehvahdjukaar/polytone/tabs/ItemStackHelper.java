package net.mehvahdjukaar.polytone.tabs;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Set;

public class ItemStackHelper {

    public static boolean matchItemsLenientBytes(ItemStack stack, ItemStack other) {
        if (stack == other) {
            return true;
        } else {
            return stack.getCount() == other.getCount() && sameItemSameTagLenient(stack, other);
        }
    }

    private static boolean sameItemSameTagLenient(ItemStack stack, ItemStack other) {
        if (!stack.is(other.getItem())) {
            return false;
        } else {
            return stack.isEmpty() && other.isEmpty() || sameTagLenientBytes(stack.getTag(), other.getTag());
        }
    }

    public static boolean sameTagLenientBytes(CompoundTag tag1, CompoundTag tag2) {
        if (tag1 == tag2) return true;
        if (tag1 == null || tag2 == null) return false;

        Set<String> keys1 = tag1.getAllKeys();
        Set<String> keys2 = tag2.getAllKeys();
        if (!keys1.equals(keys2)) return false;

        for (String key : keys1) {
            Tag t1 = tag1.get(key);
            Tag t2 = tag2.get(key);
            if (!sameTagLenientBytesTag(t1, t2)) return false;
        }
        return true;
    }

    private static boolean sameTagLenientBytesTag(Tag t1, Tag t2) {
        if (t1 == t2) return true;
        if (t1 == null || t2 == null) return false;

        // numeric types: ByteTag, ShortTag, IntTag, LongTag, FloatTag, DoubleTag
        if (isNumericTag(t1) && isNumericTag(t2)) {
            return numericEqual((Number) getNumericValue(t1), (Number) getNumericValue(t2));
        }

        // Compound
        if (t1 instanceof CompoundTag && t2 instanceof CompoundTag) {
            return sameTagLenientBytes((CompoundTag) t1, (CompoundTag) t2);
        }

        // List
        if (t1 instanceof ListTag && t2 instanceof ListTag) {
            ListTag l1 = (ListTag) t1;
            ListTag l2 = (ListTag) t2;
            if (l1.size() != l2.size()) return false;
            for (int i = 0; i < l1.size(); i++) {
                if (!sameTagLenientBytesTag(l1.get(i), l2.get(i))) return false;
            }
            return true;
        }

        // Arrays
        if (t1 instanceof ByteArrayTag && t2 instanceof ByteArrayTag) {
            byte[] a1 = ((ByteArrayTag) t1).getAsByteArray();
            byte[] a2 = ((ByteArrayTag) t2).getAsByteArray();
            if (a1.length != a2.length) return false;
            for (int i = 0; i < a1.length; i++) if (a1[i] != a2[i]) return false;
            return true;
        }
        if (t1 instanceof IntArrayTag && t2 instanceof IntArrayTag) {
            int[] a1 = ((IntArrayTag) t1).getAsIntArray();
            int[] a2 = ((IntArrayTag) t2).getAsIntArray();
            if (a1.length != a2.length) return false;
            for (int i = 0; i < a1.length; i++) if (a1[i] != a2[i]) return false;
            return true;
        }
        if (t1 instanceof LongArrayTag && t2 instanceof LongArrayTag) {
            long[] a1 = ((LongArrayTag) t1).getAsLongArray();
            long[] a2 = ((LongArrayTag) t2).getAsLongArray();
            if (a1.length != a2.length) return false;
            for (int i = 0; i < a1.length; i++) if (a1[i] != a2[i]) return false;
            return true;
        }

        // Strings
        if (t1 instanceof StringTag && t2 instanceof StringTag) {
            return ((StringTag) t1).getAsString().equals(((StringTag) t2).getAsString());
        }

        // Fallback: use equals() (some Tag implementations override equals)
        return t1.equals(t2);
    }

    private static boolean isNumericTag(Tag t) {
        return (t instanceof ByteTag) ||
                (t instanceof ShortTag) ||
                (t instanceof IntTag) ||
                (t instanceof LongTag) ||
                (t instanceof FloatTag) ||
                (t instanceof DoubleTag);
    }

    // Return numeric value as the corresponding Number object.
    // NOTE: method names here follow common mappings: getAsByte/getAsShort/getAsInt/getAsLong/getAsFloat/getAsDouble.
    // If your environment uses different getters (e.g. getByte(), asInt(), etc.) adapt accordingly.
    private static Number getNumericValue(Tag t) {
        if (t instanceof ByteTag) return ((ByteTag) t).getAsByte();
        if (t instanceof ShortTag) return ((ShortTag) t).getAsShort();
        if (t instanceof IntTag) return ((IntTag) t).getAsInt();
        if (t instanceof LongTag) return ((LongTag) t).getAsLong();
        if (t instanceof FloatTag) return ((FloatTag) t).getAsFloat();
        if (t instanceof DoubleTag) return ((DoubleTag) t).getAsDouble();
        throw new IllegalArgumentException("Not a numeric tag: " + t);
    }

    // Compare numeric equality leniently:
    // - if both integral types (byte/short/int/long) compare as long
    // - otherwise compare as double (exact equality)
    private static boolean numericEqual(Number n1, Number n2) {
        boolean n1Integral = isIntegralNumber(n1);
        boolean n2Integral = isIntegralNumber(n2);

        if (n1Integral && n2Integral) {
            return n1.longValue() == n2.longValue();
        } else {
            // compare as double; exact equality
            return Double.compare(n1.doubleValue(), n2.doubleValue()) == 0;
        }
    }

    private static boolean isIntegralNumber(Number n) {
        return (n instanceof Byte) || (n instanceof Short) || (n instanceof Integer) || (n instanceof Long);
    }

}
