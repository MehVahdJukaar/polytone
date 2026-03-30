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
const int GodRaySamples = 50;          // Higher quality with jitter/dither
const float Exposure = 0.25;
const float Decay = 0.99;
const float Density = 0.92;
const float Weight = 0.25;
const float SunSize = 0.06;            // Radius of the solid disk
const float SunGlow = 0.4;             // Intensity of the surrounding glow

// Transition width for crossfading (10 degrees in radians)
const float TRANSITION_WIDTH = radians(12.0);

// Sun colors
const vec3 SUN_CORE = vec3(1.0, 1.0, 0.9);
const vec3 SUN_GLOW = vec3(1.0, 0.7, 0.3);

// Moon colors (slightly blue)
const vec3 MOON_CORE = vec3(0.8, 0.9, 1.0);
const vec3 MOON_GLOW = vec3(0.5, 0.6, 1.0);

// Pseudo-random noise to break up banding/stepping artifacts
float interleaved_gradient_noise(vec2 uv) {
    return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
}

float getDepth(vec2 pos) {
    return texture(InDepthSampler, pos).r;
}

// ----------------------------------------------------------------------
// Compute screen position of a light source given its world direction angle
// Returns: screen uv (if visible), 'isBehind' flag, and a fade factor when
//          the light is near the screen edge.
// ----------------------------------------------------------------------
vec3 getLightScreenPos(float angle, out bool isBehind, out float screenFade) {
    vec3 sunDirWorld = normalize(vec3(
                                 cos(angle),
                                 sin(angle),
                                 0.0
                                 ));
    vec3 camPos = vec3(PolyModelViewMat[3]);
    vec3 lightPosWorld = camPos - sunDirWorld * 1000.0;

    vec4 lightClip = PolyProjMat * (PolyModelViewMat * vec4(lightPosWorld, 1.0));

    isBehind = (lightClip.w <= 0.0);
    if (isBehind) return vec3(-1.0);

    vec3 lightNDC = lightClip.xyz / lightClip.w;
    vec2 uv = lightNDC.xy * 0.5 + 0.5;

    // Fade out when the light approaches the screen edge
    float distFromCenter = distance(uv, vec2(0.5, 0.5));
    screenFade = smoothstep(1.5, 0.2, distFromCenter);
    if (isBehind) screenFade = 0.0;

    return vec3(uv, lightClip.w);
}

// ----------------------------------------------------------------------
// Returns a square sun shape (hard core + soft glow) for a given UV
// ----------------------------------------------------------------------
float getSunShape(vec2 uv, vec2 lightUV) {
    vec2 delta = uv - lightUV;
    float aspect = InSize.x / InSize.y;
    delta.x *= aspect;
    float dist = length(delta);

    float core = smoothstep(SunSize, SunSize * 0.8, dist);
    float glow = exp(-dist / (SunSize * SunGlow)) * SunGlow;
    return core + glow;
}

// ----------------------------------------------------------------------
// Compute god rays for a single light source.
// lightAngle   : world angle of the light (radians)
// lightColor   : color of the light (will be multiplied with ray intensity)
// ----------------------------------------------------------------------
vec3 computeGodRaysForLight(float lightAngle, vec3 lightColor) {
    bool isBehind;
    float screenFade;
    vec3 lightData = getLightScreenPos(lightAngle, isBehind, screenFade);
    if (screenFade <= 0.0) return vec3(0.0);

    vec2 lightUV = lightData.xy;

    vec2 deltaTexCoord = (texCoord - lightUV) * (1.0 / float(GodRaySamples)) * Density;
    vec2 samplingCoord = texCoord + (deltaTexCoord * interleaved_gradient_noise(gl_FragCoord.xy));

    vec3 rayColor = vec3(0.0);
    float illuminationDecay = 1.0;

    for (int i = 0; i < GodRaySamples; i++) {
        samplingCoord -= deltaTexCoord;

        float borderMask = smoothstep(0.0, 0.08, samplingCoord.x) *
        smoothstep(1.0, 0.92, samplingCoord.x) *
        smoothstep(0.0, 0.08, samplingCoord.y) *
        smoothstep(1.0, 0.92, samplingCoord.y);

        float sampleDepth = getDepth(samplingCoord);
        float sunShapeIntensity = getSunShape(samplingCoord, lightUV);
        float lightSource = (sampleDepth > 0.999999) ? sunShapeIntensity : 0.0;

        rayColor += lightColor * lightSource * illuminationDecay * Weight * borderMask;
        illuminationDecay *= Decay;
    }

    return rayColor * Exposure * screenFade;
}

