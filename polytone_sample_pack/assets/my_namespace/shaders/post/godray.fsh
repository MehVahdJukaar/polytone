#version 330

uniform sampler2D InSampler;
uniform sampler2D InDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

layout (std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout (std140) uniform PolyGlobals {
    mat4 PolyProjMat;
    mat4 PolyModelViewMat;
    float PolySunAngle;
};

layout (std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};


// --- CONFIGURATION ---
const float GodRayIntensity = 0.45;
const int GodRaySamples = 60;       // Higher quality with jitter/dither
const float Exposure = 0.25;
const float Decay = 0.97;
const float Density = 0.92;
const float Weight = 0.25;
const vec3 SunDirection = vec3(1.0, 0.0, 0.0); // Adjust based on your world North


// Pseudo-random noise to break up banding/stepping artifacts
float interleaved_gradient_noise(vec2 uv) {
    return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
}

float getDepth(vec2 pos) {
    return texture(InDepthSampler, pos).r;
}

vec3 getSunScreenPos(out bool isBehind) {
    vec3 sunDirWorld = normalize(vec3(
                                 cos(PolySunAngle),
                                 sin(PolySunAngle),
                                 0, 0
                                 ));
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
    float distFromCenter = distance(sunUV, vec2(0, 0));
    float screenFade = smoothstep(1.5, 0.2, distFromCenter);
    if (isBehind) screenFade = 0.0;
    if (screenFade <= 0.0) return vec3(0.0);

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

        float sampleDepth = getDepth(samplingCoord);

        // Only the sky (depth ~ 1.0) contributes light to the ray
        float lightSource = (sampleDepth > 0.999999) ? 1.0 : 0.0;

        rayColor += vec3(lightSource) * illuminationDecay * Weight * borderMask;
        illuminationDecay *= Decay;
    }

    return rayColor * Exposure * GodRayIntensity * screenFade;
}

vec3 drawDebugSun(vec2 uv, vec2 sunPos) {
    // Convert to centered space
    vec2 delta = uv - sunPos;

    // Fix aspect ratio (X stretched by width/height)
    float aspect = InSize.x / InSize.y;
    delta.x *= aspect;

    float d = length(delta);

    float core = smoothstep(0.012, 0.008, d);
    float glow = exp(-d * 35.0) * 0.7;

    vec3 coreColor = vec3(1.0, 1.0, 0.9);
    vec3 glowColor = vec3(1.0, 0.7, 0.3);

    return (core * coreColor) + (glow * glowColor);
}


void main() {
    vec2 sizeRatio = OutSize / InSize;

    // Compute improved God Rays
    vec3 godRays = computeGodRays(texCoord);

    // Apply geometry mask so rays don't brighten the sky itself (optional, looks cleaner)
    float geometryMask = smoothstep(1.0, 0.999999, getDepth(texCoord));
    // Add rays to scene
    vec4 diffuseColor = texture(InSampler, texCoord);;
    diffuseColor += vec4(godRays * geometryMask, 1);

    // Draw Visual Sun Disk
    bool isBehind;
    vec3 sunData = getSunScreenPos(isBehind);
    if (!isBehind) {
        // Only draw sun if it's not occluded by geometry
        // diffuseColor += vec4(drawDebugSun(texCoord, sunData.xy) * (1-geometryMask), 1);
    }
    fragColor = diffuseColor;
}

