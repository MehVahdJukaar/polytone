#version 150

// Directional shadow resolve, classic depth. Demo pack shader plus a debug view of the raw shadow map
// in the bottom-left corner.
// Rebuilds each pixel's camera-relative position from the level depth snapshot, projects it into the
// light's clip space and compares against the light-POV depth map Polytone rendered (InShadow).

uniform sampler2D DiffuseSampler;
uniform sampler2D InDepth;
uniform sampler2D InShadow;

in vec2 texCoord;
out vec4 fragColor;

// polytone built-ins, individual uniforms on 1.21.1
uniform mat4 PolyProjMat;
uniform mat4 PolyModelViewMat;
uniform mat4 PolyShadowMat;
uniform vec3 PolyShadowLightDir;
uniform vec3 PolyShadowCamFract;

// from expression_uniforms in polytone/post_chains/shadows.json
uniform float ShadowStrength;
uniform float ShadowBias;
uniform float NormalOffset;
uniform float PixelGridRes;

void main() {
    vec3 color = texture(DiffuseSampler, texCoord).rgb;
    float depth = texture(InDepth, texCoord).r;

    const float DebugSize = 0.25;
    if (texCoord.x < DebugSize && texCoord.y < DebugSize) {
        float mapDepth = texture(InShadow, texCoord / DebugSize).r;
        float shade = clamp((mapDepth - 0.5) * 4.0 + 0.5, 0.0, 1.0);
        fragColor = vec4(vec3(shade), 1.0);
        return;
    }

    if (depth >= 1.0) {
        fragColor = vec4(color, 1.0);
        return;
    }

    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = inverse(PolyProjMat) * ndc;
    viewPos /= viewPos.w;
    vec3 worldRel = (inverse(PolyModelViewMat) * viewPos).xyz;

    vec3 normal = normalize(cross(dFdx(worldRel), dFdy(worldRel)));
    if (dot(normal, worldRel) > 0.0) normal = -normal;

    float slope = clamp(1.0 - dot(normal, PolyShadowLightDir), 0.0, 1.0);

    // offset before snapping: surfaces lie on grid lines, an un-offset snap would coin-flip into self-shadow
    vec3 samplePos = worldRel + normal * (NormalOffset * (1.0 + 2.0 * slope));

    if (PixelGridRes > 0.5) {
        float cell = 1.0 / PixelGridRes;
        samplePos = (floor((samplePos + PolyShadowCamFract) / cell) + 0.5) * cell - PolyShadowCamFract;
    }

    vec4 lightClip = PolyShadowMat * vec4(samplePos, 1.0);
    vec3 proj = lightClip.xyz / lightClip.w;
    vec3 suv = proj * 0.5 + 0.5;

    // outside the single cascade, treat as lit
    if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0 || suv.z > 1.0) {
        fragColor = vec4(color, 1.0);
        return;
    }

    float bias = ShadowBias * (1.0 + 6.0 * slope);

    // 3x3 PCF. with the pixel grid on the result is constant per world cell, so it reads as a per-cell
    // penumbra level rather than a screen-space blur
    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(InShadow, 0));
    for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
            float occluder = texture(InShadow, suv.xy + vec2(dx, dy) * texel).r;
            shadow += (suv.z - bias > occluder) ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    // fade the outer ring instead of hard-cutting at the coverage boundary, so shadows don't pop as
    // the covered box slides over the world
    vec2 fromCenter = abs(suv.xy - 0.5) * 2.0;
    float edgeFade = 1.0 - smoothstep(0.85, 1.0, max(max(fromCenter.x, fromCenter.y), suv.z));
    shadow *= edgeFade;

    color *= (1.0 - shadow * ShadowStrength);
    fragColor = vec4(color, 1.0);
}
