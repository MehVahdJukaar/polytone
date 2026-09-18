#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform TintStrength { float uTintStrength; };

void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    vec3 warm = color * vec3(1.1, 0.85, 0.6);
    fragColor = vec4(mix(color, warm, uTintStrength), 1.0);
}
