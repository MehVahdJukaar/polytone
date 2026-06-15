# Custom Post Shaders

This feature is 1.21.1+

NOTE: while this will work on 1.21.1, care should be taken to have shaders there in the correct 1.21.1 format, for intsnce they should be in the minecraft folder.

This feature is a work in progress, more will come in the future.

<img width="800" height="450" alt="image" src="https://github.com/user-attachments/assets/3d695650-a699-4578-afb2-73d6d5619e2f" />

Example image of a custom screen space godray shader added in the [Sample Pack](https://github.com/MehVahdJukaar/polytone/tree/1.21.11) as an example.
You can also try [this](https://legacy.curseforge.com/minecraft/texture-packs/sunbathing-godrays) texture pack I made here.


This feature lets you add custom post shaders to the game.

To do so you'll need 3 files:
- polytone post chain file in `[pack namespace]/polytone/post_chains`
- post chain vanilla file in `[pack namespace]/post_effect`
- additional shadaer files in `[pack namespace]/shaders/post`

NOTE: this folder used to be called `post_shaders`. It was renamed to `post_chains`, so be sure to update older packs.

The polytone file is super simple and can look like this

```json
{
  "post_chain": "minecraft:creeper",
  "activation_condition": "g.dimensionType() == 'minecraft:overworld'"
}
```
this will simply apply the existing creeper post chain to the game.
Activation Condition field is optional.

Multiple post chains can be applied by different packs so be sure to make use of polytone configs and polytone priority fields that are explained on the main page to make sure the order in which they render is what you want.

Refer to the mc wiki for info on how to construct those 2.




# Extra Shader Uniforms

Polytone adds new uniforms to your post shaders.
These work on ANY post shaders and can be aded as follows by adding the following block to your shader file.

Once added you can then use those parameters. They are very useful to construct extra effects using matrices that are usually only available in core shaders. For example you can use the World projection matrix, `PolyProjMat`

```glsl

layout (std140) uniform PolyGlobals {
    mat4 PolyProjMat;
    mat4 PolyModelViewMat;
    float PolySunAngle;
    float PolyDayTime;
};



```

The available fields are:
- `PolyProjMat` - the world projection matrix
- `PolyModelViewMat` - the world model-view (camera) matrix
- `PolySunAngle` - the sun angle, offset so that 0 points at the horizon
- `PolyDayTime` - the current world day time (0-24000)

These are bound as a single std140 UBO, so they must be declared in this exact order. You may declare only the leading fields you actually use (e.g. just `PolyProjMat`), but never reorder or skip a field in the middle.



## Expression-Driven Uniforms

You can define custom per-effect float uniforms driven by Polytone expressions directly in your polytone post shader file. This gives you full control over shader parameters at runtime thanks to the use of scripting expressions (see their page).

Add an `expression_uniforms` map to your polytone post shader JSON:

```json
{
  "post_chain": "my_namespace:my_effect",
  "expression_uniforms": {
    "MyIntensity": "sin(g.time * 0.05) * 0.5 + 0.5",
    "PlayerHealth": "p.health / p.maxHealth",
    "DayProgress": "g.dayTime / 24000.0"
  }
}
```

Each key is the name of a UBO block in your GLSL shader. Declare it like this:

```glsl
layout (std140) uniform MyIntensity {
    float value;
};
```

You can then use `value` anywhere in that shader. The expression is re-evaluated every frame, so the uniform updates continuously.

When declared on a post chain file, these uniforms are bound to every pass in the chain, and only while the chain is active (so a disabled `activation_condition` also stops the binds).

Expressions have access to all the standard Polytone variables: `g` (global), `p` (player), `c` (camera), `random`, math functions, and any global expressions you have defined.

### Pro tip

Uniform expressions are calculated EVERY render tick. this makes them very expensive. Unless you need them to update in real time, consider using Global Expressions instead with possibly a low update rate. Then reference those values here in your uniforms.


# Expression Uniforms on Any Shader

The `expression_uniforms` shown above only target post chains. You can also inject the same kind of expression-driven uniforms into ANY shader (core shaders, rendertypes, etc.), not just post passes.

To do so add a file under `[pack namespace]/polytone/shader_effects/`. The file PATH determines which shader it targets, following the standard Polytone path convention. For example:

```
assets/minecraft/polytone/shader_effects/core/rendertype_solid.json
```

targets the shader `minecraft:core/rendertype_solid`.

The file body is just the uniform map itself (no `expression_uniforms` wrapper, no `post_chain` field):

```json
{
  "MyIntensity": "sin(g.time * 0.05) * 0.5 + 0.5",
  "DayProgress": "g.dayTime / 24000.0"
}
```

Each key is a UBO block name, declared in the target shader exactly as before:

```glsl
layout (std140) uniform MyIntensity {
    float value;
};
```

The uniforms are bound whenever a pipeline using that shader (matched by either its fragment or vertex shader id) renders. The same Pro tip applies: these evaluate every frame, so prefer Global Expressions for values that don't need per-frame updates.
