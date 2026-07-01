#version 330

uniform sampler2D InSampler;
uniform sampler2D InDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout (std140) uniform PolyGlobals {
    mat4 PolyProjMat;
    mat4 PolyModelViewMat;
    float PolySunAngle;
};

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

// --- CONFIGURATION ---
const float GodRayIntensity = 0.5;
const float MoonRayIntensity = 0.1;
const int   GodRaySamples = 30;

const float Exposure = 0.25;
const float Decay = 0.99;
const float Density = 0.92;
const float Weight = 0.25;

const float SunSize = 0.06;
const float SunGlow = 0.4;

const float PI = 3.14159265;
const float TRANSITION_WIDTH = radians(12.0);

// Colors
const vec3 SUN_CORE = vec3(1.0, 1.0, 0.9);
const vec3 SUN_GLOW = vec3(1.0, 0.7, 0.3);
const vec3 MOON_CORE = vec3(0.8, 0.9, 1.0);
const vec3 MOON_GLOW = vec3(0.5, 0.6, 1.0);

// Precomputed constants
const float INV_SAMPLES = 1.0 / float(GodRaySamples);

// --- NOISE ---
float interleaved_gradient_noise(vec2 uv) {
    return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
}

// --- DEPTH ---
float getDepth(vec2 pos) {
    return texture(InDepthSampler, pos).r;
}


// ----------------------------------------------------------------------
vec3 getLightScreenPos(float angle, out float screenFade) {
    vec3 dir = vec3(cos(angle), sin(angle), 0.0);

    vec3 camPos = PolyModelViewMat[3].xyz;
    vec3 lightPos = camPos - dir * 1000.0;

    vec4 clip = PolyProjMat * (PolyModelViewMat * vec4(lightPos, 1.0));

    if (clip.w <= 0.0) {
        screenFade = 0.0;
        return vec3(0.0);
    }

    vec2 uv = (clip.xy / clip.w) * 0.5 + 0.5;

    float dist = distance(uv, vec2(0.5));
    screenFade = smoothstep(1.5, 0.2, dist);

    return vec3(uv, clip.w);
}

// ----------------------------------------------------------------------
float getSunShape(vec2 uv, vec2 lightUV, float aspect) {
    vec2 d = uv - lightUV;
    d.x *= aspect;

    float dist = length(d);

    float core = smoothstep(SunSize, SunSize * 0.8, dist);
    float glow = exp(-dist / (SunSize * SunGlow)) * SunGlow;

    return core + glow;
}

// ----------------------------------------------------------------------
vec3 computeGodRays(vec2 lightUV, float screenFade, vec3 lightColor, float aspect) {
    if (screenFade <= 0.0) return vec3(0.0);

    vec2 delta = (texCoord - lightUV) * INV_SAMPLES * Density;

    float noise = interleaved_gradient_noise(gl_FragCoord.xy);
    vec2 coord = texCoord + delta * noise;

    vec3 acc = vec3(0.0);
    float decayAcc = 1.0;

    for (int i = 0; i < GodRaySamples; ++i) {
        coord -= delta;

        // Branchless border mask
        vec2 b0 = smoothstep(vec2(0.0), vec2(0.08), coord);
        vec2 b1 = smoothstep(vec2(1.0), vec2(0.92), coord);
        float borderMask = b0.x * b0.y * b1.x * b1.y;

        float depth = texture(InDepthSampler, coord).r;

        float shape = getSunShape(coord, lightUV, aspect);

        // 26.2 reversed-Z: far plane / sky clears to 0.0 (was 1.0), near = 1.0.
        // Sky is where depth <= epsilon.
        float light = step(depth, 0.000001) * shape;

        acc += lightColor * light * decayAcc * Weight * borderMask;

        decayAcc *= Decay;
    }

    return acc * Exposure * screenFade;
}

// ----------------------------------------------------------------------
void getLightWeights(float angle, out float sunW, out float moonW) {
    float t = mod(angle + PI, 2.0 * PI);

    if (t < TRANSITION_WIDTH) {
        sunW = t / TRANSITION_WIDTH;
    } else if (t < PI - TRANSITION_WIDTH) {
        sunW = 1.0;
    } else if (t < PI + TRANSITION_WIDTH) {
        sunW = 1.0 - (t - (PI - TRANSITION_WIDTH)) / (2.0 * TRANSITION_WIDTH);
    } else if (t < 2.0 * PI - TRANSITION_WIDTH) {
        sunW = 0.0;
    } else {
        sunW = (t - (2.0 * PI - TRANSITION_WIDTH)) / TRANSITION_WIDTH;
    }

    moonW = 1.0 - sunW;
}

// ----------------------------------------------------------------------
void main() {
    vec4 color = texture(InSampler, texCoord);

    float sunW, moonW;
    getLightWeights(PolySunAngle, sunW, moonW);

    float aspect = InSize.x / InSize.y;

    vec3 rays = vec3(0.0);

    if (sunW > 0.0) {
        float fade;
        vec3 data = getLightScreenPos(PolySunAngle, fade);
        rays += computeGodRays(data.xy, fade, SUN_GLOW, aspect) * sunW * GodRayIntensity;
    }

    if (moonW > 0.0) {
        float fade;
        vec3 data = getLightScreenPos(PolySunAngle + PI, fade);
        rays += computeGodRays(data.xy, fade, MOON_GLOW, aspect) * moonW * MoonRayIntensity;
    }

    float depth = getDepth(texCoord);
    // 26.2 reversed-Z: geometry has depth > 0, sky is depth ~ 0.0. Mask keeps rays over geometry.
    float geometryMask = smoothstep(0.0, 0.000001, depth);

    color.rgb += rays * geometryMask;

    fragColor = color;
}