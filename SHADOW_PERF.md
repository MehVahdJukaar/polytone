# Shadow map performance work (`shadow-perf` branch)

Branched off `1.21.1`. Nothing committed, nothing pushed. Everything below is uncommitted in the tree.

The starting point was a comparison against how Iris renders its shadow map. The short version of that
comparison: Iris never builds a second view of the world for the shadow pass and never throws away the
first one, while our pass rebuilt the caster set from scratch every frame, rendered it with full colour
terrain shaders, and (on Sodium) destroyed the camera's render lists in the process.

**Nothing here has been run or profiled.** All three modules compile (`:common`, `:fabric`, `:neoforge`;
`forge/` is a stale directory and not in `settings.gradle.kts`). Every performance claim is structural
reasoning about work removed, not measurement. See "Needs testing" at the bottom.

---

## The new shaders

Four new files under `common/src/main/resources/assets/minecraft/shaders/core/`:

| File | Role |
|---|---|
| `polytone_shadow_terrain.vsh` | shared vertex stage for both variants |
| `polytone_shadow_terrain.fsh` + `.json` | opaque layer (`RenderType.solid()`) |
| `polytone_shadow_terrain_cutout.fsh` + `.json` | cutout layers (`cutoutMipped()`, `cutout()`) |

### What they do

A shadow map stores one thing: the depth of the nearest occluder along each light ray. Everything the
vanilla terrain shaders compute besides position is dead weight in that pass - lightmap fetch, overlay,
fog, vertex tint, the atlas sample itself. These programs strip it all out.

The **vertex stage** keeps only what positioning needs:

```glsl
gl_Position = ProjMat * ModelViewMat * vec4(Position + ChunkOffset, 1.0);
texCoord0 = UV0;
```

`ChunkOffset` is uploaded per section by `drawLayer`, exactly as vanilla does, so the already-compiled
chunk VBOs can be replayed untouched. `texCoord0` is only there for the cutout fragment stage.

The **opaque fragment stage** does nothing at all beyond writing a constant. Critically it contains no
texture fetch and no `discard`, which is what lets the hardware keep early depth testing enabled. A
`discard` anywhere in a fragment shader forces late-Z for the whole program, so the two variants are
split rather than sharing one shader with a branch.

The **cutout fragment stage** re-adds the atlas fetch, and only for the alpha test:

```glsl
if (texture(Sampler0, texCoord0).a < 0.1) discard;
```

Without it, leaves, grass and glass panes would cast solid-block shadows. This variant pays for late-Z;
the opaque layer, which is the bulk of the geometry, does not.

Both write `fragColor = vec4(1.0)`, which goes nowhere: the shadow framebuffer has `GL_NONE` bound as its
draw buffer (see below), so the colour output is discarded and only `gl_FragDepth` survives. The write is
kept because GLSL requires the output to exist.

### How they are bound

Loaded lazily on first use in `ShadowMapRenderer.depthShader(mc, cutout)` via
`new ShaderInstance(resourceManager, name, DefaultVertexFormat.BLOCK)`, then cached. A load failure sets
a latch and the pass silently falls back to whatever `renderType.setupRenderState()` bound, which still
produces a correct (just slower) map.

Two details that are easy to get wrong if this is ever touched again:

- **The `.json` attribute list must match `DefaultVertexFormat.BLOCK` exactly** (`Position`, `Color`,
  `UV0`, `UV2`, `Normal`), even though the vertex shader only reads two of them. The attribute list
  describes the buffer layout, not what the shader consumes. Trimming it desynchronises the vertex
  bindings from the chunk VBOs.
- **`Sampler0` is set explicitly** before `apply()`. `ShaderInstance.apply()` binds samplers from its own
  `samplerMap`, populated by `setSampler`, *not* from the `RenderSystem` globals that
  `setupRenderState()` writes. Relying on the global would leave the cutout test sampling whatever was
  bound last.

---

## Everything else that changed

**`ShadowCasterVolume.java` (new)** - the main win. Previously the caster set was a box around the
camera, symmetric in every direction, so it included the entire hemisphere *behind* the viewer. This
adds the actual caster volume: the camera frustum swept away from the light. Frustum half-spaces whose
normal faces away from the light are kept (the others can never constrain anything, since the shadow ray
escapes them without bound), then the volume is closed sideways by sweeping each silhouette edge - every
boundary between a kept and a dropped face - into a plane parallel to the light direction. Anything
outside cannot cast into anything you can see.

The volume test is conservative by construction: it never reports a false "outside". It is disabled when
`update_interval > 0`, because a reused map plus a camera that has since turned would leave holes where
the volume was never filled. `EDGE_MARGIN` (4 blocks) pads every plane so PCF and depth bias cannot walk
a lookup off the edge of real occluder data.

**Depth-only framebuffer** - `glDrawBuffer(GL_NONE)` / `glReadBuffer(GL_NONE)` on the shadow FBO at
creation. Removes the per-frame full-target colour clear and the colour write traffic for every shadow
fragment. Framebuffer state, so it sticks for the target's lifetime.

