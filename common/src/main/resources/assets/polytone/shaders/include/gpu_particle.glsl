#version 330

// Per-type knobs, same for every particle of a gpu particle type. Field order must match the
// Std140Builder calls in GpuParticleRenderer. Origin and Time share a vec4 on purpose: Std140Builder
// pads a vec3 to 16 bytes, while std140 would pack a following scalar at offset 12, and everything
// after it would then be read 4 bytes off.
layout(std140) uniform ParticleInfo {
    vec4 OriginTime;    // xyz: record origin relative to the camera, w: ticks since the record time base
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

#define Origin (OriginTime.xyz)
#define Time (OriginTime.w)
