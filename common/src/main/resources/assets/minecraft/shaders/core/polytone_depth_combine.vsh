#version 150

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;

// Fullscreen triangle/quad: positions are already in NDC, so no matrices are needed.
void main() {
    gl_Position = vec4(Position, 1.0);
    texCoord = UV0;
}