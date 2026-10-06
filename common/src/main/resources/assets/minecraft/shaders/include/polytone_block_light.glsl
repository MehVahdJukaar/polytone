#moj_import <light.glsl>
#moj_import <polytone_voxel_volume.glsl>
#moj_import <polytone_point_lights.glsl>
#moj_import <polytone_far_light.glsl>

//lightmap is 16x16, clamped so we neve sample past the outer texel centers
vec2 polyLightmapUv(vec2 levels, float texelShift) {
    float halfTexel = 0.5 / 16.0;
    return clamp((levels + texelShift) / 16.0, vec2(halfTexel), vec2(1.0 - halfTexel));
}

//vanilla level stays as a floor to preserve "fullbright" entities
vec4 polyTintLightmap(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 coloredLight) {
    float coloredLevel = max(coloredLight.r, max(coloredLight.g, coloredLight.b));
    if (coloredLevel <= 0.0) {
        return vanilla;
    }
    float level = max(coloredLevel, levels.x);

    // terrain reads the lightmap between texels and entities at texel centers, shift matches the lookup it replaced
    vec4 skyOnly = texture(lightMap, polyLightmapUv(vec2(0.0, levels.y), texelShift));
    vec4 lit = texture(lightMap, polyLightmapUv(vec2(level, levels.y), texelShift));
    vec3 blockLightPart = max(lit.rgb - skyOnly.rgb, 0.0);
    vec3 hue = coloredLight / coloredLevel;
    vec4 colored = vec4(skyOnly.rgb + blockLightPart * hue, lit.a);
    float coloredShare = coloredLevel / level;
    return mix(vanilla, colored, coloredShare);
}

//Far light has no occlusion so it only picks the hue, brightness stays vanilla's
vec3 polyWithFarLight(vec3 nearLight, vec3 pos, float vanillaLevel) {
    float nearWeight = polyVoxelVolumeFade(pos);
    if (nearWeight >= 1.0) return nearLight;
    vec3 far = polyFarLight(pos);
    float farLevel = max(far.r, max(far.g, far.b));
    if (farLevel <= 0.0) return nearLight;
    //max 1 so a dim far edge doesnt get pushed up to the ful vanilla level
    vec3 farAtVanillaLevel = far * (vanillaLevel / max(farLevel, 1.0));
    return nearLight + farAtVanillaLevel * (1.0 - nearWeight);
}

vec4 polyColoredLightmap(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 pos, vec3 normal) {
    vec3 coloredLight = polyWithFarLight(max(polyBlockLight(pos, normal), polyPointLight(pos)), pos, levels.x);
    return polyTintLightmap(vanilla, lightMap, levels, texelShift, coloredLight);
}

//Direction order
const vec3 POLY_DIRECTION_NORMALS[6] = vec3[6](vec3(0.0, -1.0, 0.0), vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, -1.0),
        vec3(0.0, 0.0, 1.0), vec3(-1.0, 0.0, 0.0), vec3(1.0, 0.0, 0.0));

//sampling on the face itself blends in the dark cell behind it (half as bright) so take the brigthest side
vec4 polyColoredLightmapBrightestSide(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 pos) {
    vec3 coloredLight = polyPointLight(pos);
    for (int i = 0; i < 6; i++) {
        coloredLight = max(coloredLight, polyBlockLight(pos, POLY_DIRECTION_NORMALS[i]));
    }
    coloredLight = polyWithFarLight(coloredLight, pos, levels.x);
    return polyTintLightmap(vanilla, lightMap, levels, texelShift, coloredLight);
}

// face is Direction ordinal + 1, 0 when the mesher didnt know
vec4 polyColoredLightmapForFace(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 pos, uint face) {
    if (face == 0u || face > 6u) return polyColoredLightmapBrightestSide(vanilla, lightMap, levels, texelShift, pos);
    return polyColoredLightmap(vanilla, lightMap, levels, texelShift, pos, POLY_DIRECTION_NORMALS[face - 1u]);
}

