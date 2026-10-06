#version 430

#moj_import <polytone_red_black.glsl>

//RB flood fill algorithm here

//64 workers per group
layout(local_size_x = 4, local_size_y = 4, local_size_z = 4) in;

layout(r16ui, binding = 0) uniform readonly uimage3D BlockGrid;
layout(rgba32ui, binding = 1) uniform readonly uimage2D Palette;
// stored as level / 15
layout(rgb10_a2, binding = 2) uniform image3D Light;

// first cell of the dispatched box, cells outside it are left as they are
uniform ivec3 BoxStart;

struct Block {
    vec3 emitted;
    vec3 filterColor;
    float opacity;
    uint solidFaces;
};

// x: rgb (emitted light if emission > 0, else the filter) + emission. y: opacity byte, solid faces byte by Direction ordinal. z w: flags
uvec4 paletteEntryAt(ivec3 slot) {
    uint index = imageLoad(BlockGrid, slot).r;
    uint column = index & 255u;
    uint row = index >> 8;
    return imageLoad(Palette, ivec2(column, row));
}

uint solidFacesOf(uvec4 entry) {
    return (entry.y >> 8u) & 0x3Fu;
}

Block blockAt(ivec3 slot) {
    uvec4 entry = paletteEntryAt(slot);
    vec4 colorAndEmission = unpackUnorm4x8(entry.x);
    // alpha byte is emission * 17, gets 0-15 back
    float emission = colorAndEmission.a * 15.0;
    bool emits = emission > 0.0;
    return Block(colorAndEmission.rgb * emission, emits ? vec3(1.0) : colorAndEmission.rgb,
            float(entry.y & 0xFFu), solidFacesOf(entry));
}

bool hasSolidFace(uint solidFaces, int direction) {
    return ((solidFaces >> uint(direction)) & 1u) != 0u;
}

vec3 brightestNeighbourReaching(ivec3 local, uint ourSolidFaces) {
    vec3 brightest = vec3(0.0);
    for (int dir = 0; dir < 6; dir++) {
        if (hasSolidFace(ourSolidFaces, dir)) continue;
        ivec3 neighbour = local + NEIGHBOURS[dir];
        // past the edge wraps to the far side of the volume, which is somewhere esle in the world
        if (!insideVolume(neighbour)) continue;
        ivec3 neighbourSlot = slotOf(neighbour);
        //vanilla checks the face on both sides
        if (hasSolidFace(solidFacesOf(paletteEntryAt(neighbourSlot)), dir ^ 1)) continue;
        brightest = max(brightest, imageLoad(Light, neighbourSlot).rgb * 15.0);
    }
    return brightest;
}

// vanilla loses max(1, opacity) levles per block. drop the strongest channel by that and scale the other two with it or colors shift hue as they fade
vec3 fadeThrough(vec3 incoming, Block block) {
    float level = max(incoming.r, max(incoming.g, incoming.b));
    if (level <= 0.0) return vec3(0.0);
    float faded = max(level - max(1.0, block.opacity), 0.0);
    return incoming * (faded / level) * block.filterColor;
}

void main() {
    ivec3 local = redBlackCellOfThisThread(BoxStart);
    if (any(greaterThanEqual(local, ivec3(Size)))){
        return;
    }

    ivec3 slot = slotOf(local);
    Block block = blockAt(slot);
    vec3 light = block.emitted;
    if (block.opacity < 15.0) {
        light = max(light, fadeThrough(brightestNeighbourReaching(local, block.solidFaces), block));
    }
    imageStore(Light, slot, vec4(light / 15.0, 1.0));
}
