
//help me

//rgb10_a2, stores level / 15
uniform sampler3D InVoxelLight;
uniform usampler3D InVoxelCells;
uniform usampler2D InVoxelPalette;
uniform vec3 PolyVoxelVolumeOrigin;
uniform ivec3 PolyVoxelVolumeOriginTexel;
uniform int PolyVoxelVolumeWidth;
uniform int PolyVoxelVolumeHeight;

ivec3 _polyVoxelVolumeSize() {
    return ivec3(PolyVoxelVolumeWidth, PolyVoxelVolumeHeight, PolyVoxelVolumeWidth);
}

bool polyHasVoxelVolume() {
    return PolyVoxelVolumeWidth != 0;
}

bool polyInVoxelVolume(ivec3 local) {
    return polyHasVoxelVolume() && all(greaterThanEqual(local, ivec3(0))) && all(lessThan(local, _polyVoxelVolumeSize()));
}

//pos is camera relative everywhere in here. local counts blocks from the volume's first corner
vec3 polyVoxelLocalPos(vec3 pos) {
    return pos - PolyVoxelVolumeOrigin;
}

ivec3 polyVoxelCellAt(vec3 pos) {
    return ivec3(floor(polyVoxelLocalPos(pos)));
}

ivec3 _polyVoxelVolumeTexel(ivec3 local) {
    return (local + PolyVoxelVolumeOriginTexel) % _polyVoxelVolumeSize();
}

vec3 polyBlockLightInCell(ivec3 local) {
    if (!polyInVoxelVolume(local)) return vec3(0.0);
    return texelFetch(InVoxelLight, _polyVoxelVolumeTexel(local), 0).rgb * 15.0;
}

float polyVoxelVolumeFade(vec3 pos) {
    //origin snaps to sections so the camra can sit up to 8 blocks off center
    float offCenterSlack = 8.0;
    float reachXZ = float(PolyVoxelVolumeWidth / 2) - offCenterSlack;
    float reachY = float(PolyVoxelVolumeHeight / 2) - offCenterSlack;
    //shorter fade on y, flat boxes would barely have any full strength band otherwise
    return (1.0 - smoothstep(reachXZ - 16.0, reachXZ, length(pos.xz))) * (1.0 - smoothstep(reachY - 8.0, reachY, abs(pos.y)));
}

vec3 polyBlockLight(vec3 pos, vec3 normal) {
    vec3 local = polyVoxelLocalPos(pos + normal * 0.5);
    vec3 size = vec3(_polyVoxelVolumeSize());
    //the texture wraps, half a texel from the edge trilinear already reads the far side
    if (any(lessThan(local, vec3(0.5))) || any(greaterThan(local, size - 0.5))) return vec3(0.0);
    return texture(InVoxelLight, (local + vec3(PolyVoxelVolumeOriginTexel)) / size).rgb * 15.0;
}

// 0 (air) outside the volume
uint polyVoxelIndexInCell(ivec3 local) {
    if (!polyInVoxelVolume(local)) return 0u;
    return texelFetch(InVoxelCells, _polyVoxelVolumeTexel(local), 0).r;
}

// x: bytes r g b a, red lowest. rgb is the light when emission > 0, else the filter color. a = emission * 17
// y: byte 0 opacity, byte 1 solid faces (bit = Direction ordinal: down up north south west east). z: flags 0-31, w: flags 32-63
uvec4 polyVoxelPaletteEntry(uint index) {
    uint column = index & 255u;
    uint row = index >> 8;
    return texelFetch(InVoxelPalette, ivec2(column, row), 0);
}

uvec4 polyVoxelEntryInCell(ivec3 local) {
    return polyVoxelPaletteEntry(polyVoxelIndexInCell(local));
}

float polyVoxelEmission(uvec4 entry) {
    return float(entry.x >> 24) / 17.0;
}

vec3 polyVoxelColor(uvec4 entry) {
    return vec3(entry.x & 255u, (entry.x >> 8) & 255u, (entry.x >> 16) & 255u) / 255.0;
}

float polyVoxelOpacity(uvec4 entry) {
    return float(entry.y & 255u);
}

bool polyVoxelHasSolidFace(uvec4 entry, int direction) {
    return ((entry.y >> uint(8 + direction)) & 1u) != 0u;
}

// lag is a PolyVoxelFlag_<name> uniform. it holds bit + 1, 0 when no block uses that flag
bool polyHasVoxelFlag(uvec4 entry, int flag) {
    if (flag <= 0) return false;
    int bit = flag - 1;
    uint word = bit < 32 ? entry.z : entry.w;
    return ((word >> uint(bit & 31)) & 1u) != 0u;
}

bool polyHasVoxelFlag(ivec3 local, int flag) {
    if (flag <= 0) return false;
    return polyHasVoxelFlag(polyVoxelEntryInCell(local), flag);
}
