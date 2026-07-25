#version 330

// Directional shadow resolve pass (Polytone 1.21.11 post pipeline).
//
// Reconstructs each screen pixel's camera-relative world position from the level depth, projects it
// into the light's clip space, and compares against the shadow depth map that ShadowMapRenderer
// rendered from the sun/moon's point of view. Pixels whose reconstructed position is further from
// the light than the nearest occluder are darkened.
//
// Acne handling: a flat constant bias can't cope with surfaces that sit edge-on to the light (walls
// under a high sun) - they self-shadow (z-fighting). We reconstruct the surface normal from the
// screen-space derivatives of the world position, then (a) push the sample point off the surface
// along that normal and (b) scale the depth bias by how edge-on the surface is to the light.
//
// Pixelated look: when PixelGridRes > 0 the (offset) sample position is snapped to a WORLD-ALIGNED
// grid of PixelGridRes cells per block before the shadow test. Every screen pixel inside the same
// grid cell then performs the identical test, so shadow edges land on the block-texture grid of the
// surface they fall on (no oblique shadow-map texels). Because Minecraft faces are axis-aligned, the
// normal offset only shifts the axis perpendicular to the face, and the visible grid on the face
// stays exactly texture-aligned. PolyShadowCamFract anchors the grid to absolute world space, so it
// does not swim as the camera moves. 16 = same size as default block texels; 32/48 = finer; 0 = off.

uniform sampler2D InSampler;        // scene colour (pass input "In")
uniform sampler2D InDepthSampler;   // level depth (pass input "InDepth" with use_depth_buffer)
uniform sampler2D InShadow;         // light-POV depth map (Polytone binds it by name)

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform PolyGlobals {
    mat4 PolyProjMat;               // camera projection
    mat4 PolyModelViewMat;          // camera view (rotation only; camera at origin)
    float PolySunAngle;
};

layout(std140) uniform PolyShadow {
    mat4 PolyShadowMat;             // light view-projection, camera-relative space
    vec3 PolyShadowLightDir;        // unit direction toward the light, camera-relative space
    vec3 PolyShadowCamFract;        // fract(cameraPos): world-grid anchor for camera-relative coords
};

// From expression_uniforms in polytone/post_chains/shadows.json
layout(std140) uniform ShadowStrength { float uShadowStrength; };  // 0..1, how dark full shadow gets
layout(std140) uniform ShadowBias     { float uShadowBias; };      // base depth bias (normalized light depth)
layout(std140) uniform NormalOffset   { float uNormalOffset; };    // sample offset along the surface normal, in blocks
layout(std140) uniform PixelGridRes   { float uPixelGridRes; };    // world-grid cells per block; 0 = smooth

