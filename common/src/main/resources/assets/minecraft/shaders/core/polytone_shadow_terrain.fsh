#version 150

out vec4 fragColor;

// Opaque layer of the shadow depth pass. The framebuffer has no draw buffer bound, so this output is
// thrown away and only gl_FragDepth matters; deliberately free of any texture fetch or discard so the
// hardware can keep early depth testing on.
void main() {
    fragColor = vec4(1.0);
}
