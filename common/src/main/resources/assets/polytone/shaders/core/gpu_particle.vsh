#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <polytone:gpu_particle.glsl>

// One record, repeated over the four corners of its quad. Nothing here changes after the spawn:
// the whole simulation is this shader plus Time.
in vec3 Position;   // spawn position, relative to Origin
in vec3 Velocity;   // blocks per tick
in vec2 SpawnLife;  // spawn tick (relative to the time base), lifetime in ticks
in vec4 Params;     // size, roll, custom, seed
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;

const float TAU = 6.2831853;

float hash(float seed, float salt) {
    return fract(sin(seed * 127.1 + salt * 311.7) * 43758.5453);
}

void main() {
    int corner = gl_VertexID & 3;
    float life = SpawnLife.y;
    float age = Time - SpawnLife.x;
    if (life <= 0.0 || age < 0.0 || age >= life) {
        gl_Position = vec4(-2.0, -2.0, -2.0, 1.0);
        sphericalVertexDistance = 0.0;
        cylindricalVertexDistance = 0.0;
        vertexColor = vec4(0.0);
        texCoord0 = vec2(0.0);
        return;
    }
    float f = age / life;
    float size0 = Params.x;
    float roll0 = Params.y;
    float seed = Params.w;

    // velocity decays by friction each tick: dv/dt = g - k v with k = Drag, solved exactly
    vec3 g = vec3(0.0, -Gravity, 0.0);
    vec3 pos;
    if (Drag > 1.0e-5) {
        float ek = (1.0 - exp(-Drag * age)) / Drag;
        pos = Position + Velocity * ek + g * (age - ek) / Drag;
    } else {
        pos = Position + Velocity * age + 0.5 * g * age * age;
    }
    pos += Origin;
    if (Sway != 0.0) {
        pos.x += Sway * sin(age * 0.1 + hash(seed, 1.0) * TAU);
        pos.z += Sway * cos(age * 0.08 + hash(seed, 2.0) * TAU);
    }

    vec3 right;
    vec3 up;
    if (Billboard == 1) {
        right = normalize(vec3(ModelViewMat[0][0], 0.0, ModelViewMat[2][0]));
        up = vec3(0.0, 1.0, 0.0);
    } else if (Billboard == 2) {
        right = vec3(1.0, 0.0, 0.0);
        up = vec3(0.0, 0.0, -1.0);
    } else if (Billboard == 3) {
        // current velocity direction; keep the quad facing the camera around that axis
        vec3 v = Drag > 1.0e-5 ? (Velocity - g / Drag) * exp(-Drag * age) + g / Drag : Velocity + g * age;
        float len = length(v);
        up = len > 1.0e-6 ? v / len : vec3(0.0, 1.0, 0.0);
        vec3 toCam = normalize(-pos);
        right = cross(up, toCam);
        float rl = length(right);
        right = rl > 1.0e-6 ? right / rl : vec3(ModelViewMat[0][0], ModelViewMat[1][0], ModelViewMat[2][0]);
    } else {
        right = vec3(ModelViewMat[0][0], ModelViewMat[1][0], ModelViewMat[2][0]);
        up = vec3(ModelViewMat[0][1], ModelViewMat[1][1], ModelViewMat[2][1]);
    }

    vec2 cn = vec2((corner == 1 || corner == 2) ? 1.0 : -1.0, (corner >= 2) ? 1.0 : -1.0);
    float roll = roll0 + Spin * age;
    if (roll != 0.0) {
        float s = sin(roll);
        float co = cos(roll);
        cn = vec2(cn.x * co - cn.y * s, cn.x * s + cn.y * co);
    }
    float size = SizeEnd < 0.0 ? size0 : mix(size0, SizeEnd, f);
    vec3 vert = pos + right * (cn.x * size) + up * (cn.y * size * Aspect);

    float frame = RandomSprite == 1 ? floor(hash(seed, 4.0) * float(Frames)) : min(floor(f * float(Frames)), float(Frames - 1));
    float u = (corner == 1 || corner == 2) ? 1.0 : 0.0;
    float v = (corner >= 2) ? 0.0 : 1.0;
    texCoord0 = vec2(u, (frame + v) / float(Frames));

    float fade = 1.0;
    if (Fade.x > 0.0) fade *= smoothstep(0.0, Fade.x, f);
    if (Fade.y > 0.0) fade *= 1.0 - smoothstep(1.0 - Fade.y, 1.0, f);

    vec4 color = UseColorEnd == 1 ? mix(Color, ColorEnd, f) : Color;
    vertexColor = color * texelFetch(Sampler2, UV2 / 16, 0) * vec4(1.0, 1.0, 1.0, fade);
    sphericalVertexDistance = fog_spherical_distance(vert);
    cylindricalVertexDistance = fog_cylindrical_distance(vert);
    gl_Position = ProjMat * ModelViewMat * vec4(vert, 1.0);
}
