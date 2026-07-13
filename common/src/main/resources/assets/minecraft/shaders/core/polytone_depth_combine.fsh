#version 150

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// Folds the first-person hand depth back into the saved world-depth snapshot. This pass is drawn
// into the world-depth snapshot with a LESS_THAN_OR_EQUAL depth test and depth writes on, so writing
// the (hand-only) main depth here leaves min(worldDepth, handDepth) per pixel. Where the hand is
// absent the main depth is the cleared far value (1.0), which never passes LEQUAL against nearer
// world geometry, so the world depth is preserved. Color writes are masked off by the caller.
void main() {
    gl_FragDepth = texture(InSampler, texCoord).r;
    fragColor = vec4(0.0);
}
