#version 150

// Full-screen copy of the swap target back to minecraft:main, always opaque.

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
}
