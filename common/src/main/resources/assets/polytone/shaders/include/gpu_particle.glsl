#version 330

// Per-type knobs, same for every particle of a gpu particle type. Field order must match the
// Std140Builder calls in GpuParticleRenderer.
layout(std140) uniform ParticleInfo {
    vec3 Origin;        // record origin relative to the camera
    float Time;         // ticks since the record time base
    float Gravity;      // blocks per tick squared, positive pulls down
    float Drag;         // -ln(friction per tick)
    float Sway;
    float Spin;
    float SizeEnd;      // < 0 keeps the start size
    float Aspect;       // height / width
    float AlphaCutoff;
    int Frames;
    vec2 Fade;          // fade in, fade out, as fractions of the lifetime
    int Billboard;      // 0 camera, 1 around Y, 2 flat, 3 along velocity
    int RandomSprite;
    int UseColorEnd;
    vec4 ColorEnd;
};
