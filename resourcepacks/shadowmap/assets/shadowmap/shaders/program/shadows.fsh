#version 150

// Directional shadow resolve pass (Polytone 1.21.1 post pipeline).
//
// Reconstructs each screen pixel's camera-relative world position from the level depth snapshot,
// projects it into the light's clip space, and compares against the shadow depth map that
// ShadowMapManager rendered from the sun/moon's point of view. Pixels whose reconstructed position
// is further from the light than the nearest occluder are darkened.
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

uniform sampler2D DiffuseSampler;   // scene colour (vanilla auto-binds the pass intarget)
uniform sampler2D InDepth;          // level depth snapshot (Polytone binds it via use_depth_buffer)
uniform sampler2D InShadow;         // light-POV depth map (Polytone binds it via use_shadow_map)

in vec2 texCoord;
out vec4 fragColor;

// Polytone built-ins (individual uniforms on 1.21.1)
uniform mat4 PolyProjMat;           // camera projection
uniform mat4 PolyModelViewMat;      // camera view (rotation only; camera at origin)
uniform mat4 PolyShadowMat;         // light view-projection, camera-relative space
uniform vec3 PolyShadowLightDir;    // unit direction toward the light, camera-relative space
uniform vec3 PolyShadowCamFract;    // fract(cameraPos): world-grid anchor for camera-relative coords

// From expression_uniforms in polytone/post_chains/shadows.json
uniform float ShadowStrength;       // 0..1, how dark a fully shadowed pixel gets
uniform float ShadowBias;           // base depth bias (normalized light depth)
uniform float NormalOffset;         // sample offset along the surface normal, in blocks
uniform float PixelGridRes;         // world-grid cells per block for pixelated shadows; 0 = smooth

void main() {
    vec3 color = texture(DiffuseSampler, texCoord).rgb;
    float depth = texture(InDepth, texCoord).r;

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
    vec3 normal = normalize(cross(dFdx(worldRel), dFdy(worldRel)));
    if (dot(normal, worldRel) > 0.0) normal = -normal;

    // How edge-on the surface is to the light: 0 when facing it, ->1 when grazing/away.
    float slope = clamp(1.0 - dot(normal, PolyShadowLightDir), 0.0, 1.0);

    // Push the sample point off the surface along its normal BEFORE any grid snapping: it removes
    // acne on walls, and it guarantees snapped cell centers sit outside the surface's own block
    // (surfaces lie exactly on grid lines, so an un-offset snap would coin-flip into self-shadow).
    vec3 samplePos = worldRel + normal * (NormalOffset * (1.0 + 2.0 * slope));

    // Snap to the center of a world-aligned grid cell (grid anchored at absolute block corners).
    if (PixelGridRes > 0.5) {
        float g = 1.0 / PixelGridRes;
        samplePos = (floor((samplePos + PolyShadowCamFract) / g) + 0.5) * g - PolyShadowCamFract;
    }

    // Camera-relative world -> light clip space -> [0,1] shadow-map coords.
    vec4 lightClip = PolyShadowMat * vec4(samplePos, 1.0);
    vec3 proj = lightClip.xyz / lightClip.w;
    vec3 suv = proj * 0.5 + 0.5;

    // Outside the (single cascade) shadow frustum -> treat as lit.
    if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0 || suv.z > 1.0) {
        fragColor = vec4(color, 1.0);
        return;
    }

    // Slope-scaled depth bias: surfaces edge-on to the light need more.
    float bias = ShadowBias * (1.0 + 6.0 * slope);

    // 3x3 PCF. With the pixel grid on, the result is constant per world cell, so this reads as a
    // per-cell penumbra level (soft but still blocky) rather than a smooth screen-space blur.
    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(InShadow, 0));
    for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
            float occluder = texture(InShadow, suv.xy + vec2(dx, dy) * texel).r;
            shadow += (suv.z - bias > occluder) ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    color *= (1.0 - shadow * ShadowStrength);
    fragColor = vec4(color, 1.0);
}
