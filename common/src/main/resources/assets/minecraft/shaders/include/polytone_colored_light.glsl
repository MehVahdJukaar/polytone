#moj_import <polytone_voxel_volume.glsl>
#moj_import <polytone_point_lights.glsl>
#moj_import <polytone_far_light.glsl>

//all colored light at pos in light levels: the volume faded out at its edge plus the moving point lights
vec3 polyColoredLight(vec3 pos, vec3 normal) {
    float fade = polyVoxelVolumeFade(pos);
    vec3 blockLight = fade > 0.0 ? polyBlockLight(pos, normal) * fade : vec3(0.0);
    return max(blockLight, polyPointLight(pos));
}

//lightmap is 16x16, clamped so we neve sample past the outer texel centers
vec2 _polyLightmapUv(vec2 levels, float texelShift) {
    float halfTexel = 0.5 / 16.0;
    return clamp((levels + texelShift) / 16.0, vec2(halfTexel), vec2(1.0 - halfTexel));
}

//vanilla level stays as a floor to preserve "fullbright" entities
vec4 _polyTintLightmap(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 coloredLight) {
    float coloredLevel = max(coloredLight.r, max(coloredLight.g, coloredLight.b));
    if (coloredLevel <= 0.0) {
        return vanilla;
    }
    float level = max(coloredLevel, levels.x);

    // terrain reads the lightmap between texels and entities at texel centers, shift matches the lookup it replaced
    vec4 skyOnly = texture(lightMap, _polyLightmapUv(vec2(0.0, levels.y), texelShift));
    vec4 lit = texture(lightMap, _polyLightmapUv(vec2(level, levels.y), texelShift));
    vec3 blockLightPart = max(lit.rgb - skyOnly.rgb, 0.0);
    vec3 hue = coloredLight / coloredLevel;
    vec4 colored = vec4(skyOnly.rgb + blockLightPart * hue, lit.a);
    float coloredShare = coloredLevel / level;
    return mix(vanilla, colored, coloredShare);
}

//Far light has no occlusion so it only picks the hue, brightness stays vanilla's
vec3 _polyWithFarLight(vec3 nearLight, vec3 pos, float vanillaLevel) {
    float nearWeight = polyVoxelVolumeFade(pos);
    if (nearWeight >= 1.0) return nearLight;
    vec3 far = polyFarLight(pos);
    float farLevel = max(far.r, max(far.g, far.b));
    if (farLevel <= 0.0) return nearLight;
    //max 1 so a dim far edge doesnt get pushed up to the ful vanilla level
    vec3 farAtVanillaLevel = far * (vanillaLevel / max(farLevel, 1.0));
    return nearLight + farAtVanillaLevel * (1.0 - nearWeight);
}

//block light range is the blockstates own so vanilla uv2 already has the level (flat or smooth), the volume only picks the hue
vec3 _polyColorAtVanillaBrightness(vec3 lightColor, float vanillaLightLevel) {
    float level = max(lightColor.r, max(lightColor.g, lightColor.b));
    if (level <= 0.0) return lightColor;
    return lightColor * (vanillaLightLevel / max(level, 1.0));
}

vec4 _polyColoredLightmap(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 pos, vec3 normal) {
    float fade = polyVoxelVolumeFade(pos);
    vec3 blockLight = fade > 0.0 ? _polyColorAtVanillaBrightness(polyBlockLight(pos, normal), levels.x) * fade : vec3(0.0);
    vec3 coloredLight = _polyWithFarLight(max(blockLight, polyPointLight(pos)), pos, levels.x);
    return _polyTintLightmap(vanilla, lightMap, levels, texelShift, coloredLight);
}

//Direction order
const vec3 _POLY_DIRECTION_NORMALS[6] = vec3[6](vec3(0.0, -1.0, 0.0), vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, -1.0),
        vec3(0.0, 0.0, 1.0), vec3(-1.0, 0.0, 0.0), vec3(1.0, 0.0, 0.0));

//sampling on the face itself blends in the dark cell behind it (half as bright) so take the brigthest side
vec4 _polyColoredLightmapBrightestSide(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 pos) {
    vec3 coloredLight = vec3(0.0);
    float fade = polyVoxelVolumeFade(pos);
    for (int i = 0; i < 6 && fade > 0.0; i++) {
        coloredLight = max(coloredLight, polyBlockLight(pos, _POLY_DIRECTION_NORMALS[i]));
    }
    coloredLight = _polyColorAtVanillaBrightness(coloredLight, levels.x) * fade;
    coloredLight = max(coloredLight, polyPointLight(pos));
    coloredLight = _polyWithFarLight(coloredLight, pos, levels.x);
    return _polyTintLightmap(vanilla, lightMap, levels, texelShift, coloredLight);
}

// face is Direction ordinal + 1, 0 when the mesher didnt know
vec4 _polyColoredLightmapForFace(vec4 vanilla, sampler2D lightMap, vec2 levels, float texelShift, vec3 pos, uint face) {
    if (face == 0u || face > 6u) return _polyColoredLightmapBrightestSide(vanilla, lightMap, levels, texelShift, pos);
    return _polyColoredLightmap(vanilla, lightMap, levels, texelShift, pos, _POLY_DIRECTION_NORMALS[face - 1u]);
}

