package net.mehvahdjukaar.polytone.content.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The set of positions that can cast a shadow into what the camera can actually see, used to cull the
 * shadow pass. Everything is camera-relative (camera at the origin).
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
    // JOML plane order: NX, PX, NY, PY, NZ, PZ - opposing faces sit at adjacent indices, which the
    // silhouette loop below relies on.
    private static final int FRUSTUM_FACES = 6;

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

        // Camera frustum half-spaces, normalized with inward normals.
        Vector4f[] faces = new Vector4f[FRUSTUM_FACES];
        for (int i = 0; i < FRUSTUM_FACES; i++) {
            Vector4f face = cameraViewProjection.frustumPlane(i, new Vector4f());
            if (!face.isFinite()) return; // degenerate matrix - fall back to the light box alone
            faces[i] = face;
        }

        // A point inside the frustum, used to orient the edge planes. NDC origin maps to the middle of
        // the frustum, which is strictly interior for any non-degenerate projection.
        Vector3f interior = cameraViewProjection.invert(new Matrix4f()).transformProject(new Vector3f());
        if (!interior.isFinite()) return; // non-invertible - light box only

        // An occluder p shadows the frustum F when p - s*L lands in F for some s >= 0 (L points toward
        // the light, so the light travels along -L). For half-space n.p + d >= 0 that needs
        // n.p + d >= s * (n.L) to hold for some s >= 0: when n.L < 0 the right side falls away without
        // bound and the plane never constrains anything, so only planes with n.L >= 0 survive.
        boolean[] facesLight = new boolean[FRUSTUM_FACES];
        for (int i = 0; i < FRUSTUM_FACES; i++) {
            Vector4f face = faces[i];
            facesLight[i] = dotNormal(face, towardLight) >= 0f;
            if (facesLight[i]) addPlane(face.x, face.y, face.z, face.w + EDGE_MARGIN);
        }

        // Keeping only those planes leaves the volume open sideways. Close it along the frustum's
        // silhouette: every edge between a kept and a dropped face, swept into a plane parallel to the
        // light. Frustum faces are adjacent unless they are the opposing pair on the same axis.
        for (int a = 0; a < FRUSTUM_FACES; a++) {
            if (!facesLight[a]) continue;
            for (int b = 0; b < FRUSTUM_FACES; b++) {
                if (facesLight[b] || (a ^ 1) == b) continue;
                addEdgePlane(faces[a], faces[b], towardLight, interior);
            }
        }
    }

    // Plane through the intersection line of two faces, parallel to the light direction.
    private void addEdgePlane(Vector4f a, Vector4f b, Vector3f towardLight, Vector3f interior) {
        Vector3f na = new Vector3f(a.x, a.y, a.z);
        Vector3f nb = new Vector3f(b.x, b.y, b.z);
        Vector3f edge = na.cross(nb, new Vector3f());
        float edgeLenSq = edge.lengthSquared();
        if (edgeLenSq < 1.0E-9f) return; // parallel faces, no edge

        // Point on that line closest to the origin: (h_a (n_b x e) + h_b (e x n_a)) / |e|^2, with
        // h = -d for the n.p + d = 0 form used here.
        Vector3f point = nb.cross(edge, new Vector3f()).mul(-a.w)
                .fma(-b.w, edge.cross(na, new Vector3f()))
                .div(edgeLenSq);

        // Normal is perpendicular to both the edge and the light, so the plane contains the whole sweep.
        Vector3f normal = edge.cross(towardLight, new Vector3f());
        if (normal.lengthSquared() < 1.0E-10f) return; // edge runs along the light; it casts no silhouette
        normal.normalize();

        float d = -normal.dot(point);
        // Orient so the frustum (and therefore the whole swept volume) is on the positive side.
        if (normal.dot(interior) + d < 0f) {
            normal.negate();
            d = -d;
        }
        addPlane(normal.x, normal.y, normal.z, d + EDGE_MARGIN);
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

    private static float dotNormal(Vector4f plane, Vector3f v) {
        return plane.x * v.x + plane.y * v.y + plane.z * v.z;
    }

    /**
     * Conservative test of a camera-relative box given as centre plus per-axis half-extents. Never
     * reports a false "outside", which is what a caster set needs.
     */
    public boolean intersects(float cx, float cy, float cz, float ex, float ey, float ez) {
        // Light box first: three slab tests, and they reject the overwhelming majority.
        if (outsideSlab(xx, xy, xz, halfLateral, cx, cy, cz, ex, ey, ez)) return false;
        if (outsideSlab(yx, yy, yz, halfLateral, cx, cy, cz, ex, ey, ez)) return false;
        if (outsideSlab(zx, zy, zz, halfDepth, cx, cy, cz, ex, ey, ez)) return false;

        for (int i = 0, n = planeCount * 4; i < n; i += 4) {
            float nx = planes[i], ny = planes[i + 1], nz = planes[i + 2];
            float radius = Math.abs(nx) * ex + Math.abs(ny) * ey + Math.abs(nz) * ez;
            if (nx * cx + ny * cy + nz * cz + planes[i + 3] + radius < 0f) return false;
        }
        return true;
    }

    // Box against the slab of half-width `half` around the light axis (ax, ay, az): the box spans its
    // centre distance along that axis plus its half-extents projected onto it.
    private static boolean outsideSlab(float ax, float ay, float az, float half,
                                       float cx, float cy, float cz, float ex, float ey, float ez) {
        float radius = Math.abs(ax) * ex + Math.abs(ay) * ey + Math.abs(az) * ez;
        return Math.abs(ax * cx + ay * cy + az * cz) > half + radius;
    }
}
