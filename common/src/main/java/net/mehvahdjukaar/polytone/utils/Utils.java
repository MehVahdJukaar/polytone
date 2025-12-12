package net.mehvahdjukaar.polytone.utils;

import net.minecraft.resources.Identifier;

import java.util.*;

public class Utils {

    public static <T> Set<T> mergeSet(Set<T> first, Set<T> second) {
        var set = new HashSet<T>();
        set.addAll(first);
        set.addAll(second);
        return Collections.unmodifiableSet(set);
    }

    public static <T> Optional<List<T>> mergeList(Optional<List<T>> newObj, Optional<List<T>> oldObj) {
        if (newObj.isPresent() && oldObj.isPresent()) {
            return Optional.of(mergeList(newObj.get(), oldObj.get()));
        } else if (newObj.isPresent()) {
            return newObj;
        } else return oldObj;
    }

    public static <T> List<T> mergeList(List<? extends T> newObj, List<? extends T> oldObj) {
        var list = new ArrayList<T>();
        list.addAll(oldObj);
        list.addAll(newObj);
        return Collections.unmodifiableList(list);
    }

    public static <T, V> Map<T, V> mergedMap(Map<T, V> newObj, Map<T, V> oldObj) {
        var map = new HashMap<T, V>();
        map.putAll(oldObj);
        map.putAll(newObj);
        return Collections.unmodifiableMap(map);
    }

    //inverse ordered alphabetical map for resource locations
    public static <T> Map<Identifier, T> sortedMap() {
        return new TreeMap<>(Comparator.comparing(Identifier::toString).reversed());
    }


}
