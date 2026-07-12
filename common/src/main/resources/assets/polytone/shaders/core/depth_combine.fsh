#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// Re-projects a saved world depth back into the current depth buffer (which, after
// the first-person hand is drawn, only holds the hand on a cleared background).
// The pipeline runs with a LESS_THAN_OR_EQUAL depth test and depth writes on, so
// writing the world depth here leaves min(worldDepth, existingHandDepth) per pixel.
// Color writes are masked off (WRITE_NONE) so the scene color is untouched.
void main() {
    gl_FragDepth = texture(InSampler, texCoord).r;
    fragColor = vec4(0.0);
}
