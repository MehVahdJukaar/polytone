#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <polytone:gpu_particle.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;
in vec4 debugRealColor;

out vec4 fragColor;

void main() {
    // TEMP DEBUG: R = texture alpha, G = real vertex alpha, B = would survive the alpha cutoff
    if (vertexColor.a > 1.5) {
        if (vertexColor.rgb != vec3(0.0)) {
            fragColor = vec4(vertexColor.rgb, 1.0);
            return;
        }
        // exactly what the real path composes, forced opaque so blending can't hide it
        vec4 real = texture(Sampler0, texCoord0) * debugRealColor * ColorModulator;
        fragColor = vec4(real.rgb, 1.0);
        return;
    }
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < AlphaCutoff) {
        discard;
    }
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
