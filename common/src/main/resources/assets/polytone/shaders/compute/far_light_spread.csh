#version 430

#moj_import <polytone_red_black.glsl>

//near_light_spread without the occlusion, on 4 block cells
layout(local_size_x = 4, local_size_y = 4, local_size_z = 4) in;

layout(rgba8, binding = 0) uniform readonly image3D Emission;
//output
layout(rgb10_a2, binding = 1) uniform image3D Light;

//FarLightGrid.CELL_SIZE, one level lost per block
const float CELL_SIZE = 4.0;
//cell centers sit up to 2 blocks off the light so pad the reach by a cell. only hue so overshoot is fine
const float PADDING = CELL_SIZE;
//stored as level / MAX_LEVEL, same in polytone_far_light.glsl
const float MAX_LEVEL = 15.0 + PADDING;

void main() {
    ivec3 local = redBlackCellOfThisThread(ivec3(0));
    if (!insideVolume(local)) return;

    ivec3 slot = slotOf(local);
    vec3 light = imageLoad(Emission, slot).rgb * 15.0;
    float emitted = max(light.r, max(light.g, light.b));
    if (emitted > 0.0) light *= (emitted + PADDING) / emitted;
    for (int dir = 0; dir < 6; dir++) {
        ivec3 neighbour = local + NEIGHBOURS[dir];
        if (!insideVolume(neighbour)) continue;
        vec3 incoming = imageLoad(Light, slotOf(neighbour)).rgb * MAX_LEVEL;
        float level = max(incoming.r, max(incoming.g, incoming.b));
        if (level <= CELL_SIZE) continue;
        light = max(light, incoming * ((level - CELL_SIZE) / level));
    }
    imageStore(Light, slot, vec4(light / MAX_LEVEL, 1.0));
}
