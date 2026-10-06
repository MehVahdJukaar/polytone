#version 430

#moj_import <polytone_red_black.glsl>

//near_light_spread without the occlusion, on 4 block cells
layout(local_size_x = 4, local_size_y = 4, local_size_z = 4) in;

layout(rgba8, binding = 0) uniform readonly image3D Emission;
//output
layout(rgb10_a2, binding = 1) uniform image3D Light;

//FarLightGrid.CELL_SIZE, one level lost per block
const float CELL_SIZE = 4.0;

void main() {
    ivec3 local = redBlackCellOfThisThread(ivec3(0));
    if (!insideVolume(local)) return;

    ivec3 slot = slotOf(local);
    vec3 light = imageLoad(Emission, slot).rgb * 15.0;
    for (int dir = 0; dir < 6; dir++) {
        ivec3 neighbour = local + NEIGHBOURS[dir];
        if (!insideVolume(neighbour)) continue;
        vec3 incoming = imageLoad(Light, slotOf(neighbour)).rgb * 15.0;
        float level = max(incoming.r, max(incoming.g, incoming.b));
        if (level <= CELL_SIZE) continue;
        light = max(light, incoming * ((level - CELL_SIZE) / level));
    }
    imageStore(Light, slot, vec4(light / 15.0, 1.0));
}
