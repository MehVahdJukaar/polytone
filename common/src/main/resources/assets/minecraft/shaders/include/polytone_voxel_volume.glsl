
//help me

//rgb10_a2, stores level / 15
uniform sampler3D InVoxelLight;
uniform usampler3D InVoxelCells;
uniform usampler2D InVoxelPalette;
uniform vec3 PolyVoxelVolumeOrigin;
uniform ivec3 PolyVoxelVolumeOriginTexel;
uniform int PolyVoxelVolumeSize;
uniform int PolyVoxelVolumeHeight;

ivec3 polyVoxelVolumeSize() {
    return ivec3(PolyVoxelVolumeSize, PolyVoxelVolumeHeight, PolyVoxelVolumeSize);
}

bool polyInVoxelVolume(ivec3 local) {
    return PolyVoxelVolumeSize != 0 && all(greaterThanEqual(local, ivec3(0))) && all(lessThan(local, polyVoxelVolumeSize()));
}

ivec3 polyVoxelCellAt(vec3 pos) {
    return ivec3(floor(pos - PolyVoxelVolumeOrigin));
}

ivec3 polyVoxelVolumeTexel(ivec3 local) {
    return (local + PolyVoxelVolumeOriginTexel) % polyVoxelVolumeSize();
}

vec3 polyLightInCell(ivec3 local) {
    if (!polyInVoxelVolume(local)) return vec3(0.0);
    return texelFetch(InVoxelLight, polyVoxelVolumeTexel(local), 0).rgb * 15.0;
}

float polyVoxelVolumeFade(vec3 pos) {
    //origin snaps to sections so the camra can sit up to 8 blocks off center
    float offCenterSlack = 8.0;
    float reachXZ = float(PolyVoxelVolumeSize / 2) - offCenterSlack;
    float reachY = float(PolyVoxelVolumeHeight / 2) - offCenterSlack;
    //shorter fade on y, flat boxes would barely have any full strength band otherwise
    return (1.0 - smoothstep(reachXZ - 16.0, reachXZ, length(pos.xz))) * (1.0 - smoothstep(reachY - 8.0, reachY, abs(pos.y)));
}

vec3 polyBlockLight(vec3 pos, vec3 normal) {
    float fade = polyVoxelVolumeFade(pos);
    if (fade <= 0.0) return vec3(0.0);
    //hardware trilinear. edges blend with the far sdie of the ring but fade is 0 there anyway
    vec3 inFrontOfFace = pos + normal * 0.5;
    vec3 local = inFrontOfFace - PolyVoxelVolumeOrigin;
    vec3 uvw = (local + vec3(PolyVoxelVolumeOriginTexel)) / vec3(polyVoxelVolumeSize());
    return texture(InVoxelLight, uvw).rgb * (15.0 * fade);
}

// 0 (air) outside the volume
uint polyVoxelIndexInCell(ivec3 local) {
    if (!polyInVoxelVolume(local)) return 0u;
    return texelFetch(InVoxelCells, polyVoxelVolumeTexel(local), 0).r;
}

// x: bytes r g b a, red lowest. rgb is the light when emission > 0, else the filter color. a = emission * 17
// y: byte 0 opacity, byte 1 solid faces (bit = Direction ordinal: down up north south west east). z: flags 0-31, w: flags 32-63
uvec4 polyVoxelPaletteEntry(uint index) {
    uint column = index & 255u;
    uint row = index >> 8;
    return texelFetch(InVoxelPalette, ivec2(column, row), 0);
}

// lag is a PolyVoxelFlag_<name> uniform. it holds bit + 1, 0 when no block uses that flag
bool polyHasVoxelFlag(ivec3 local, int flag) {
    if (flag <= 0) return false;
    uvec4 entry = polyVoxelPaletteEntry(polyVoxelIndexInCell(local));
    int bit = flag - 1;
    uint word = bit < 32 ? entry.z : entry.w;
    return ((word >> uint(bit & 31)) & 1u) != 0u;
}
