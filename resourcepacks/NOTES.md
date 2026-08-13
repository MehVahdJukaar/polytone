# Polytone dev resource packs

This folder holds resource packs we develop **alongside** the mod. It sits at the
repo root (next to `common/`) and is **not** a Gradle module - `settings.gradle.kts`
only includes `common`, `fabric`, `neoforge`, so nothing here is bundled into the
built mod jar. (Precedent: `research/`, `polytone_sample_pack/` also live at root.)

## Testing live in the dev client

Each pack is symlinked into the Fabric run dir so the running game loads it directly:

```
fabric/run/resourcepacks/sunbathing -> ../../resourcepacks/sunbathing   (repo root)
```

Minecraft refuses symlinked packs unless the target is allowlisted, so
`fabric/run/allowed_symlinks.txt` contains the repo-root `resourcepacks/` prefix.
Edit files here → they show up in-game after a resource reload (F3+T).

## Cross-version support via the overlay system (`sunbathing`)

26.2 **reversed the depth buffer** (far plane / sky clears to `0.0`, near = `1.0`;
`DepthStencilState.DEFAULT` uses `GREATER_THAN_OR_EQUAL`). Any post shader that reads
`InDepthSampler` must flip its sky test. So the same `.fsh` cannot work on both the
old (≤ 26.1.x) and new (26.2+) depth conventions.

Minecraft's pack **overlay** system solves this: the base `assets/` holds one variant,
and an overlay directory (applied only for a given pack-format range) overrides
specific files.

```
sunbathing/
  pack.mcmeta                 # declares support 69..90 + overlay entry
  pack.png
  assets/sunbathing/...       # BASE = OLD depth convention (formats 69..87 = 26.1.x)
    shaders/post/godrays.fsh    step(0.999999, depth) ; smoothstep(1.0, 0.999999, depth)
  reversed_z/                 # OVERLAY, applied for formats 88..90 (26.2+)
    assets/sunbathing/shaders/post/godrays.fsh
                                step(depth, 0.000001) ; smoothstep(0.0, 0.000001, depth)
```

`pack.mcmeta`:

```json
"overlays": { "entries": [ { "min_format": 88, "max_format": 90, "directory": "reversed_z" } ] }
```

### Why `min_format` / `max_format` (not `formats: [a,b]`)

`lastPreMinorVersion(CLIENT) = 64`. Overlay ranges **above 64 must** use
`min_format`/`max_format`; the legacy `formats: [a,b]` array only validates for
formats ≤ 64 (that's why `Visual Effects+`'s `formats: [34, 34]` overlay works but
ours can't use that form). Both 26.1.x (format 75) and 26.2 (format 88) are > 64, so
they use the new parser and read `min_format`/`max_format` fine.

### Format → Polytone version → pipeline (from decompiled `SharedConstants`)

| format | Poly version | post-shader pipeline                 | depth      | pack variant        |
|--------|--------------|--------------------------------------|------------|---------------------|
| 34     | 1.21.1       | **OLD** - individual uniforms, post JSON in `assets/<ns>/shaders/post/*.json`, `PostChainEffect` | old | `1.21.1/` overlay (TODO) |
| 75     | 1.21.11      | NEW - `PolyGlobals`/`Globals` std140 UBO blocks | old        | **base**            |
| 84     | 26.1.2       | NEW - UBO                             | old        | **base**            |
| 88     | 26.2         | NEW - UBO                             | reversed-Z | `reversed_z/` overlay |

`lastPreMinorVersion(CLIENT)=64`: format 34 (1.21.1) is ≤64, so its overlay entry uses the
`formats:[34,34]` array form (like `Visual Effects+`), and the pack must add a legacy
`supported_formats` block + lower `min_format` to 34 so 1.21.1's old mcmeta parser accepts it.

### 1.21.1 support - the `1.21.1/` overlay (IMPLEMENTED on the `1.21.1` branch)

The 1.21.1 branch (`PostChainEffect.java` + `PostShadersManager.java`) feeds the pack JSON to
**vanilla 1.21.1's OLD `PostChain`/`EffectInstance`** (GLSL 150, *individual* uniforms, program
JSONs under `shaders/program/`). `PostShadersManager.isNewFormatChain()` actively **rejects** the
base's 1.21.2+ `post_effect` format, so the pack can't run on 1.21.1 without ported files. The
`1.21.1/` overlay supplies them (all under `1.21.1/assets/sunbathing/`):

- **`post_effect/<name>.json`** - the vanilla OLD chain format. NOTE: the chain path is still
  `post_effect/…` (not `shaders/post/…`); `PostChainEffect.chainResource()` hardcodes
  `post_effect/<name>.json` and passes it straight to `new PostChain(…, rl)`. Content is old-format:
  `"targets": ["swap"]`, passes with `name`/`intarget`/`outtarget`. Two passes: the effect
  (`name: "sunbathing:<fx>"`, main→swap) then `{ "name": "blit", intarget: swap, outtarget: main }`
  (reuses vanilla `minecraft:blit`).
