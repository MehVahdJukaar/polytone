package net.mehvahdjukaar.polytone.content.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// The light's ortho box, optionally narrowed to the camera frustum swept away from the light. Camera-relative.
public final class ShadowCasterVolume {

    private static final float EDGE_MARGIN = 4f;

    private static final int MAX_PLANES = 24;
    // JOML order NX, PX, NY, PY, NZ, PZ: the opposing face of i is i ^ 1
    private static final int FRUSTUM_FACES = 6;

    private final Vector3f lightRight;
    private final Vector3f lightUp;
    private final Vector3f lightForward;
    private final float halfLateral;
    private final float halfDepth;

    private final float[] planes = new float[MAX_PLANES * 4];
    private int planeCount;

    ShadowCasterVolume(Matrix4f lightView, float coverage, float depthRange) {
        this.lightRight = new Vector3f(lightView.m00(), lightView.m10(), lightView.m20());
        this.lightUp = new Vector3f(lightView.m01(), lightView.m11(), lightView.m21());
        this.lightForward = new Vector3f(lightView.m02(), lightView.m12(), lightView.m22());
        this.halfLateral = coverage;
        this.halfDepth = depthRange;
    }

    void buildCasterPlanes(Matrix4f cameraViewProjection, Vector3f towardLight) {
        planeCount = 0;

        Vector4f[] faces = new Vector4f[FRUSTUM_FACES];
        for (int i = 0; i < FRUSTUM_FACES; i++) {
            Vector4f face = cameraViewProjection.frustumPlane(i, new Vector4f());
            if (!face.isFinite()) return;
            faces[i] = face;
        }

        Vector3f interior = cameraViewProjection.invert(new Matrix4f()).transformProject(new Vector3f());
        if (!interior.isFinite()) return;

        // A point p casts into the frustum when p - s*L is inside it for some s >= 0. For a half-space n.p + d >= 0
        // that is only a constraint when n.L >= 0; the other faces never reject anything.
        boolean[] facesLight = new boolean[FRUSTUM_FACES];
        for (int i = 0; i < FRUSTUM_FACES; i++) {
            Vector4f face = faces[i];
            facesLight[i] = dotNormal(face, towardLight) >= 0f;
            if (facesLight[i]) addPlane(face.x, face.y, face.z, face.w + EDGE_MARGIN);
        }

        for (int a = 0; a < FRUSTUM_FACES; a++) {
            if (!facesLight[a]) continue;
            for (int b = 0; b < FRUSTUM_FACES; b++) {
                boolean opposingFace = (a ^ 1) == b;
                if (facesLight[b] || opposingFace) continue;
                addEdgePlane(faces[a], faces[b], towardLight, interior);
            }
        }
    }

    private void addEdgePlane(Vector4f a, Vector4f b, Vector3f towardLight, Vector3f interior) {
        Vector3f normalA = new Vector3f(a.x, a.y, a.z);
        Vector3f normalB = new Vector3f(b.x, b.y, b.z);
        Vector3f edge = normalA.cross(normalB, new Vector3f());
        float edgeLenSq = edge.lengthSquared();
        if (edgeLenSq < 1.0E-9f) return;

        Vector3f point = normalB.cross(edge, new Vector3f()).mul(-a.w)
                .fma(-b.w, edge.cross(normalA, new Vector3f()))
                .div(edgeLenSq);

        Vector3f normal = edge.cross(towardLight, new Vector3f());
        if (normal.lengthSquared() < 1.0E-10f) return; // edge runs along the light, no silhouette
        normal.normalize();

        float d = -normal.dot(point);
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

    public boolean intersects(float cx, float cy, float cz, float ex, float ey, float ez) {
        if (outsideSlab(lightRight, halfLateral, cx, cy, cz, ex, ey, ez)) return false;
        if (outsideSlab(lightUp, halfLateral, cx, cy, cz, ex, ey, ez)) return false;
        if (outsideSlab(lightForward, halfDepth, cx, cy, cz, ex, ey, ez)) return false;

        for (int i = 0, n = planeCount * 4; i < n; i += 4) {
            float nx = planes[i], ny = planes[i + 1], nz = planes[i + 2];
            float radius = Math.abs(nx) * ex + Math.abs(ny) * ey + Math.abs(nz) * ez;
            if (nx * cx + ny * cy + nz * cz + planes[i + 3] + radius < 0f) return false;
        }
        return true;
    }

    private static boolean outsideSlab(Vector3f axis, float halfWidth,
                                       float cx, float cy, float cz, float ex, float ey, float ez) {
        float radius = Math.abs(axis.x) * ex + Math.abs(axis.y) * ey + Math.abs(axis.z) * ez;
        return Math.abs(axis.x * cx + axis.y * cy + axis.z * cz) > halfWidth + radius;
    }
}
