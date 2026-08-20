#version 150

#moj_import <fog.glsl>
#moj_import <light.glsl>

in vec3 Position;

uniform samplerBuffer ParticleData;
uniform sampler2D Sampler1; // heightmap, see GpuParticleHeightmap
uniform sampler2D Sampler2; // lightmap

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Origin;      // record origin relative to the camera
uniform float Time;       // ticks since the record time base
uniform float Gravity;    // blocks per tick squared, positive pulls down
uniform float Drag;       // -ln(friction per tick)
uniform float SizeEnd;    // < 0 keeps the start size
uniform int UseColorEnd;
uniform vec4 ColorEnd;
uniform vec2 Fade;        // fade in, fade out, as fractions of the lifetime
uniform float Sway;
uniform float Spin;
uniform float Aspect;     // height / width
uniform int Billboard;    // 0 camera, 1 around Y, 2 flat, 3 along velocity
uniform int Frames;
uniform int RandomSprite;
uniform int AreaCount;    // quads per spawn
uniform vec3 AreaSize;    // box one spawn spreads over
uniform int KillBelowHeightmap;
uniform vec4 HeightmapInfo; // xy: heightmap origin (xz) relative to the camera, z: size in blocks, w: world minY
uniform float CameraY;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

const float TAU = 6.2831853;

float hash(float seed, float salt) {
    return fract(sin(seed * 127.1 + salt * 311.7) * 43758.5453);
}

void main() {
    int quad = gl_VertexID >> 2;
    int corner = gl_VertexID & 3;
    // with an area, one record is AreaCount quads in a row; each gets its own offset and phase
    int id = quad / AreaCount;
    int sub = quad - id * AreaCount;
    vec4 a = texelFetch(ParticleData, id * 4);
    vec4 b = texelFetch(ParticleData, id * 4 + 1);
    vec4 c = texelFetch(ParticleData, id * 4 + 2);
    vec4 d = texelFetch(ParticleData, id * 4 + 3);

    float life = b.w;
    float age = Time - b.z;
    if (life <= 0.0 || age < 0.0 || age >= life) {
        gl_Position = vec4(-2.0, -2.0, -2.0, 1.0);
        vertexDistance = 0.0;
        vertexColor = vec4(0.0);
        texCoord0 = vec2(0.0);
        return;
    }
    float f = age / life;
    float seed = c.x + float(sub) * 0.6180339;
    float lightAlpha = c.y;
    float rgb = c.z;
    float size0 = c.w;
    float roll0 = d.x;

    // velocity decays by friction each tick: dv/dt = g - k v with k = Drag, solved exactly
    vec3 p0 = a.xyz;
    vec3 v0 = vec3(a.w, b.x, b.y);
    vec3 g = vec3(0.0, -Gravity, 0.0);
    vec3 pos;
    if (Drag > 1.0e-5) {
        float ek = (1.0 - exp(-Drag * age)) / Drag;
        pos = p0 + v0 * ek + g * (age - ek) / Drag;
    } else {
        pos = p0 + v0 * age + 0.5 * g * age * age;
    }
    pos += Origin;
    if (AreaCount > 1) {
        pos += (vec3(hash(seed, 5.0), hash(seed, 6.0), hash(seed, 7.0)) - 0.5) * AreaSize;
    }
    if (KillBelowHeightmap == 1) {
        vec2 uv = (pos.xz - HeightmapInfo.xy) / HeightmapInfo.z;
        if (all(greaterThanEqual(uv, vec2(0.0))) && all(lessThan(uv, vec2(1.0)))) {
            vec4 t = texelFetch(Sampler1, ivec2(uv * HeightmapInfo.z), 0);
            float surface = HeightmapInfo.w + t.r * 255.0 + t.g * 255.0 * 256.0;
            if (pos.y + CameraY < surface) {
                gl_Position = vec4(-2.0, -2.0, -2.0, 1.0);
                vertexDistance = 0.0;
                vertexColor = vec4(0.0);
                texCoord0 = vec2(0.0);
                return;
            }
        }
    }
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
        vec3 v = Drag > 1.0e-5 ? (v0 - g / Drag) * exp(-Drag * age) + g / Drag : v0 + g * age;
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

    float alpha8 = floor(lightAlpha / 65536.0);
    float light = lightAlpha - alpha8 * 65536.0;
    float b8 = floor(rgb / 65536.0);
    float g8 = floor((rgb - b8 * 65536.0) / 256.0);
    float r8 = rgb - b8 * 65536.0 - g8 * 256.0;
    vec4 startColor = vec4(r8, g8, b8, alpha8) / 255.0;
    vec4 color = UseColorEnd == 1 ? mix(startColor, ColorEnd, f) : startColor;
    ivec2 lightCoords = ivec2(int(mod(light, 256.0)), int(light / 256.0));
    vertexColor = color * minecraft_sample_lightmap(Sampler2, lightCoords) * vec4(1.0, 1.0, 1.0, fade);
    vertexDistance = fog_distance(vert, FogShape);
    gl_Position = ProjMat * ModelViewMat * vec4(vert, 1.0);
}
