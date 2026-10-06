package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.content.shaders.light.PolyShaderPointLights;
import net.mehvahdjukaar.polytone.content.shaders.shadow.ShadowMapManager;
import net.mehvahdjukaar.polytone.content.shaders.voxel.VoxelVolumeManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PolytoneBuiltInUniformsSet extends HashSet<String> {

    private static final Set<String> DYNAMIC = ConcurrentHashMap.newKeySet();

    static {
        DYNAMIC.add(PolyGlobalUniforms.BLOCK_NAME);
        DYNAMIC.add(ShadowMapManager.BLOCK_NAME);
        DYNAMIC.add(VoxelVolumeManager.BLOCK_NAME);
        DYNAMIC.add(PolyShaderPointLights.BLOCK_NAME);
    }

    public static void register(String name) {
        DYNAMIC.add(name);
    }

    public static Set<String> dynamicNames() {
        return DYNAMIC;
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
