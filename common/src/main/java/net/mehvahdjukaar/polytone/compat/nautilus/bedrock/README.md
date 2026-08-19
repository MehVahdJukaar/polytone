# Bedrock particle importer

Reads a Bedrock Edition particle file (`particles/*.json`, `format_version` 1.10+) and turns it into
Polytone resource-pack json. It lives inside the mod, not in a standalone tool, so the output can be
validated by the very codecs that will load it back (`CustomParticleType.CODEC`) instead of by a
hand-copied schema that drifts every time those codecs change.

Nothing in here runs at reload time. The entry point is `BedrockParticleImporter`, meant to be driven
by a Nautilus Studio import action (or a dev command) that writes the returned files to a pack.

## The two-sided mapping

Bedrock's unit of authoring is an **effect**: one file holding an emitter *and* the particle it
spawns. Polytone's unit is a **particle type**, with emitters living on whatever hosts them (a block,
a model bone, or another particle). The bridge is that last one - a particle with `particle_emitters`
*is* an emitter object - so one Bedrock effect converts to up to two Polytone particles:

| Bedrock | Polytone output |
|---|---|
| `emitter_*` components | `<name>_emitter.json`: an `invisible`, physics-less particle whose `lifetime` is the emitter lifetime and whose `particle_emitters[0]` carries the shape (as `x`/`y`/`z`) and rate (as `chance`/`count`) |
| `particle_*` components | `<name>.json`: the visible particle (initializer + ticker) |
| `basic_render_parameters.texture` + `uv` | a `TextureRequest` describing the atlas rect to crop, plus the vanilla sprite-list json |

If an effect has no meaningful emitter (instant rate, point shape, no lifetime) the emitter particle
is skipped and the visible particle is emitted alone, to be spawned by whatever host the author picks.

## Layout

- `model/` - a faithful, read-only mirror of the Bedrock format. Plain `RecordCodecBuilder`, *not*
  `SchemaRecord`: this is a foreign format we only ever parse, never edit in the pack editor, so it
  has no business carrying editor schema metadata.
- `molang/` - `MolangExpr` holds each value as it was written (literal or expression source) and
  defers the actual language translation to a `MolangTranslator`. The default one passes source
  through verbatim and flags it, which is enough to get the *structure* right while the real
  translator is still missing.
- `convert/` - the mapping itself. `PolytoneExpressions` is the single place that knows our
  expression dialect, `PolytoneParticleJson` the one that knows how a particle json is shaped
  (root vs `initializer` vs `ticker`) and validates the result against the real loading codec.

## Known holes

These are structural gaps, not laziness - each needs a feature on the Polytone side first:

- **Sphere / disc emission is approximated.** Emitter `x`/`y`/`z` are three independent expressions
  with no shared state, so a uniform point on a sphere (which needs one shared angle or a shared
  normalisation) can't be expressed. Boxes and points convert exactly; spheres fall back to a
  per-axis gaussian blob and warn. A native `shape` field on emitters would fix this properly.
  (There may be a trick here: `ParticleExpEnv` is reused across a tick's field expressions, so MVEL
  variables *might* survive between them. Unverified, and too fragile to build on.)
- **Parametric motion** needs the particle's spawn origin to offset from. Its `rotation` half does
  convert, which is what most packs actually use it for.
- **Expiration and timeline events, curves and flipbook UVs** have no counterpart and are reported
  as diagnostics. *Creation* events do convert, but only on a particle that draws nothing: the event
  fires once while our emitters fire every tick, so the particle's lifetime is pinned to one tick to
  make the two line up. That is the shape of Bedrock's own "emitter" effects.
- **Per-particle stable randoms**: Bedrock's `variable.particle_random_1..4` are fixed at spawn,
  `random.rand()` re-rolls on every call. Seeding a named custom in the initializer is the way to
  reproduce them, and is the first thing the Molang translator should do with them.

## Units

Bedrock is seconds / meters / degrees and evaluates per frame. We are ticks / blocks / radians and
evaluate 20 times a second. Every conversion goes through `BedrockUnits` - never inline a `* 20`
or a `Math.toRadians`.
