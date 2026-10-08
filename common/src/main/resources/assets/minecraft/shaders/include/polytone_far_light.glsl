
//level / POLY_FAR_LIGHT_MAX_LEVEL, padded by a cell in far_light_spread.csh
uniform sampler3D InFarLight;
uniform vec3 PolyFarLightOrigin;
uniform ivec3 PolyFarLightOriginTexel;
uniform ivec3 PolyFarLightSize;

const float POLY_FAR_LIGHT_CELL = 4.0;
const float POLY_FAR_LIGHT_MAX_LEVEL = 15.0 + POLY_FAR_LIGHT_CELL;

float polyFarLightFade(vec3 pos) {
    vec3 halfExtentInBlocks = vec3(PolyFarLightSize) * (POLY_FAR_LIGHT_CELL * 0.5);
    //same 8 block secton snap slack as the near volume
    vec3 reach = halfExtentInBlocks - 8.0;
    //8 and 4 cells, twice the near volume bands since cells are coarse
    float fadeBandXZ = 32.0;
    float fadeBandY = 16.0;
    return (1.0 - smoothstep(reach.x - fadeBandXZ, reach.x, length(pos.xz))) * (1.0 - smoothstep(reach.y - fadeBandY, reach.y, abs(pos.y)));
}

vec3 polyFarLight(vec3 pos) {
    if (PolyFarLightSize.x == 0) return vec3(0.0);
    float fade = polyFarLightFade(pos);
    if (fade <= 0.0) return vec3(0.0);
    vec3 cell = (pos - PolyFarLightOrigin) / POLY_FAR_LIGHT_CELL;
    return texture(InFarLight, (cell + vec3(PolyFarLightOriginTexel)) / vec3(PolyFarLightSize)).rgb * (POLY_FAR_LIGHT_MAX_LEVEL * fade);
}
