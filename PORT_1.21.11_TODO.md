# 1.21.1 → 1.21.11 forward-port: what to check & what's missing

Forward-port of the 1.21.1 commit range (`shadow pass` … `bubble`) onto branch `1.21.11`.
Everything below is **compile-verified and IDE-clean** on all three modules (`:common`,
`:fabric`, `:neoforge`), but **nothing has been run in-game** - all runtime/visual behaviour
still needs an actual check.

## Commit stack (local, NOT pushed) actually noy sure about this. stuff was pushed

```
e0cde738  editor custom-particle preview (sandbox engine)
03f593d2  editor gui-modifier + biome-scene previews
eb41f64d  editor noise preview
82dc3743  editor colormap preview + workspace ref bridge + dep bump (codecui 1.3.1 / nautilus 1.4.2)
8df880f0  companion/texture-parts redesign
a6712823  expressions + editor nudge + colormap foundation
cc08e693  shadow system
```

---

## TO CHECK IN-GAME (compiles, but unverified visually/behaviourally)

### Shadow system
- [ ] Shadows render correctly under **vanilla** terrain (ported to the 1.21.11 GpuDevice/RenderPass
      stack; never run on 1.21.11).
- [ ] Entity + block-entity shadow casters, the texel-snap anti-shimmer, and the reuse/re-align on
      `update_interval` frames.
- [ ] `colors.json "entity_shadows": false` still cancels the vanilla blob shadows (now via
      `ShadowFeatureRendererMixin`).
- NOTE: **no shadows under Sodium** until the Sodium path is ported (see Missing).

### Editor previews (check in the Nautilus editor)
- [ ] **Noise** - grayscale field renders; caption seed/octaves.
- [ ] **Gui-modifier** (live preview on the real screen):
  - [ ] unconditional screen-render hook + `nextStratum` layering + the 2D-stack (`Matrix3x2fStack`)
        sprite translate render correctly and don't disturb normal screens (overlay no-ops when idle).
  - [ ] repeated `pushPreview` does NOT accumulate slot offsets (slot base-snapshot / reset should
        prevent drift).
  - [ ] the picker's click-swallow only triggers while picking is enabled (normal play untouched).
- [ ] **Particle** (sandbox engine) - HIGHEST-RISK preview:
  - [ ] **render-matrix / billboard placement** in the offscreen viewport (the preview camera view/proj
        vs. the camera-relative quads from `extract`) - most likely thing to need a tweak.
  - [ ] emitter children appear IN THE PREVIEW and NOT in the live world while the editor is open.
  - [ ] model-particle types (via `ModelParticleRenderGroup`), not just quad particles.
  - [ ] a particle actually **animates** with `custom_particles_async` ON (default true) - the whole
        reason for the gated sync hooks.
- [ ] **Biome** - PARKED (see Missing); if kept: fog/sky backdrop coverage, `debugQuads` depth/blend
      for translucent water, sky→fog gradient tuning, and the tint layer (`ItemBlockRenderTypes`) for
      grass/leaves.

### General
- [ ] The dep bump (codecui `1.21.11-1.3.1`, nautilus_studio `1.21.11-1.4.2`) is consumed cleanly at
      runtime, not just at compile.

---

## MISSING / DEFERRED

### 1. Biome scene preview - PARKED (committed in `03f593d2`, held)
Reads sky/fog from the 26.x **environment-attribute** map (`biome.getAttributes()` +
`modifier.attributeModifications()`, static read via `EnvironmentAttributes.SKY_COLOR/FOG_COLOR`).
Owner wants to **rethink the env-attribute approach broadly** before finalizing, so this is left
committed-but-unpushed and may be revised or backed out when that redesign lands.

### 2. Sodium shadow path - NOT ported
On 1.21.1 the shadow pass had a Sodium fallback (`SodiumShadowRenderer`,
`SodiumLightVolumeFrustum`, `SodiumWorldRendererShadowAccessor`) that re-culled Sodium's terrain
against the light volume. On 1.21.11, Sodium 0.8.12 hijacks the terrain pipeline differently and the
1.21.1 `renderSectionLayer`-invoker trick is gone. **Result: shadows do not cast under Sodium**
(the dev runtime has Sodium 0.8.12). Needs investigation of what Sodium 0.8.12 exposes on 1.21.11.

### 3. Dormant features (exist on 1.21.1, absent on 1.21.11 - not ported)
- **Variant Textures** (`VariantTextureManager` / `VariantTexture`) - the feature doesn't exist on
  1.21.11 at all. The `ContentManager.Spec` / `wikiPage` base it needs is now in place, so it can
  adopt it when the feature itself is ported.
- **Item-tint colormap** (the `TINT` texture part via item `getTint` / `ofItemColor`) - item-tint
  doesn't exist on 1.21.11; the item manager carries only the `_bar` part.
- **Dimension-editable CODEC widening** (`DimensionEffectsManager` Decoder→Codec) - would make
  dimension modifiers editable in the editor.
- **TextureSidecars newer nautilus API** (`childrenByName` / `referencedFirstPresent`) - kept the
  inlined `PackEditor.sidecarsFromSpec` projection instead; revisit if desired.

### 4. Moot
- `LeashTexture` - 1.21.11 rewrote leash rendering; the 1.21.1 delta was comment-only, nothing to port.

---

## Notes
- The 3 particle preview hooks (`tickSync`, sync spawn-init, emitter sink) are all gated by a
  render-thread `ParticlePreviewMode` thread-local, so normal gameplay and the async particle workers
  are byte-for-byte unchanged when the preview is idle. The emitter sink also fixes a latent bug where
  preview emitters would have spawned particles into the live world.
- Editing the sibling `nautilus_studio` (`../pack_editor`) is fair game if a preview needs an API fix
  or hits a nautilus bug - none was needed for these previews.
