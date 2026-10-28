package net.mehvahdjukaar.polytone.content.shaders;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PolytoneBuiltInUniformsSet extends HashSet<String> {

    private static final Set<String> DYNAMIC = ConcurrentHashMap.newKeySet();
    private static final Set<String> DYNAMIC_SAMPLERS = ConcurrentHashMap.newKeySet();

    public static void register(String name) {
        DYNAMIC.add(name);
    }

    //programs linked before this call wont see it
    public static void registerSampler(String name) {
        DYNAMIC_SAMPLERS.add(name);
    }

    public static Set<String> dynamicSamplers() {
        return DYNAMIC_SAMPLERS;
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