- **`shaders/program/<name>.json`** - vanilla program: `blend` (one/zero), `vertex: "screenquad"`
  (reuses vanilla `minecraft:screenquad.vsh` - computes `texCoord`), `fragment: "sunbathing:<name>"`,
  `attributes: ["Position"]`, `samplers` list, `uniforms` list. **Every uniform Polytone or PostPass
  pushes MUST be declared here** or `safeGetUniform` no-ops (DUMMY): `ProjMat`/`InSize`/`OutSize`
  (PostPass) + `PolyProjMat`/`PolyModelViewMat`/`PolySunAngle` + the `expression_uniforms` floats.
  Samplers: `DiffuseSampler` (PostPass auto-binds intarget), `InDepth`, and any custom `LensFlare`.
- **`shaders/program/<name>.fsh`** - `#version 150`, individual `uniform`s, NO std140 blocks.
  Renames vs the base UBO copy: `InSampler`→`DiffuseSampler`, `InDepthSampler`→**`InDepth`** (no
  `Sampler` suffix - this pipeline's setSampler name == the fsh uniform name), UBO block members →
  the block name as a plain uniform (`uSunRayIntensity`→`SunRayIntensity`, `uFlareStrength`→
  `FlareStrength`). Depth stays OLD convention (`step(0.999999, depth)`).
- **`polytone/post_shaders/<name>.json`** - 1.21.1 schema: adds `"use_depth_buffer": true` (how the
  OLD pipeline gets `InDepth` - a per-frame depth snapshot blit) + `"priority"`. `samplers`/
  `expression_uniforms` carry over. Overrides the base copy (which omits `use_depth_buffer`).

Namespace resolution: pass/program/fragment names are `"sunbathing:<x>"` - the fabric
`EffectInstanceMixin` rewrites `shaders/program/sunbathing:<x>.json` → `sunbathing:shaders/program/
<x>.json` while `POLYTONE_LOADING`. Unnamespaced names (`screenquad`, `blit`) pass through to
`minecraft:`.

**Cross-version `pack.mcmeta` (the fiddly part - verified against 1.21.1 & 1.21.11 codecs):**
1.21.1's `OverlayEntry` codec makes **`formats` mandatory** and ignores `min_format`/`max_format`;
the newer (PackFormat major/minor) codec dispatches to `validateNewFormat` when `min_format`/
`max_format` are present and reads **only** those (legacy `formats` is ignored, so a no-op `formats`
alongside them is harmless - but legacy `formats` values **>64** are rejected by the newer parser,
so ranges >64 can't be expressed via `formats`). Put ALL keys on each entry (like `vfx+dev`):
```json
"pack": { "pack_format": 75, "supported_formats": [34, 90], "min_format": 34, "max_format": 90 },
"overlays": { "entries": [
  { "directory": "reversed_z", "formats": [88, 90], "min_format": 88, "max_format": 90 },
  { "directory": "1.21.1",     "formats": [34, 34], "min_format": 34, "max_format": 34 }
]}
```
**GOTCHA (this bit us - pack went "not recognized" on every modern version):** each overlay's
`formats` range **must equal** its `min_format`/`max_format` - do NOT use a dummy `[0, 0]`. Once any
overlay's effective min major version is ≤ `lastPreMinorVersion` (64) - which the `1.21.1` entry (34)
forces - `PackFormat.validateHolderList` runs every entry through the **strict** `requireNewFormat`
path, and `validateNewFormat` errors with "version declaration mismatch between formats (from 0) and
min_format (88)" if `formats` ≠ `min_format..max_format`. That error fails the whole `pack.mcmeta`
parse, so the pack silently disappears from the resource-pack screen. `[88, 90]` also still never
matches 1.21.1's format (34), so the old parser keeps skipping `reversed_z` correctly.
- 1.21.1 parser: accepts pack via `supported_formats` (pack_format 75≠34); applies `1.21.1` overlay
  (`formats:[34,34]` matches), skips `reversed_z` (`formats:[88,90]` never matches 34). Parses fine - every entry has `formats`.
- Newer parser: applies `reversed_z` via `min_format`/`max_format` 88–90; skips `1.21.1`
  (34–34 ≠ current). `formats` must match the min/max (strict path is on because 34 ≤ 64).

Polytone ALSO extends overlay entries with an optional `polytone_condition` (config/mods/expression
gate via `OverlayEntryMixin` → `isApplicable`) - used by `vfx+dev` for config-toggled overlays; not
needed for pure version gating.

Still TODO: verify in-game on the `1.21.1` dev client (F3+T reload; check log has no
`isNewFormatChain` rejection and rays/flare render).

## Config-driven quality / visual settings (Polytone config system)

Polytone lets packs ship user-facing sliders/toggles that feed shader uniforms. Chain:
**`config_entries/*.json` (in-game slider) → `config('ns:id')` in an MVEL expression →
`expression_uniforms` float UBO → GLSL.**

1. `assets/sunbathing/polytone/config_entries/ray_intensity.json` (number → slider):
   ```json
   { "default_value": 1.0, "min": 0.0, "max": 2.0, "step": 0.05,
     "value_translation": "sunbathing.config.ray_intensity" }
   ```
   (bool `default_value` → toggle; string → cycler; `presets` optional.)
2. `.../polytone/post_shaders/godrays.json` gains:
   ```json
   "expression_uniforms": { "RayIntensity": "config('sunbathing:ray_intensity')" },
   "activation_condition": "config('sunbathing:god_rays_enabled') && g.skyType()==1 && g.rain()<=0.1"
   ```
3. In `godrays.fsh`, replace the hardcoded `const float GodRayIntensity` etc. with, per uniform:
   - 26.x (UBO): `layout(std140) uniform RayIntensity { float value; };`
   - 1.21.1: `uniform float RayIntensity;`

Caveat: `expression_uniforms` are **single floats only** - no int/array uniforms. `GodRaySamples`
(a loop bound) must arrive as a float and be `int(...)`-cast in GLSL. Sliders live in Polytone's
config screen (config button); changing one saves to `config/polytone_options.json` and reloads.
Expression namespaces available: `g.*` (global: time, dayTime, skyType, rain, season…),
`p.*` (player/position: biome, health, x/y/z…), `c.*` (camera), `r.*` (random), `config('ns:id')`.

### Wired into sunbathing (config_entries → post_shaders/godrays.json → godrays.fsh)

| config id (`sunbathing:`) | type / range        | shader UBO block / use                     |
|---------------------------|---------------------|--------------------------------------------|
| `enabled`                 | bool (true)         | gates `activation_condition`               |
| `sun_intensity`           | 0–2, step .05 (0.5) | `SunRayIntensity` → sun ray strength       |
| `moon_intensity`          | 0–2, step .05 (0.1) | `MoonRayIntensity` → moon ray strength     |
| `quality`                 | 4–60, step 1 (30)   | `RayQuality` → `int()` loop sample count   |
| `density`                 | .5–1, step .01 (.92)| `RayDensity` → ray reach                    |
| `decay`                   | .85–1, step .005(.99)| `RayDecay` → per-sample falloff            |

Both `godrays.fsh` copies (base + `reversed_z/`) declare the 5 UBO blocks - keep them in sync
(they differ only in the two reversed-Z depth lines). Slider labels: `assets/sunbathing/lang/en_us.json`.

### Config lang keys (GOTCHA - verified in `OptionHolder.create`)

The option **name** is looked up at **`config.<namespace>.<id>`** (via `id.toLanguageKey("config")`),
NOT `<namespace>.config.<id>`. Tooltip is **`config.<namespace>.<id>.tooltip`**. Example:
`sunbathing:sun_intensity` → `"config.sunbathing.sun_intensity"` + `".tooltip"`.

`value_translation` is **not** the label - it's an optional *format string* for the VALUE
(`Component.translatable(key, value)`, so it needs a `%s`). **Omit it** and the slider shows the
raw number (`value + ""`). Setting it to a plain name key makes the value slot render that key
literally - which is the bug that showed "no values on sliders".

## Lens flare (second effect in `sunbathing`, procedural, no new infra)

Screen-space flare reusing the same ingredients as godrays: sun screen pos from `PolyGlobals`,
occlusion from `InDepthSampler`, config gate. Files: `post_effect/lens_flare.json`,
`polytone/post_shaders/lens_flare.json`, `shaders/post/lens_flare.fsh` (+ `reversed_z/` copy),
`config_entries/lens_flare.json` (a 0–1 **intensity** slider, default 0 = off, driving the
`FlareStrength` UBO via `expression_uniforms`; `activation_condition` uses `config(...) > 0`). The base/overlay copies differ only in the
one occlusion depth test (`step(0.999999,d)` ↔ `step(d,0.000001)`) - same reversed-Z rule. Effect =
a **single medium iris ring** with a chromatic rainbow band (`spectrum()` hue ramp across the ring
thickness), placed along the sun→center axis so it parallaxes as you look around; gated by
daytime × edge-fade × soft 5-tap occlusion, kept subtle (`FLARE_STRENGTH ≈ 0.1`). Tuning consts
(`RING_POS`/`RING_RADIUS`/`RING_THICKNESS`/`FLARE_STRENGTH`) are fixed for now - promote to
`expression_uniforms` sliders later the same way godrays did.

## Versioning (telling users which revision they have)

Convention in this ecosystem (see `Visual Effects+`: `§e■ 1.0.0 : VFX+ BETA`): put a
**revision string in the pack `description`**, which users see in the resource-pack
list. Sunbathing currently shows `§8v1.1`. Bump it in `pack.mcmeta` (base only - the
overlay has no `pack.mcmeta`, so the description is single-source) on each release.

## Building a distributable zip

Zip the pack contents at root level (so `pack.mcmeta` is at the zip root, not nested):

```
cd resourcepacks/sunbathing && zip -r -X ~/Desktop/sunbathing.zip . -x '.*'
```
