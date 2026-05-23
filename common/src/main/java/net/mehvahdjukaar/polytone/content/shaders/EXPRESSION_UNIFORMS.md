## Expression-Driven Uniforms

You can define custom per-effect float uniforms driven by Polytone expressions directly in your polytone post shader file. This gives you full control over shader parameters at runtime — no need to hardcode values.

Add an `expression_uniforms` map to your polytone post shader JSON:

```json
{
  "post_chain": "my_namespace:my_effect",
  "activation_condition": "g.dimensionType() == \"minecraft:overworld\"",
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