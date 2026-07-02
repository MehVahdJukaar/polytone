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

const float PI = 3.14159265;
const float TRANSITION_WIDTH = radians(12.0);

// --- Single rainbow iris ring (subtle, cinematic) ---
const float RING_POS = 1.15;       // ring position along the sun -> screen-center axis (past center)
const float RING_RADIUS = 0.12;    // medium ring radius (fraction of screen height)
const float RING_THICKNESS = 0.045;// width of the rainbow band

// Overall intensity, driven by the "Lens Flare" config slider (0..1) via expression_uniforms.
layout(std140) uniform FlareStrength { float uFlareStrength; };

// Day weight: 1 when the sun is the active light, 0 at night (mirrors godrays getLightWeights)
float sunWeight(float angle) {
    float t = mod(angle + PI, 2.0 * PI);
    if (t < TRANSITION_WIDTH) return t / TRANSITION_WIDTH;
    else if (t < PI - TRANSITION_WIDTH) return 1.0;
    else if (t < PI + TRANSITION_WIDTH) return 1.0 - (t - (PI - TRANSITION_WIDTH)) / (2.0 * TRANSITION_WIDTH);
    else if (t < 2.0 * PI - TRANSITION_WIDTH) return 0.0;
    else return (t - (2.0 * PI - TRANSITION_WIDTH)) / TRANSITION_WIDTH;
}

// Sun screen-space position; .z (clip.w) > 0 means it's in front of the camera
vec3 getSunScreenPos() {
    vec3 dir = vec3(cos(PolySunAngle), sin(PolySunAngle), 0.0);
    vec3 camPos = PolyModelViewMat[3].xyz;
    vec3 lightPos = camPos - dir * 1000.0;
    vec4 clip = PolyProjMat * (PolyModelViewMat * vec4(lightPos, 1.0));
    if (clip.w <= 0.0) return vec3(0.0, 0.0, -1.0);
    return vec3((clip.xy / clip.w) * 0.5 + 0.5, clip.w);
}

// smooth hue ramp: 0 = red, 0.33 = green, 0.66 = blue, 1 = red
vec3 spectrum(float t) {
    t = clamp(t, 0.0, 1.0);
    return clamp(vec3(
        abs(t * 6.0 - 3.0) - 1.0,
        2.0 - abs(t * 6.0 - 2.0),
        2.0 - abs(t * 6.0 - 4.0)
    ), 0.0, 1.0);
}

// 1.0 where the sampled pixel is open sky.
// 26.2 reversed-Z: far plane / sky clears to 0.0 (was 1.0), near = 1.0.
float skyAt(vec2 uv) {
    float depth = texture(InDepthSampler, clamp(uv, 0.0, 1.0)).r;
    return step(depth, 0.000001);
}

void main() {
    vec4 color = texture(InSampler, texCoord);

    vec3 sun = getSunScreenPos();
    float sunW = sunWeight(PolySunAngle);

    if (sun.z > 0.0 && sunW > 0.0) {
        vec2 sunUV = sun.xy;
        float aspect = InSize.x / InSize.y;

        // soft 5-tap occlusion so the flare fades smoothly as terrain crosses the sun
        vec2 o = 3.0 / InSize;
        float visible = (skyAt(sunUV)
                       + skyAt(sunUV + vec2( o.x, 0.0))
                       + skyAt(sunUV + vec2(-o.x, 0.0))
                       + skyAt(sunUV + vec2(0.0,  o.y))
                       + skyAt(sunUV + vec2(0.0, -o.y))) * 0.2;

        // fade as the sun nears / leaves the screen edges
        float edgeFade = smoothstep(0.0, 0.25, sunUV.x) * smoothstep(1.0, 0.75, sunUV.x)
                       * smoothstep(0.0, 0.25, sunUV.y) * smoothstep(1.0, 0.75, sunUV.y);

        float gate = sunW * edgeFade * visible;

        if (gate > 0.0) {
            // one medium iris ring, placed along the sun -> center axis (parallaxes as you look around)
            vec2 toCenter = vec2(0.5) - sunUV;
            vec2 ringCenter = sunUV + toCenter * RING_POS;

            vec2 cd = texCoord - ringCenter;
            cd.x *= aspect;
            float dist = length(cd);

            float x = (dist - RING_RADIUS) / RING_THICKNESS;  // -1 inner .. +1 outer edge
            float band = 1.0 - smoothstep(0.0, 1.0, abs(x));  // bright at the ring, fades off the band
            vec3 rainbow = spectrum((x * 0.5 + 0.5) * 0.75);  // red (inner) -> blue (outer)

            // slider 1.0 -> 0.2 strength (0.5 -> 0.1, a subtle default), keeping headroom
            color.rgb += rainbow * band * (uFlareStrength * 0.2) * gate;
        }
    }

    fragColor = color;
}
