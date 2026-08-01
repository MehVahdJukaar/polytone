#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;

out vec2 texCoord0;

// Shared vertex stage for the shadow depth pass. Same section-relative positioning as the vanilla
// terrain shaders (ChunkOffset is set per section), minus everything that only feeds colour.
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ChunkOffset, 1.0);
    texCoord0 = UV0;
}
