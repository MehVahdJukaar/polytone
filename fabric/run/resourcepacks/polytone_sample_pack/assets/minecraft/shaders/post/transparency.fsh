#version 330

uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D TranslucentSampler;
uniform sampler2D TranslucentDepthSampler;
uniform sampler2D ItemEntitySampler;
uniform sampler2D ItemEntityDepthSampler;
uniform sampler2D ParticlesSampler;
uniform sampler2D ParticlesDepthSampler;
uniform sampler2D WeatherSampler;
uniform sampler2D WeatherDepthSampler;
uniform sampler2D CloudsSampler;
uniform sampler2D CloudsDepthSampler;

layout (std140) uniform PolyGlobals {
    mat4 PolyProjMat;
    mat4 PolyModelViewMat;
};

in vec2 texCoord;
out vec4 fragColor;

// --- CONFIGURATION ---
const float GodRayIntensity = 0.45;
const int GodRaySamples = 80;       // Higher quality with jitter/dither
const float Exposure = 0.25;
const float Decay = 0.98;
const float Density = 0.92;
const float Weight = 0.35;
const vec3 SunDirection = vec3(0.0, 0.0, 1.0); // Adjust based on your world North

// Helper for layering
vec4 color_layers[6] = vec4[](vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0));
float depth_layers[6] = float[](0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
int active_layers = 0;

void try_insert(vec4 color, float depth) {
    if (color.a == 0.0) return;
    color_layers[active_layers] = color;
    depth_layers[active_layers] = depth;
    int jj = active_layers++;
    int ii = jj - 1;
    while (jj > 0 && depth_layers[jj] > depth_layers[ii]) {
        float dTmp = depth_layers[ii]; depth_layers[ii] = depth_layers[jj]; depth_layers[jj] = dTmp;
        vec4 cTmp = color_layers[ii]; color_layers[ii] = color_layers[jj]; color_layers[jj] = cTmp;
        jj = ii--;
    }
}

vec3 blend(vec3 dst, vec4 src) {
    return (dst * (1.0 - src.a)) + src.rgb;
}

// Pseudo-random noise to break up banding/stepping artifacts
float interleaved_gradient_noise(vec2 uv) {
    return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
}

vec3 getSunScreenPos(out bool isBehind) {
    vec3 sunDirWorld = normalize(SunDirection);
    vec3 camPos = vec3(PolyModelViewMat[3]);
    vec3 sunPosWorld = camPos - sunDirWorld * 1000.0;

    vec4 sunClip = PolyProjMat * (PolyModelViewMat * vec4(sunPosWorld, 1.0));

    // w <= 0 means the sun is behind the camera plane
    isBehind = (sunClip.w <= 0.0);

    if (isBehind) return vec3(-1.0);

    vec3 sunNDC = sunClip.xyz / sunClip.w;
    return vec3(sunNDC.xy * 0.5 + 0.5, sunClip.w);
}

vec3 computeGodRays(vec2 uv) {
    bool isBehind;
    vec3 sunData = getSunScreenPos(isBehind);
    vec2 sunUV = sunData.xy;

    // 1. DYNAMIC FADING
    // Fades the rays out as the sun moves toward the screen edges or behind the camera
    float distFromCenter = distance(sunUV, vec2(0.5));
    float screenFade = smoothstep(1.5, 0.7, distFromCenter);
    if (isBehind) screenFade = 0.0;
    if (screenFade <= 0.0) return vec3(0.0);

    // 2. OCCLUSION PRE-CHECK
    // If the sun center is hidden by a mountain, dim the rays
    float sunDepthSample = texture(MainDepthSampler, clamp(sunUV, 0.0, 1.0)).r;
    float sunVisibility = (sunDepthSample > 0.9999) ? 1.0 : 0.2; // 0.2 keeps a slight "haze"

    // 3. RAY MARCHING SETUP
    vec2 deltaTexCoord = (uv - sunUV) * (1.0 / float(GodRaySamples)) * Density;

    // Interleaved noise dithering: shifts the sample start per pixel to hide banding
    vec2 samplingCoord = uv + (deltaTexCoord * interleaved_gradient_noise(gl_FragCoord.xy));

    vec3 rayColor = vec3(0.0);
    float illuminationDecay = 1.0;

    for (int i = 0; i < GodRaySamples; i++) {
        samplingCoord -= deltaTexCoord;

        // 4. EDGE SOFTENING
        // Crucial fix: Fades samples to 0 as they exit the screen frame
        // This prevents rays from "bunching up" and spiking in brightness at the borders
        float borderMask = smoothstep(0.0, 0.08, samplingCoord.x) * smoothstep(1.0, 0.92, samplingCoord.x) * smoothstep(0.0, 0.08, samplingCoord.y) * smoothstep(1.0, 0.92, samplingCoord.y);

        float sampleDepth = texture(MainDepthSampler, samplingCoord).r;

        // Only the sky (depth ~ 1.0) contributes light to the ray
        float lightSource = (sampleDepth > 0.9999) ? 1.0 : 0.0;

        rayColor += vec3(lightSource) * illuminationDecay * Weight * borderMask;
        illuminationDecay *= Decay;
    }

    return rayColor * Exposure * GodRayIntensity * screenFade * sunVisibility;
}

vec3 drawDebugSun(vec2 uv, vec2 sunPos) {
    float d = distance(uv, sunPos);
    float core = smoothstep(0.012, 0.008, d);
    float glow = exp(-d * 35.0) * 0.7;
    vec3 coreColor = vec3(1.0, 1.0, 0.9);
    vec3 glowColor = vec3(1.0, 0.7, 0.3);
    return (core * coreColor) + (glow * glowColor);
}

void main() {
    // Collect scene layers
    color_layers[0] = vec4(texture(MainSampler, texCoord).rgb, 1.0);
    depth_layers[0] = texture(MainDepthSampler, texCoord).r;
    active_layers = 1;

    try_insert(texture(TranslucentSampler, texCoord), texture(TranslucentDepthSampler, texCoord).r);
    try_insert(texture(ItemEntitySampler, texCoord), texture(ItemEntityDepthSampler, texCoord).r);
    try_insert(texture(ParticlesSampler, texCoord), texture(ParticlesDepthSampler, texCoord).r);
    try_insert(texture(WeatherSampler, texCoord), texture(WeatherDepthSampler, texCoord).r);
    try_insert(texture(CloudsSampler, texCoord), texture(CloudsDepthSampler, texCoord).r);

    vec3 sceneAccum = color_layers[0].rgb;
    for (int i = 1; i < active_layers; ++i) {
        sceneAccum = blend(sceneAccum, color_layers[i]);
    }

    // Compute improved God Rays
    vec3 godRays = computeGodRays(texCoord);

    // Apply geometry mask so rays don't brighten the sky itself (optional, looks cleaner)
    float geometryMask = smoothstep(1.0, 0.9995, depth_layers[0]);

    // Add rays to scene
    sceneAccum += (godRays * geometryMask);

    // Draw Visual Sun Disk
    bool isBehind;
    vec3 sunData = getSunScreenPos(isBehind);
    if (!isBehind) {
        // Only draw sun if it's not occluded by geometry
        float sunOcclusion = step(0.9999, depth_layers[0]);
        sceneAccum += drawDebugSun(texCoord, sunData.xy) * sunOcclusion;
    }

    fragColor = vec4(sceneAccum, 1.0);
}