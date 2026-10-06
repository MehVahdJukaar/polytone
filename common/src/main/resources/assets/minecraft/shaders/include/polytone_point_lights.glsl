#moj_import <minecraft:globals.glsl>

//size must match ShaderPointLights.MAX_LIGHTS
layout(std140) uniform PolyPointLights {
    ivec3 PolyPointLightAnchor;
    int PolyPointLightCount;
    //xyz from the anchor block, w = radius
    vec4 PolyPointLightPos[64];
    vec4 PolyPointLightColor[64];
};

// pos is camera relative. same units as the volume: light levels so 15 is a full torch. no occlusion
vec3 polyPointLight(vec3 pos) {
    vec3 anchor = vec3(PolyPointLightAnchor - CameraBlockPos) + CameraOffset;
    vec3 total = vec3(0.0);
    for (int i = 0; i < PolyPointLightCount; i++) {
        vec3 lightPos = PolyPointLightPos[i].xyz + anchor;
        float levelAtCenter = PolyPointLightPos[i].w;
        float level = min(levelAtCenter - distance(pos, lightPos), 15.0);
        if (level > 0.0) total = max(total, PolyPointLightColor[i].rgb * level);
    }
    return total;
}