void main() {
    // DEBUG (test pack only): draw the raw shadow depth map into the bottom-left corner so the
    // light-POV render can be inspected directly. DebugSize = fraction of the screen it occupies.
    const float DebugSize = 0.25;
    if (texCoord.x < DebugSize && texCoord.y < DebugSize) {
        float d = texture(InShadow, texCoord / DebugSize).r;
        // Ortho depth clusters in a narrow band; stretch it a bit so occluders are actually visible.
        float v = clamp((d - 0.5) * 4.0 + 0.5, 0.0, 1.0);
        fragColor = vec4(vec3(v), 1.0);
        return;
    }

    vec3 color = texture(InSampler, texCoord).rgb;
    float depth = texture(InDepthSampler, texCoord).r;

    // Cleared/sky depth (far plane) -> nothing to shadow.
    if (depth >= 1.0) {
        fragColor = vec4(color, 1.0);
        return;
    }

    // Screen -> camera-relative world position (camera sits at the origin, so PolyModelViewMat is
    // rotation-only and its inverse gives the world offset from the camera).
    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = inverse(PolyProjMat) * ndc;
    viewPos /= viewPos.w;
    vec3 worldRel = (inverse(PolyModelViewMat) * viewPos).xyz;

    // Reconstruct the surface normal from screen-space derivatives of the world position, oriented to
    // face the camera (which is at the origin in this space).
    vec3 ddx = dFdx(worldRel);
    vec3 ddy = dFdy(worldRel);
    vec3 normal = normalize(cross(ddx, ddy));
    if (dot(normal, worldRel) > 0.0) normal = -normal;

    // How head-on this surface is to the camera. At a SILHOUETTE - a cloud edge against the sky, or
    // any depth discontinuity - the 2x2 derivative quad straddles two depths, so the "surface" it
    // reconstructs lies almost along the view ray and the normal it yields is meaningless. Left
    // unchecked that garbage normal feeds the geometric term and paints a dark rim around distant
    // clouds. Measured on the RAW normal, before the axis snap, which would hide it.
    float normalTrust = smoothstep(0.05, 0.2, abs(dot(normal, normalize(worldRel))));

    // Minecraft surfaces are overwhelmingly axis-aligned, and this normal gets noisy far away (it is
    // built from a quantised depth buffer). Snap it to the dominant axis when that axis is clearly
    // dominant, so ndotl is exact per face; models and entities keep the raw normal.
    vec3 an = abs(normal);
    float dominant = max(an.x, max(an.y, an.z));
    if (dominant > 0.9) normal = normalize(step(dominant - 1e-4, an) * sign(normal));

    // World-space size of one screen pixel on this surface. It grows with distance and with grazing
    // view angles, and it is the natural unit for everything below: neighbouring pixels' sample
    // points sit this far apart, so any error smaller than it is invisible and any grid finer than it
    // aliases. Clamped so depth discontinuities (huge derivatives) can't blow it up.
    float footprint = min(max(length(ddx), length(ddy)), 2.0);

    // How the surface sits relative to the light. slope: 0 when facing it, ->1 when grazing/away.
    float ndotl = dot(normal, PolyShadowLightDir);
    float slope = clamp(1.0 - ndotl, 0.0, 1.0);

    // A face turned away from the light (ndotl <= 0) is in shadow by geometry alone, and the depth
    // map can never tell us that: nothing stands between it and the light, and what the light DOES
    // see along that ray is usually the same block's lit top or side face, which is FURTHER from the
    // light - so the depth compare reads "lit". That is the sunrise case: the back face of a block
    // gets shadowed low down (where the block's own front face projects over it) but stays bright
    // near the top (where the top face projects instead). Blended over a narrow band so the
    // terminator doesn't alias and derivative noise on block edges doesn't leave dark rims.
    float facing = smoothstep(0.0, 0.05, ndotl);

    // Scales read off the light matrix, so the world-space reasoning below stays correct whatever
    // coverage / depth_range / resolution the pack's shadow_map.json picks. Row 0 and row 2 of the
    // light view-projection give suv.x and suv.z per world block (the 0.5 folds NDC into [0,1]).
    float uvPerBlock = length(vec3(PolyShadowMat[0][0], PolyShadowMat[1][0], PolyShadowMat[2][0])) * 0.5;
    float zPerBlock = length(vec3(PolyShadowMat[0][2], PolyShadowMat[1][2], PolyShadowMat[2][2])) * 0.5;
    float texelWorld = (1.0 / float(textureSize(InShadow, 0).x)) / max(uvPerBlock, 1e-6);

    // Grid cell size, coarsened with distance. A cell smaller than a screen pixel stops being a
    // stylistic choice and becomes aliasing: neighbouring pixels land in different cells, and which
    // cell a pixel lands in flips with sub-pixel camera motion - that is the far-field flicker.
    // Coarsen in powers of two so cells stay nested and the grid doesn't swim when the step happens.
    float cell = 0.0;
    if (uPixelGridRes > 0.5) {
        cell = 1.0 / uPixelGridRes;
        cell *= exp2(max(0.0, ceil(log2(max(footprint * 1.5 / cell, 1e-6)))));
    }

    // Push the sample point off the surface along its normal BEFORE any grid snapping: it removes
    // acne on walls, and it guarantees snapped cell centers sit outside the surface's own block
    // (surfaces lie exactly on grid lines, so an un-offset snap would coin-flip into self-shadow).
    // The footprint term keeps that true far away, where the reconstructed position is itself a
    // pixel-blob wide.
    vec3 samplePos = worldRel + normal * (uNormalOffset * (1.0 + 2.0 * slope) + footprint);

    // Snap to the center of a world-aligned grid cell (grid anchored at absolute block corners).
    if (cell > 0.0) {
        samplePos = (floor((samplePos + PolyShadowCamFract) / cell) + 0.5) * cell - PolyShadowCamFract;
    }

    // Camera-relative world -> light clip space -> [0,1] shadow-map coords.
    vec4 lightClip = PolyShadowMat * vec4(samplePos, 1.0);
    vec3 proj = lightClip.xyz / lightClip.w;
    vec3 suv = proj * 0.5 + 0.5;

    // How well the (single cascade) map covers this pixel: 1 well inside, fading to 0 across the
    // outer ~15% ring and past the far edge of the light depth range. BOTH terms below are scaled by
    // it. The geometric back-face term needs it as much as the map term does: outside the cascade we
    // know nothing about this surface - clouds and distant terrain sit far outside the coverage box -
    // and darkening them off a normal we can't trust is worse than leaving them lit.
    vec2 d = abs(suv.xy - 0.5) * 2.0;
    float coverageFade = 1.0 - smoothstep(0.85, 1.0, max(max(d.x, d.y), suv.z));

    float shadow = 0.0;
    if (coverageFade > 0.0) {
        // Depth bias. The dominant term is receiver slope: the point we sample can sit up to `lateral`
        // blocks away from the point being shaded (one PCF texel, half a grid cell from the snap, one
        // pixel footprint), and on a surface grazing the light the recorded depth changes by tan(theta)
        // per block of that - 5.7x under a 10 degree sun. Cover that gap or the surface self-shadows in
        // stripes, which is what flickered in the distance where cells and footprints are large.
        float tanTheta = min(sqrt(max(1.0 - ndotl * ndotl, 0.0)) / max(ndotl, 0.05), 16.0);
        float lateral = texelWorld + 0.5 * cell + footprint;
        float bias = uShadowBias * (1.0 + 6.0 * slope) + lateral * tanTheta * zPerBlock;

        // 3x3 PCF. With the pixel grid on, the result is constant per world cell, so this reads as a
        // per-cell penumbra level (soft but still blocky) rather than a smooth screen-space blur.
        vec2 texel = 1.0 / vec2(textureSize(InShadow, 0));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                float occluder = texture(InShadow, suv.xy + vec2(dx, dy) * texel).r;
                shadow += (suv.z - bias > occluder) ? 1.0 : 0.0;
            }
        }
        shadow /= 9.0;
    }

    // Self-shadowing: a face pointing away from the light is dark no matter what the map says.
    shadow = max(shadow, (1.0 - facing) * normalTrust) * coverageFade;

    color *= (1.0 - shadow * uShadowStrength);
    fragColor = vec4(color, 1.0);
}
