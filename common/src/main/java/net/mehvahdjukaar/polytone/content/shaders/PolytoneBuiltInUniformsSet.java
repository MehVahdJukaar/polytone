package net.mehvahdjukaar.polytone.content.shaders;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PolytoneBuiltInUniformsSet extends HashSet<String> {

    private static final Set<String> DYNAMIC = ConcurrentHashMap.newKeySet();

    public static void register(String name) {
        DYNAMIC.add(name);
    }

    public PolytoneBuiltInUniformsSet(HashSet<String> initial) {
        super(initial);
    }

    @Override
    public boolean contains(Object o) {
        if (super.contains(o)) return true;
        return o instanceof String s && DYNAMIC.contains(s);
    }
}
