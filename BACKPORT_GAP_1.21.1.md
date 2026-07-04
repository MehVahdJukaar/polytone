# Feature gap: what `1.21.1` is missing vs `26.2`

Generated 2026-07-04. Compares the `1.21.1` branch (the PR/backport target) against `26.2`
(newest dev). `1.21.1` is a **fresh backport target** (~30 commits off a `1.21` base) that has
been receiving selective backports, so several mature subsystems from the `26.x` / `1.21.11`
lineage have not landed there yet.

## Method & caveats

- Compared by **class basename** (stable across MC versions) plus per-subsystem file listings,
  cross-checked against recent commit subjects and the wiki feature pages.
- Package layout differs between branches (`26.2` moved `color`/`colormap`/`fluid`/`noise` under
  `content/`; `1.21.1` keeps them at `polytone/`) — that reorg is **not** a feature gap and is
  excluded here.
- Many `26.2`-only **mixins/accessors** (`GuiRenderStateMixin`, `SubmitNodeStorageMixin`,
  `RenderPassMixin`, `TextureSetupMixin`, …) are render-pipeline plumbing for the newer MC, not
  user-facing features — excluded from the list below. Some features (Sodium core-shader hooks,
  parts of the sky/render work) may be **hard or impossible to backport** cleanly because they
  depend on newer MC rendering; flagged where relevant.

## Confirmed missing subsystems (whole features)

| # | Feature | Wiki page | Defining classes absent in `1.21.1` | Notes |
|---|---------|-----------|-------------------------------------|-------|
| 1 | **Environment Attributes** (define/modify custom env attributes) | Environment-Attributes | `EnvironmentAttributesHandler`, `EnvironmentAttributeMapMod`, `ExtendedAttributeMod`, `EnvironmentSystemMixin`, `AttributeProbeMixin`, `EnvironmentAttributeSystemBuilderMixin`, `EnvironmentAttributeEntryMixin`, `SpatialAttributeInterpolatorMixin` | `1.21.1` only has expression-side attr *reading* (`ExpUtils.parseEnvAttr`, proxies) + basic `WeatherFog`. The registration/modifier system is absent. |
| 2 | **Global Expressions** (reusable named expressions) | Scripting-Expressions | `GlobalExpressionsManager`, `GlobalExpression` (`content/global_expressions`) | Entire subsystem missing. |
| 3 | **Shader expression uniforms** (expression-driven & built-in uniforms for post shaders) | Shaders | `ShaderUniformsManager`, `ExpressionUniformBuffers`, `PolytoneGlobalUniforms`, `PolytoneBuiltInUniformsSet`, `PostProcessCodecs`, `EXPRESSION_UNIFORMS.md` | `1.21.1` has **base** post shaders (`PostShadersManager`, `PostChainEffect`) but not the expression-uniform layer. |
| 4 | **Sky / fog rendering: sunbathing, procedural clouds, flare** | Dimension-Effects-Modifiers / Extra-Features | `SkyRendererMixin`, `FogRenderer`, `FogEnvironmentMod`, `FogEnvironmentWrapper` (+ `weather.json`) | Recent `26.2` commits `sunbathing`, `flare`, `sky stuff`. `1.21.1` has only `FogRendererMixin`. Depth-occlusion for clouds needs a mixin (see memory `project_sunbathing_clouds`). |
| 5 | **Expression-driven models** (`expmodel`) | Models-Improvements / Custom-Item-Models | `ExpressionModel` (`content/expmodel`) | `1.21.1` has the base model system (`CustomModelsManager`, `WornModel`) but not the expression model. |
| 6 | **Sodium core-shader compat** | Shaders | `SodiumCompat`, `SodiumChunkRendererMixin` | Lets terrain/block shader replacement + expression uniforms work under Sodium (memory `project_sodium_core_shader_support`). May be MC/Sodium-version sensitive. |
| 7 | **EMF / ETF compat** (entity model & texture features) | Entity-Modifiers | `EmfCompat`, `EtfCompat` | `1.21.1` compat dir has only Iris + Seasons; `26.2` adds EMF/ETF (and moved `IrisCompat` under `compat/`). |

