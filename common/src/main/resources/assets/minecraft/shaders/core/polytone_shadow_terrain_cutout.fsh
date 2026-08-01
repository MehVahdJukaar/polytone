#version 150

uniform sampler2D Sampler0;

in vec2 texCoord0;

out vec4 fragColor;

// Cutout layers of the shadow depth pass. The atlas fetch is kept solely for the alpha test - without
// it leaves, grass and glass panes would cast solid-block shadows. Split from the opaque fragment
// shader because the discard forces late depth testing, which the opaque layer should not pay for.
void main() {
    if (texture(Sampler0, texCoord0).a < 0.1) discard;
    fragColor = vec4(1.0);
}