// ----------------------------------------------------------------------
// Determine the weights for sun and moon based on the current angle.
// The weights are 0 or 1 except near 0 and π where they crossfade.
// ----------------------------------------------------------------------
void getLightWeights(float angle, out float sunWeight, out float moonWeight) {
    // Map angle to [0, 2π) with t = angle + π
    float t = angle + 3.14159;
    t = mod(t, 2.0 * 3.14159);

    if (t < TRANSITION_WIDTH) {
        // Transition from moon (0) to sun (1) near t = 0 (angle = -π)
        sunWeight = t / TRANSITION_WIDTH;
    } else if (t < 3.14159 - TRANSITION_WIDTH) {
        // Sun fully active (negative angles)
        sunWeight = 1.0;
    } else if (t < 3.14159 + TRANSITION_WIDTH) {
        // Transition from sun to moon near t = π (angle = 0)
        sunWeight = 1.0 - (t - (3.14159 - TRANSITION_WIDTH)) / (2.0 * TRANSITION_WIDTH);
    } else if (t < 2.0 * 3.14159 - TRANSITION_WIDTH) {
        // Moon fully active (positive angles)
        sunWeight = 0.0;
    } else {
        // Transition from moon to sun near t = 2π (angle = π)
        sunWeight = (t - (2.0 * 3.14159 - TRANSITION_WIDTH)) / TRANSITION_WIDTH;
    }

    moonWeight = 1.0 - sunWeight;
}

void main() {
    vec4 diffuseColor = texture(InSampler, texCoord);

    // Determine sun/moon activation weights
    float sunWeight, moonWeight;
    getLightWeights(PolySunAngle, sunWeight, moonWeight);

    // Compute god rays from both lights, scaled by their weights
    vec3 godRays = vec3(0.0);
    if (sunWeight > 0.0) {
        godRays += computeGodRaysForLight(PolySunAngle, SUN_GLOW) * sunWeight * GodRayIntensity;
    }
    if (moonWeight > 0.0) {
        godRays += computeGodRaysForLight(PolySunAngle + 3.14159, MOON_GLOW) * moonWeight * MoonRayIntensity;
    }

    // Apply geometry mask so rays don't brighten the sky itself
    float geometryMask = smoothstep(1.0, 0.999999, getDepth(texCoord));
    diffuseColor.rgb += godRays * geometryMask;

    // Draw visual disks (sun and moon) weighted by their activation
    bool dummy;
    float dummyFade;
    // Sun disk
    if (sunWeight > 0.0 && false) {
        vec3 sunData = getLightScreenPos(PolySunAngle, dummy, dummyFade);
        if (!dummy && dummyFade > 0.0) {
            float shape = getSunShape(texCoord, sunData.xy);
            vec3 diskColor = mix(SUN_GLOW, SUN_CORE, smoothstep(SunSize * 0.5, SunSize, shape));
            diffuseColor.rgb += shape * diskColor * sunWeight;
        }
    }
    // Moon disk
    if (moonWeight > 0.0 && false) {
        vec3 moonData = getLightScreenPos(PolySunAngle + 3.14159, dummy, dummyFade);
        if (!dummy && dummyFade > 0.0) {
            float shape = getSunShape(texCoord, moonData.xy);
            vec3 diskColor = mix(MOON_GLOW, MOON_CORE, smoothstep(SunSize * 0.5, SunSize, shape));
            diffuseColor.rgb += shape * diskColor * moonWeight;
        }
    }

    fragColor = diffuseColor;
}