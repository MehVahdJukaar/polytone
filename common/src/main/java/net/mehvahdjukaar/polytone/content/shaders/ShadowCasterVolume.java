package net.mehvahdjukaar.polytone.content.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The set of positions that can cast a shadow into what the camera can actually see, used to cull the
 * shadow pass. Everything is camera-relative (camera at the origin), matching the space the shadow
 * matrices and Sodium's culling coordinates already live in.
 *
 * <p>Two nested tests, cheap first:
 * <ol>
 *   <li>the orthographic light box itself - half-width {@code coverage} on the two light-lateral axes,
 *       {@code depthRange} along the light axis. This is what the shadow map physically covers.</li>
 *   <li>optionally, the <i>caster volume</i>: the camera frustum swept away from the light. An occluder
 *       only matters if its shadow ray lands inside the view frustum, so anything outside this volume
 *       can be dropped even though it sits inside the light box. Typically prunes everything behind and
 *       below the camera.</li>
 * </ol>
 *
 * <p>The caster volume is only valid for the frame it was built from, so it is skipped when the map is
 * being reused across frames (see {@link ShadowMapRenderer}); a stale volume would leave holes where the
 * camera has since turned to look.
 */
public final class ShadowCasterVolume {

    // Slack, in blocks, added to every caster-volume plane. The resolve shader samples the map at
    // reconstructed positions of visible fragments, and PCF/bias can push a lookup slightly past the
    // frustum edge; a small margin keeps those lookups backed by real occluder depth.
    private static final float EDGE_MARGIN = 4f;

    private static final int MAX_PLANES = 24;

    // Light basis (rows of the light view matrix) = light-space axes in camera-relative world coords.
    private final float xx, xy, xz;
    private final float yx, yy, yz;
    private final float zx, zy, zz;
    private final float halfLateral;
    private final float halfDepth;

    // Caster-volume half-spaces, packed as [nx, ny, nz, d]; a point is inside when n.p + d >= 0.
    // Empty when the volume is disabled, leaving just the light box.
    private final float[] planes = new float[MAX_PLANES * 4];
    private int planeCount;

    ShadowCasterVolume(Matrix4f lightView, float coverage, float depthRange) {
        this.xx = lightView.m00(); this.xy = lightView.m10(); this.xz = lightView.m20();
        this.yx = lightView.m01(); this.yy = lightView.m11(); this.yz = lightView.m21();
        this.zx = lightView.m02(); this.zy = lightView.m12(); this.zz = lightView.m22();
        this.halfLateral = coverage;
        this.halfDepth = depthRange;
    }

    /**
     * Builds the caster volume from the camera's view-projection and the direction toward the light.
     * Both must be in the same camera-relative space this volume is tested in.
     */
    void buildCasterPlanes(Matrix4f cameraViewProjection, Vector3f towardLight) {
        planeCount = 0;

        // Camera frustum half-spaces, inward normals (Gribb-Hartmann: row3 +/- row_i of the clip matrix).
        float[][] frustum = new float[6][];
        frustum[0] = normalized(cameraViewProjection.m03() + cameraViewProjection.m00(),
                cameraViewProjection.m13() + cameraViewProjection.m10(),
                cameraViewProjection.m23() + cameraViewProjection.m20(),
                cameraViewProjection.m33() + cameraViewProjection.m30()); // left
        frustum[1] = normalized(cameraViewProjection.m03() - cameraViewProjection.m00(),
                cameraViewProjection.m13() - cameraViewProjection.m10(),
                cameraViewProjection.m23() - cameraViewProjection.m20(),
                cameraViewProjection.m33() - cameraViewProjection.m30()); // right
        frustum[2] = normalized(cameraViewProjection.m03() + cameraViewProjection.m01(),
                cameraViewProjection.m13() + cameraViewProjection.m11(),
                cameraViewProjection.m23() + cameraViewProjection.m21(),
                cameraViewProjection.m33() + cameraViewProjection.m31()); // bottom
        frustum[3] = normalized(cameraViewProjection.m03() - cameraViewProjection.m01(),
                cameraViewProjection.m13() - cameraViewProjection.m11(),
                cameraViewProjection.m23() - cameraViewProjection.m21(),
                cameraViewProjection.m33() - cameraViewProjection.m31()); // top
        frustum[4] = normalized(cameraViewProjection.m03() + cameraViewProjection.m02(),
                cameraViewProjection.m13() + cameraViewProjection.m12(),
                cameraViewProjection.m23() + cameraViewProjection.m22(),
                cameraViewProjection.m33() + cameraViewProjection.m32()); // near
        frustum[5] = normalized(cameraViewProjection.m03() - cameraViewProjection.m02(),
                cameraViewProjection.m13() - cameraViewProjection.m12(),
                cameraViewProjection.m23() - cameraViewProjection.m22(),
                cameraViewProjection.m33() - cameraViewProjection.m32()); // far

        for (float[] plane : frustum) {
            if (plane == null) return; // degenerate matrix - fall back to the light box alone
        }

        // A point inside the frustum, used to orient the edge planes. NDC origin maps to the middle of
        // the frustum, which is strictly interior for any non-degenerate projection.
        Vector3f interior = new Vector3f();
        try {
            new Matrix4f(cameraViewProjection).invert().transformProject(interior);
        } catch (RuntimeException e) {
            return; // non-invertible - light box only
        }
        if (!interior.isFinite()) return;

        // An occluder p shadows the frustum F when p - s*L lands in F for some s >= 0 (L points toward
        // the light, so the light travels along -L). For half-space n.p + d >= 0 that needs
        // n.p + d >= s * (n.L) to hold for some s >= 0: when n.L < 0 the right side falls away without
        // bound and the plane never constrains anything, so only planes with n.L >= 0 survive.
        boolean[] kept = new boolean[6];
        for (int i = 0; i < 6; i++) {
            float[] p = frustum[i];
            kept[i] = p[0] * towardLight.x + p[1] * towardLight.y + p[2] * towardLight.z >= 0f;
            if (kept[i]) addPlane(p[0], p[1], p[2], p[3] + EDGE_MARGIN);
        }

        // Keeping only those planes leaves the volume open sideways. Close it along the frustum's
        // silhouette: every edge between a kept and a dropped face, swept into a plane parallel to the
        // light. Frustum faces are adjacent unless they are the opposing pair on the same axis.
        for (int a = 0; a < 6; a++) {
            if (!kept[a]) continue;
            for (int b = 0; b < 6; b++) {
                if (kept[b] || (a ^ 1) == b) continue;
                addEdgePlane(frustum[a], frustum[b], towardLight, interior);
            }
        }
    }

