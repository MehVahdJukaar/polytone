#version 330

// Directional shadow resolve, classic depth. Demo copy of the sunbathing shader plus a debug view of the raw shadow
// map in the bottom-left corner.
// Rebuilds each pixel's camera-relative position from the level depth, projects it into the light's clip space and
// compares against the light-POV depth map Polytone rendered (InShadow / PolyShadow).

uniform sampler2D InSampler;
uniform sampler2D InDepthSampler;
uniform sampler2D InShadow;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform PolyGlobals {
    mat4 PolyProjMat;
    mat4 PolyModelViewMat;
    float PolySunAngle;
};

layout(std140) uniform PolyShadow {
    mat4 PolyShadowMat;
    vec3 PolyShadowLightDir;
    vec3 PolyShadowCamFract;
};

// from expression_uniforms in polytone/post_chains/shadows.json
layout(std140) uniform ShadowStrength { float uShadowStrength; };
layout(std140) uniform ShadowBias     { float uShadowBias; };
layout(std140) uniform NormalOffset   { float uNormalOffset; };
layout(std140) uniform PixelGridRes   { float uPixelGridRes; };

void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    float depth = texture(InDepthSampler, texCoord).r;

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

    vec3 ddx = dFdx(worldRel);
    vec3 ddy = dFdy(worldRel);
    vec3 normal = normalize(cross(ddx, ddy));
    if (dot(normal, worldRel) > 0.0) normal = -normal;

    // silhouettes give a garbage normal (derivative quad straddles two depths); without this distant clouds get dark rims
    float normalTrust = smoothstep(0.05, 0.2, abs(dot(normal, normalize(worldRel))));

    vec3 absNormal = abs(normal);
    float dominant = max(absNormal.x, max(absNormal.y, absNormal.z));
    if (dominant > 0.9) normal = normalize(step(dominant - 1e-4, absNormal) * sign(normal));

    float footprint = min(max(length(ddx), length(ddy)), 2.0);

    float ndotl = dot(normal, PolyShadowLightDir);
    float slope = clamp(1.0 - ndotl, 0.0, 1.0);

    float facing = smoothstep(0.0, 0.05, ndotl);

    float uvPerBlock = length(vec3(PolyShadowMat[0][0], PolyShadowMat[1][0], PolyShadowMat[2][0])) * 0.5;
    float zPerBlock = length(vec3(PolyShadowMat[0][2], PolyShadowMat[1][2], PolyShadowMat[2][2])) * 0.5;
    float texelWorld = (1.0 / float(textureSize(InShadow, 0).x)) / max(uvPerBlock, 1e-6);

    float cell = 0.0;
    if (uPixelGridRes > 0.5) {
        cell = 1.0 / uPixelGridRes;
        cell *= exp2(max(0.0, ceil(log2(max(footprint * 1.5 / cell, 1e-6)))));
    }

    // offset before snapping: surfaces lie on grid lines, an un-offset snap would coin-flip into self-shadow
    vec3 samplePos = worldRel + normal * (uNormalOffset * (1.0 + 2.0 * slope) + footprint);

    if (cell > 0.0) {
        samplePos = (floor((samplePos + PolyShadowCamFract) / cell) + 0.5) * cell - PolyShadowCamFract;
    }

    vec4 lightClip = PolyShadowMat * vec4(samplePos, 1.0);
    vec3 proj = lightClip.xyz / lightClip.w;
    vec3 suv = proj * 0.5 + 0.5;

    vec2 fromCenter = abs(suv.xy - 0.5) * 2.0;
    float coverageFade = 1.0 - smoothstep(0.85, 1.0, max(max(fromCenter.x, fromCenter.y), suv.z));

    float shadow = 0.0;
    if (coverageFade > 0.0) {

        // receiver-slope bias, without it distant grazing surfaces self-shadow in stripes
        float tanTheta = min(sqrt(max(1.0 - ndotl * ndotl, 0.0)) / max(ndotl, 0.05), 16.0);
        float lateral = texelWorld + 0.5 * cell + footprint;
        float bias = uShadowBias * (1.0 + 6.0 * slope) + lateral * tanTheta * zPerBlock;

        vec2 texel = 1.0 / vec2(textureSize(InShadow, 0));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                float occluder = texture(InShadow, suv.xy + vec2(dx, dy) * texel).r;
                shadow += (suv.z - bias > occluder) ? 1.0 : 0.0;
            }
        }
        shadow /= 9.0;
    }

    shadow = max(shadow, (1.0 - facing) * normalTrust) * coverageFade;

    color *= (1.0 - shadow * uShadowStrength);
    fragColor = vec4(color, 1.0);
}
