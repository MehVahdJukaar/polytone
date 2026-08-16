package net.mehvahdjukaar.polytone.content.shaders.sodium;

import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.mehvahdjukaar.polytone.content.shaders.ShadowCasterVolume;
import org.joml.FrustumIntersection;

final class SodiumLightVolumeFrustum implements Frustum {

    private final ShadowCasterVolume volume;
    private final float sectionRadius;

    SodiumLightVolumeFrustum(ShadowCasterVolume volume, float sectionRadius) {
        this.volume = volume;
        this.sectionRadius = sectionRadius;
    }

    @Override
    public boolean testSection(float x, float y, float z) {
        return volume.intersects(x, y, z, sectionRadius, sectionRadius, sectionRadius);
    }

    @Override
    public boolean testSectionExpanded(float x, float y, float z, float extend) {
        float e = sectionRadius + extend;
        return volume.intersects(x, y, z, e, e, e);
    }

    @Override
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return volume.intersects((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f,
                (maxX - minX) * 0.5f, (maxY - minY) * 0.5f, (maxZ - minZ) * 0.5f);
    }

    @Override
    public int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        // never INSIDE, so the tree traversal keeps testing children
        return testAab(minX, minY, minZ, maxX, maxY, maxZ) ? FrustumIntersection.INTERSECT : FrustumIntersection.OUTSIDE;
    }
}
