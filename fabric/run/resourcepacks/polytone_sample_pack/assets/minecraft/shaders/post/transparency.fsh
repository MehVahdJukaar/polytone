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
    vec3 PolySunPos;

};

in vec2 texCoord;

vec4 color_layers[6] = vec4[](vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0));
float depth_layers[6] = float[](0, 0, 0, 0, 0, 0);
int active_layers = 0;

out vec4 fragColor;

void try_insert(vec4 color, float depth) {
    if (color.a == 0.0) {
        return;
    }

    color_layers[active_layers] = color;
    depth_layers[active_layers] = depth;

    int jj = active_layers++;
    int ii = jj - 1;
    while (jj > 0 && depth_layers[jj] > depth_layers[ii]) {
        float depthTemp = depth_layers[ii];
        depth_layers[ii] = depth_layers[jj];
        depth_layers[jj] = depthTemp;

        vec4 colorTemp = color_layers[ii];
        color_layers[ii] = color_layers[jj];
        color_layers[jj] = colorTemp;

        jj = ii--;
    }
}

vec3 blend(vec3 dst, vec4 src) {
    return (dst * (1.0 - src.a)) + src.rgb;
}

// Strength of the rays
const float GodRayIntensity = 0.3;

// Number of steps along the ray
const int GodRaySamples = 200;

// Parameters for fine-tuning the effect
const float Exposure = 0.2;
const float Decay = 0.96;
const float Density = 0.8;
const float Weight = 0.5;
// Direction towards North (Negative Z)
// We use a large distance or w=0 to represent 'at infinity'
const vec3 SunDirection = vec3(0.0, 0.0, 1.0);

vec2 getSunScreenPos() {
    // SunDirection is normalized
    vec3 sunDirWorld = normalize(SunDirection);

    // Choose a point along the direction in front of the camera
    vec3 camPos = vec3(PolyModelViewMat[3]); // camera world position
    vec3 sunPosWorld = camPos - sunDirWorld * 1000.0; // arbitrary distance along direction

    vec4 sunClip = PolyProjMat * vec4((PolyModelViewMat * vec4(sunPosWorld, 1.0)).xyz, 1.0);

    if (sunClip.w <= 0.0) return vec2(-1.0, -1.0);

    vec3 sunNDC = sunClip.xyz / sunClip.w;
    return sunNDC.xy * 0.5 + 0.5;
}

vec3 computeGodRays(vec2 uv) {
    vec2 SunScreenPos = getSunScreenPos();
    // skip if sun behind camera
    if (SunScreenPos.x < 0.0) return vec3(0.0);
    // ----- unchanged logic -----
    vec2 deltaTexCoord = (uv - SunScreenPos);
    deltaTexCoord *= 1.0 / float(GodRaySamples) * Density;

    vec3 color = vec3(0.0);
    float illuminationDecay = 1.0;

    vec2 samplingCoord = uv;

    for (int i = 0; i < GodRaySamples; i++) {
        samplingCoord -= deltaTexCoord;
        samplingCoord = clamp(samplingCoord, vec2(0.0), vec2(1.0));

        float sampleDepth = texture(MainDepthSampler, samplingCoord).r;

        float mask = (sampleDepth > 0.9999999) ? 1.0 : 0.0;

        color += vec3(mask) * illuminationDecay * Weight;
        illuminationDecay *= Decay;
    }

    return color * Exposure * GodRayIntensity;
}

// Debug draw of a mat4 in a screen-space rectangle
// rectMin, rectMax in UV space (0–1)
// values assumed in [-1,1] → normalized to [0,1]
vec4 drawMatrix4(vec2 uv, vec2 rectMin, vec2 rectMax, mat4 m) {
    // Check if inside debug region
    if (uv.x < rectMin.x || uv.y < rectMin.y ||
    uv.x > rectMax.x || uv.y > rectMax.y) {
        return vec4(0.0);
    }

    // Local UV inside rect
    vec2 local = (uv - rectMin) / (rectMax - rectMin);

    // 4x4 grid cell index
    ivec2 cell = ivec2(floor(local * 4.0));
    cell = clamp(cell, ivec2(0), ivec2(3));

    // Access matrix (column-major in GLSL)
    float v = m[cell.x][cell.y];

    // Normalize [-1,1] → [0,1]
    float n = clamp(v * 0.5 + 0.5, 0.0, 1.0);

    // Optional grid lines
    vec2 grid = fract(local * 4.0);
    float line = step(0.95, grid.x) + step(0.95, grid.y);

    vec3 color = vec3(n);

    // draw grid lines in red
    if (line > 0.0) {
        color = vec3(1.0, 0.0, 0.0);
    }

    return vec4(color, 1.0);
}

