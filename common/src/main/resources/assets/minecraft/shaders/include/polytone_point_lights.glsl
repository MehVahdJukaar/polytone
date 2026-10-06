//size must match ShaderPointLights.MAX_LIGHTS
uniform vec4 PolyPointLightPos[64];
uniform vec4 PolyPointLightColor[64];
uniform int PolyPointLightCount;

// pos is camera relative. same units as the volume: light levels so 15 is a full torch. no occlusion
vec3 polyPointLight(vec3 pos) {
    vec3 total = vec3(0.0);
    for (int i = 0; i < PolyPointLightCount; i++) {
        vec3 lightPos = PolyPointLightPos[i].xyz;
        float levelAtCenter = PolyPointLightPos[i].w;
        float level = min(levelAtCenter - distance(pos, lightPos), 15.0);
        if (level > 0.0) total = max(total, PolyPointLightColor[i].rgb * level);
    }
    return total;
}
