
uniform ivec3 Size;
// where the volume's first block sits in the wrapped-around textures
uniform ivec3 OriginTexel;
// 1 updates the red cells, (x + y + z) odd. 0 the black ones
uniform int RedPass;

const ivec3 NEIGHBOURS[6] = ivec3[](
    ivec3(0, -1, 0), ivec3(0, 1, 0),
    ivec3(0, 0, -1), ivec3(0, 0, 1),
    ivec3(-1, 0, 0), ivec3(1, 0, 0)
);

ivec3 slotOf(ivec3 local) {
    return (local + OriginTexel) % Size;
}

bool insideVolume(ivec3 local) {
    return all(greaterThanEqual(local, ivec3(0))) && all(lessThan(local, ivec3(Size)));
}

//half as mani threads along x
ivec3 redBlackCellOfThisThread(ivec3 boxStart) {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    int y = boxStart.y + id.y;
    int z = boxStart.z + id.z;
    return ivec3(boxStart.x + id.x * 2 + ((y + z + RedPass) & 1), y, z);
}