vec3 drawDebugSun(vec2 uv, vec2 sunPos) {
    float d = distance(uv, sunPos);

    // 1. The "Hot" Core (The actual disk of the sun)
    // Using a very tight smoothstep for a crisp but slightly anti-aliased edge
    float core = smoothstep(0.015, 0.012, d);

    // 2. The Corona / Glow
    // We use an inverse exponential falloff for a natural light spread
    float glow = exp(-d * 40.0) * 0.6;
    float wideGlow = exp(-d * 15.0) * 0.2;

    // 3. Coloring
    // White-hot center fading into a warm yellow/orange
    vec3 coreColor = vec3(1.0, 1.0, 0.9);
    vec3 glowColor = vec3(1.0, 0.6, 0.2);

    // Combine them
    vec3 finalSun = (core * coreColor) + (glow * glowColor) + (wideGlow * glowColor);

    // Subtle flare flicker (optional/fun)
    // If you have a 'Time' uniform, you could multiply the glow by:
    // (1.0 + 0.05 * sin(Time * 5.0))

    return finalSun;
}

void main() {
    color_layers[0] = vec4(texture(MainSampler, texCoord).rgb, 1.0);
    depth_layers[0] = texture(MainDepthSampler, texCoord).r;
    active_layers = 1;

    try_insert(texture(TranslucentSampler, texCoord), texture(TranslucentDepthSampler, texCoord).r);
    try_insert(texture(ItemEntitySampler, texCoord), texture(ItemEntityDepthSampler, texCoord).r);
    try_insert(texture(ParticlesSampler, texCoord), texture(ParticlesDepthSampler, texCoord).r);
    try_insert(texture(WeatherSampler, texCoord), texture(WeatherDepthSampler, texCoord).r);
    try_insert(texture(CloudsSampler, texCoord), texture(CloudsDepthSampler, texCoord).r);

    vec3 texelAccum = color_layers[0].rgb;
    for (int ii = 1; ii < active_layers; ++ii) {
        texelAccum = blend(texelAccum, color_layers[ii]);
    }
    // Calculate the rays
    vec3 godRays = computeGodRays(texCoord);

    // --- SKY MASKING LOGIC ---
    // Check the depth of the current pixel (the destination of the ray)
    float currentDepth = texture(MainDepthSampler, texCoord).r;

    // If currentDepth is ~1.0, it's the sky.
    // We create a multiplier that is 1.0 for blocks and 0.0 for sky.
    float geometryMask = (currentDepth < 0.999) ? 1.0 : 0.0;

    // Smooth the mask slightly so the transition isn't pixelated at the horizon
    geometryMask = clamp((1.0 - currentDepth) * 1000.0, 0.0, 1.0);

    // Apply the rays ONLY to geometry
    // This prevents the sky from getting brighter
    texelAccum += (godRays * geometryMask);


    // Calculate the sun color
    vec2 sunPos = getSunScreenPos();
    vec3 sunVisual = drawDebugSun(texCoord, sunPos) * step(0.999999, depth_layers[0]);

    // Add the sun to the scene (additive blending looks more realistic for light)
    // We only draw it if it's on screen (sunPos.x >= 0.0)
    if (sunPos.x >= 0.0) {
        texelAccum += sunVisual;
    }

    fragColor = vec4(texelAccum, 1.0);


    vec4 debugMat = drawMatrix4(texCoord, vec2(0.02, 0.7), vec2(0.3, 0.98), PolyProjMat);
    fragColor = mix(fragColor, debugMat, debugMat.a);

}
