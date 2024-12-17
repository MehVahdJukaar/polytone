package net.mehvahdjukaar.polytone.utils;

import java.util.*;

public class ListUtils {

    public static <T> Set<T> mergeSet(Set<T> first, Set<T> second) {
        var set = new HashSet<T>();
        set.addAll(first);
        set.addAll(second);
        return Collections.unmodifiableSet(set);
    }

    public static <T> List<T> mergeList(List<? extends T> first, List<? extends T> second) {
        var list = new ArrayList<T>();
        list.addAll(first);
        list.addAll(second);
        return Collections.unmodifiableList(list);
    }

}
