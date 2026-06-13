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
- additional shader files in `[pack namespace]/shaders/post`


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




# Extra Post Shader Uniforms

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

`PolyGlobals` is now auto-bound to every render pass — not just post shaders. Declare the block in any vanilla, replaced, or custom shader and the values are available there too.



## Expression-Driven Uniforms

You can define custom per-effect float uniforms driven by Polytone expressions directly in your polytone post chain file. This gives you full control over shader parameters at runtime thanks to the use of scripting expressions (see their page).

Add an `expression_uniforms` map to your polytone post chain JSON:

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

You can then use `value` anywhere in that shader pass. The expression is re-evaluated every frame, so the uniform updates continuously.

Expressions have access to all the standard Polytone variables: `g` (global), `p` (player), `c` (camera), `random`, math functions, and any global expressions you have defined.

### Pro tip

Uniform expressions are calculated EVERY render tick. this makes them very expensive. Unless you need them to update in real time, consider using Global Expressions instead with possibly a low update rate. Then reference those values here in your uniforms.



# Shader Effects (any shader, 1.21.11+)

Same expression-driven uniforms as above, but attachable to **any** shader — vanilla core shaders, user-replaced shaders, or post-chain pass shaders.

Files live in `[pack namespace]/polytone/shader_effects/<shader path>.json`. The **file path is the target shader id**, so

`assets/minecraft/polytone/shader_effects/core/rendertype_solid.json`

targets `minecraft:core/rendertype_solid`.

The JSON body is just a map of UBO-block-name → expression:

```json
{
  "Intensity": "world_time * 0.001",
  "TintRed": "p.biome == 'minecraft:nether_wastes' ? 1 : 0"
}
```

Shader side, declare each as a single-float UBO:

```glsl
layout (std140) uniform Intensity { float value; };
layout (std140) uniform TintRed   { float value; };
```

No `activation_condition` field — gate values inside the expression itself (`cond ? v : 0`). Vanilla shaders that don't declare the block are unaffected.