**`SodiumLightVolumeFrustum` now delegates** to `ShadowCasterVolume` instead of re-implementing the
separating-axis test, so the Sodium terrain replay is narrowed by the same caster planes as the vanilla
path. It previously had the box test duplicated inline and got no caster narrowing at all.

**Block entities on the vanilla path** come from the collected sections' `getRenderableBlockEntities()`
rather than walking every loaded chunk's block-entity map. That map holds every block entity, renderable
or not; the section lists are already filtered and already caster-culled.

**New settings** in `ShadowMapSettings`: `render_entities` and `render_block_entities`, both defaulting
to `true`. Entity and block-entity models are rebuilt on the CPU for every shadow render, so a pack that
only wants terrain shadows can now turn off the largest remaining per-frame cost.

---

## What is missing

### 1. Sodium still clobbers the camera render lists (biggest remaining item)

`SodiumShadowRenderer.reCull` calls `rsm.update(...)` + `finalizeRenderLists(...)`, which overwrite
Sodium's `renderLists` and `taskLists`, and then `restoreCameraList()` calls `markGraphDirty()` to force
a rebuild. The result is **two full occlusion traversals and two render-list builds per frame instead of
one**, even standing perfectly still, plus one frame of chunk-build priorities derived from the sun's
viewpoint rather than the camera's.

Iris solves this with `@Unique` shadow copies of `renderLists`/`taskLists` and `@Redirect`s on every
access to them while the shadow pass is active, so the camera's state is never touched, plus outright
cancellation of `updateChunks`/`uploadChunks` during the pass. That is the fix to build.

Deliberately not attempted here: Sodium is a `compileOnly` dependency whose internals move between
versions, and this cannot be validated without running the game. Getting it wrong breaks the *main*
terrain pass, not just shadows. It wants a session where it can actually be tested.

### 2. The vanilla path still linearly scans every section

`collectShadowSections` walks all of `viewArea.sections` every frame. The caster volume made each test
reject far more, but the scan itself is still O(all loaded sections): roughly 15k entries at render
distance 12, 101k at 32. Quadratic in render distance and independent of what is on screen.

Iris instead reuses vanilla's own `setupRender` traversal, fed the shadow frustum, wrapped in a
save/restore of the culling state. If that is ported, note that the swap must save
`lastCameraPitch`/`lastCameraYaw` alongside the visible-section list, or vanilla's "camera moved enough"
check gets poisoned and the main pass starts re-traversing at the wrong times. It would also make the
block-entity reuse below free.

### 3. Block entities on the Sodium path

Sodium leaves the vanilla sections uncompiled, so `drawBlockEntities` still falls back to scanning every
loaded chunk within lateral coverage and walking its full block-entity map. Fixing this properly means
pulling the list out of Sodium's own render lists.

### 4. Smaller items, none attempted

- **`update_interval` still defaults to `0`** (re-render every frame). Raising it is the single cheapest
  frame-time win available, but it is a latency-for-frames trade rather than an optimisation, and it was
  left alone deliberately so it cannot mask whether the structural work above actually helped. The
  amortisation path itself already exists and works.
- **Resolution still defaults to 2048.** Iris defaults to 1024, which is a quarter of the fill.
- **The colour texture is still allocated** (16 MB at 2048²) despite never being written.
  `TextureTarget` has no constructor that skips it; dropping it means a hand-rolled FBO.
- **Entities render into the shared `bufferSource` mid-`renderLevel`, unsorted.** Iris uses a dedicated
  `RenderBuffers` and sorts entities by type before rendering. A dedicated buffer plus a one-line sort
  captures most of that.
- **No `render_player` toggle and no entity distance multiplier.** Iris gates the player separately
  (off by default) and culls entities against a tighter frustum than terrain.
- **Hardware depth comparison** (`GL_COMPARE_R_TO_TEXTURE` + `sampler2DShadow`) is not used and probably
  should not be: `PostChainEffect.SHADOW_SAMPLER` binds `InShadow` as a plain `sampler2D`, so changing it
  is a pack-facing API break.
- **Translucent geometry is intentionally not rendered.** A shadow map stores opaque occluder depth.

---

## Needs testing

Compilation is the only verification that has happened. Worth checking in game before trusting any of it:

- **The depth shaders actually load.** A failure falls back silently and correctly, so the pass will look
  right while none of the shader win is present. Check the log for the fallback message.
- **Cutout foliage still casts see-through shadows.** If the `Sampler0` handoff is wrong, leaves and
  grass cast solid-block shadows.
- **Shadows do not clip at screen edges when turning quickly.** That is the caster volume being too
  tight; raise `EDGE_MARGIN`.
- **The Sodium path still renders terrain correctly** after the frustum was reworked to delegate.
- **`update_interval > 0` still works**, since that path disables the caster volume and takes a different
  branch.
