layout(std140) uniform PolyGlobals {
    mat4 PolyProjMat;
    mat4 PolyModelViewMat;
    float PolySunAngle;
    float PolyDayTime;
    float PolyDeltaTime;
    ivec3 PolyPlayerBlockPos;
    vec3 PolyPlayerOffset;
};

vec3 polyPositionFromDepth(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = inverse(PolyProjMat) * ndc;
    view /= view.w;
    return (inverse(PolyModelViewMat) * view).xyz;
}

vec3 polyPlayerPos() {
    return vec3(PolyPlayerBlockPos) - PolyPlayerOffset;
}