    // Plane through the intersection line of two faces, parallel to the light direction.
    private void addEdgePlane(float[] a, float[] b, Vector3f towardLight, Vector3f interior) {
        // Direction of the shared edge.
        float ex = a[1] * b[2] - a[2] * b[1];
        float ey = a[2] * b[0] - a[0] * b[2];
        float ez = a[0] * b[1] - a[1] * b[0];
        float lenSq = ex * ex + ey * ey + ez * ez;
        if (lenSq < 1.0E-9f) return; // parallel faces, no edge

        // Point on that line closest to the origin: (h_a (n_b x e) + h_b (e x n_a)) / |e|^2, with
        // h = -d for the n.p + d = 0 form used here.
        float ha = -a[3];
        float hb = -b[3];
        float px = (ha * (b[1] * ez - b[2] * ey) + hb * (ey * a[2] - ez * a[1])) / lenSq;
        float py = (ha * (b[2] * ex - b[0] * ez) + hb * (ez * a[0] - ex * a[2])) / lenSq;
        float pz = (ha * (b[0] * ey - b[1] * ex) + hb * (ex * a[1] - ey * a[0])) / lenSq;

        // Normal is perpendicular to both the edge and the light, so the plane contains the whole sweep.
        float nx = ey * towardLight.z - ez * towardLight.y;
        float ny = ez * towardLight.x - ex * towardLight.z;
        float nz = ex * towardLight.y - ey * towardLight.x;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-5f) return; // edge runs along the light; it casts no silhouette
        nx /= len; ny /= len; nz /= len;

        float d = -(nx * px + ny * py + nz * pz);
        // Orient so the frustum (and therefore the whole swept volume) is on the positive side.
        if (nx * interior.x + ny * interior.y + nz * interior.z + d < 0f) {
            nx = -nx; ny = -ny; nz = -nz; d = -d;
        }
        addPlane(nx, ny, nz, d + EDGE_MARGIN);
    }

    private void addPlane(float nx, float ny, float nz, float d) {
        if (planeCount >= MAX_PLANES) return;
        int i = planeCount * 4;
        planes[i] = nx;
        planes[i + 1] = ny;
        planes[i + 2] = nz;
        planes[i + 3] = d;
        planeCount++;
    }

    private static float[] normalized(float nx, float ny, float nz, float d) {
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-8f) return null;
        return new float[]{nx / len, ny / len, nz / len, d / len};
    }

    /**
     * Conservative test of a camera-relative box given as centre plus per-axis half-extents. Never
     * reports a false "outside", which is what a caster set needs.
     */
    public boolean intersects(float cx, float cy, float cz, float ex, float ey, float ez) {
        // Light box first: three dot products, and it rejects the overwhelming majority.
        if (Math.abs(xx * cx + xy * cy + xz * cz) > halfLateral + (Math.abs(xx) * ex + Math.abs(xy) * ey + Math.abs(xz) * ez)) return false;
        if (Math.abs(yx * cx + yy * cy + yz * cz) > halfLateral + (Math.abs(yx) * ex + Math.abs(yy) * ey + Math.abs(yz) * ez)) return false;
        if (Math.abs(zx * cx + zy * cy + zz * cz) > halfDepth + (Math.abs(zx) * ex + Math.abs(zy) * ey + Math.abs(zz) * ez)) return false;

        for (int i = 0, n = planeCount * 4; i < n; i += 4) {
            float nx = planes[i], ny = planes[i + 1], nz = planes[i + 2];
            float radius = Math.abs(nx) * ex + Math.abs(ny) * ey + Math.abs(nz) * ez;
            if (nx * cx + ny * cy + nz * cz + planes[i + 3] + radius < 0f) return false;
        }
        return true;
    }
}
