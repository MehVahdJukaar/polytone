package net.mehvahdjukaar.polytone.content.shaders.sodium;

import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

// A Sodium culling Frustum shaped like the shadow map's orthographic light volume: a box of half-width
// coverage on the two light-lateral axes and depthRange along the light axis, centered on the camera.
// Re-culls Sodium's terrain from the light's POV so occluders behind/beside the player still cast.
// Test coordinates arrive camera-relative (Sodium pre-subtracts the camera position), the same space the
// ortho box lives in, so only the light basis is needed. Same separating-axis test as
// ShadowMapRenderer.collectShadowSections; conservative (never a false "invisible"), as a caster set wants.
final class SodiumLightVolumeFrustum implements Frustum {

    // Rows of the light-view rotation = light-space axes expressed in (camera-relative) world coords.
    private final float xx, xy, xz; // lateral axis 1
    private final float yx, yy, yz; // lateral axis 2
    private final float zx, zy, zz; // light axis
    private final float halfXY;     // coverage (lateral half-extent)
    private final float halfZ;      // depth range (half-extent along the light axis)
    private final float sectionRadius;

    SodiumLightVolumeFrustum(Matrix4f lightView, float coverage, float depthRange, float sectionRadius) {
        this.xx = lightView.m00(); this.xy = lightView.m10(); this.xz = lightView.m20();
        this.yx = lightView.m01(); this.yy = lightView.m11(); this.yz = lightView.m21();
        this.zx = lightView.m02(); this.zy = lightView.m12(); this.zz = lightView.m22();
        this.halfXY = coverage;
        this.halfZ = depthRange;
        this.sectionRadius = sectionRadius;
    }

    private boolean intersects(float cx, float cy, float cz, float ex, float ey, float ez) {
        if (Math.abs(xx * cx + xy * cy + xz * cz) > halfXY + (Math.abs(xx) * ex + Math.abs(xy) * ey + Math.abs(xz) * ez)) return false;
        if (Math.abs(yx * cx + yy * cy + yz * cz) > halfXY + (Math.abs(yx) * ex + Math.abs(yy) * ey + Math.abs(yz) * ez)) return false;
        if (Math.abs(zx * cx + zy * cy + zz * cz) > halfZ + (Math.abs(zx) * ex + Math.abs(zy) * ey + Math.abs(zz) * ez)) return false;
        return true;
    }

    // Section tests get the section center; Sodium bakes the padded section radius in, so we do too.
    @Override
    public boolean testSection(float x, float y, float z) {
        return intersects(x, y, z, sectionRadius, sectionRadius, sectionRadius);
    }

    @Override
    public boolean testSectionExpanded(float x, float y, float z, float extend) {
        float e = sectionRadius + extend;
        return intersects(x, y, z, e, e, e);
    }

    @Override
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return intersects((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f,
                (maxX - minX) * 0.5f, (maxY - minY) * 0.5f, (maxZ - minZ) * 0.5f);
    }

    @Override
    public int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        // Report INTERSECT (never INSIDE) when visible so the tree traversal keeps testing children.
        return testAab(minX, minY, minZ, maxX, maxY, maxZ) ? FrustumIntersection.INTERSECT : FrustumIntersection.OUTSIDE;
    }
}