## Confirmed missing enhancements (extend an existing feature)

| # | Feature | Present-in-`1.21.1` base | Missing in `1.21.1` |
|---|---------|--------------------------|---------------------|
| 8 | **Particle emitters — entity & item-model sources** | Block/Particle emitters | `EntityParticleEmitter`, `ItemModelParticleEmitter`, `CustomParticleInstance`, `ModelParticleRenderState`, `ModelParticleRenderGroup`, `MultiExpressionParticleTicker` |
| 9 | **Particle rate limiting** | — | `TokenBucket`, `TokenBucketTracker`, `ParticleLimitMixin` (commits `particle limit fix`, `token stuff`) |
| 10 | **Particle hitbox debug renderer** | — | `ParticleHitboxDebugRenderer` |
| 11 | **Expression engine hardening** (`exp improvements`, commit `46e97839`) | old MVEL/exp4j engine | `ExpUtils.coerceLogicalOperands` (JS-truthiness for `&&`/`\|\|`/`!` — without it, `config()` etc. as a condition throws `ClassCastException`), `CodecUtils.LENIENT_DOUBLE/FLOAT` numeric-string fast path, centralized safe eval `PolyExp.executeDouble/executeBool`. See memory `project_mvel_expression_gotchas`. |

## What `1.21.1` already has (do **not** re-port)

- Colormaps, custom colors, biome/block/fluid/dimension/item/entity/sound/tab/lightmap modifiers,
  noise (`NoiseManager`), variant textures, slotify.
- **Base** post-process shaders: `PostShadersManager`, `PostChainEffect` (incl. recent
  `post shader custom samplers`, `pre build uniforms`).
- Custom particle types + Block/Particle emitters, rotation modes/providers, particle modifiers.
- Custom models (`CustomModelsManager`, `WornModel`), creative-tab backports, config system.
- **File-level conditions** via `utils/Parsed.CONDITION_CODEC` (NOT `ConditionUtils`): only
  `polytone_ignore`, `version`, `require_mods` (string/list), `require_config`.
- **Overlay-only** `polytone_condition` (incl. expression form) via `utils/ConditionUtils`
  `CODEC_OVERLAY`/`CODEC_EXPRESSION` — applies solely to `pack.mcmeta`
  `OverlayMetadataSection.OverlayEntry`, **not** to data files.

### Conditions gap — still to port to `1.21.1`
- **`polytone_condition` at the file level** (wrapper over the fields *and* the expression form).
  Today it only exists on pack overlays; the wiki Conditions section documents it as a file field.
- **`require_mods` `{mod, version}` range form** — file-level `require_mods` in `Parsed` is
  string/list only; the version-range object is 26.2-only.
- The MVEL logical-coercion fix (#11) — needed so expression conditions like `config(...) && ...`
  don't throw with `Object`-returning functions.

  In 26.2 all three live in the unified `common/ConditionUtils.CODEC_SINGLE_JSON`; the `1.21.1`
  port needs to fold `Parsed`'s file-level codec and `ConditionUtils` together the same way.

## Lower-confidence / needs a look

- **Contextual boss-bar color**: `BarColor`, `ContextualBarRendererMixin` — likely a small
  Gui/bar feature, but could be plumbing. Verify before porting.
- The bulk of `26.2`-only mixins are render-pipeline adaptation for the newer MC and are **not**
  features to backport.

## Suggested port order (most user value / least risk first)

1. Expression engine hardening (#11) — small, self-contained, unblocks crash-safe conditions.
2. Global Expressions (#2) — self-contained subsystem.
3. Particle enhancements (#8–10).
4. Shader expression uniforms (#3) — builds on the post-shader base already present.
5. Environment Attributes (#1) — larger, touches many mixins.
6. Sky/fog: sunbathing/clouds/flare (#4), EMF/ETF (#7), Sodium compat (#6) — most MC-version risk.
