# Polytone Sample Pack

One small file per feature. Copy the ones you need into your own pack and rename the targets.
Every field is documented on the [wiki](https://github.com/MehVahdJukaar/polytone/wiki); this file
only says what each example does and where to stand to see it.

Needs Minecraft 1.21.11 or newer. Reload with F3+T after editing.

The pack has two parts:

- the base files, loaded always. Plain values, no scripting.
- the `advanced/` overlay, loaded only when you turn on **Advanced Examples** from the paintbrush
  button on the resource pack screen. Expressions, custom particles, an expression model, a post
  shader.

## Where files go

- `assets/minecraft/polytone/<feature>/<target>.json` targets the vanilla thing named by the file
  (`biome_modifiers/swamp.json` -> `minecraft:swamp`). No `targets` field needed.
- `assets/polytone_sample/polytone/...` is the pack's own namespace. Files here either define
  something new (colormaps, configs, particles) or use an explicit `targets` list. Never put an
  explicit-target file under `minecraft/`: it would overwrite another pack's file at that path.

## Coming from OptiFine

| OptiFine file | Here |
|---|---|
| `optifine/colormap/sky0.png` (grid) | `colormaps/optifine_sky.json` + `.png`, applied by `biome_modifiers/optifine_grid_colors.json` |
| `optifine/colormap/fog0.png` (grid) | `colormaps/optifine_fog.json` + `.png`, same file |
| `optifine/colormap/underwater.png` (grid) | `colormaps/optifine_underwater.json` + `.png`, same file |
| `optifine/colormap/water.png` (vanilla format) | `colormaps/tropical_water.json` + `fluid_modifiers/water.json` |
| `optifine/colormap/birch.png`, `pine.png`, `swampgrass.png` | a standalone colormap + a block modifier, like `autumn_leaves.json` |
| `optifine/colormap/custom/*.properties` | `block_modifiers/<block>.json` with a `colormap` |
| `optifine/color.properties` | `colors.json` |
| `optifine/lightmap/world0.png` | `dimension_modifiers/overworld.json` (attributes, the lightmap texture is gone in 1.21.5+) |
| per-biome sky/fog in `.properties` | `biome_modifiers/<biome>.json` |

The grid format needs to know which column is which biome. Numeric biome ids do not exist any more
and mods shuffle registry order, so `biome_id_mappers/vanilla_biomes.json` pins every vanilla biome
to a column (`texture_size: 64` makes the values pixel columns). The three grid colormaps reference
it. Column x = biome, row y = height (`y_level`, 0-128 blocks), same layout OptiFine used.

`optifine_grid_colors.json` targets every biome (`".*"`) in the base layer group with
`priority: -2`. That layer runs before the day/night timeline and the weather group, so nights and
rain still darken your colors, and the per-biome files (`cherry_grove.json`) still win over it.

If you are not porting an existing grid texture, prefer a per-biome file. It is one line per color
and nothing to keep in sync.

## Base files

| File | What it does |
|---|---|
| `polytone_sample/.../biome_id_mappers/vanilla_biomes.json` | Biome to column table for the grid colormaps above. |
| `polytone_sample/.../colormaps/optifine_*.json` + `.png` | The three grid colormaps: `biome_id` on x, `y_level` on y. |
| `polytone_sample/.../biome_modifiers/optifine_grid_colors.json` | Applies them to sky, fog and water fog in every biome. |
| `minecraft/.../biome_modifiers/cherry_grove.json` | Pink sky and fog in cherry groves, light blue water. Sky and fog are `attributes_modifiers`; water color is still a plain biome field. |
| `minecraft/.../biome_modifiers/swamp.json` | Clearer swamp water: sets `water_fog_color`, `REMOVE`s the vanilla `water_fog_end_distance` layer, and `multiply`s `fog_start_distance`. Set vs modify vs remove in one file. |
| `polytone_sample/.../biome_modifiers/biomesoplenty_fallback.json` | Regex target `biomesoplenty:.*`, `require_mods` so it does not error without the mod, `priority: -1` so a per-biome file still wins. |
| `minecraft/.../dimension_modifiers/overworld.json` | Warm torches, blue nights (what a lightmap did before 1.21.5). Lives in `post_process` because the day/night timeline runs after `base` and would override it. Also lowers clouds in `base`. |
| `minecraft/.../dimension_modifiers/the_nether.json` | Brighter nether (`ambient_light`) with a darker red fog. |
| `polytone_sample/.../colors.json` | Dye, map, text, xp bar, effect and particle colors. Same thing as `color.properties`, json form. |
| `polytone_sample/.../colormaps/autumn_foliage.json` + `.png` | A standalone colormap with the vanilla temperature/downfall sampler, `triangular` like grass. |
| `polytone_sample/.../block_modifiers/autumn_leaves.json` | Applies that colormap to every block in `#minecraft:leaves`. One colormap, many blocks: reference it, do not inline it per block. |
| `polytone_sample/.../colormaps/tropical_water.json` + `.png` | Water tint by climate. |
| `polytone_sample/.../fluid_modifiers/water.json` | Targets both `water` and `flowing_water` (they are two fluids). |
| `minecraft/.../block_modifiers/stone.json` | Stone sounds like amethyst. Changing a sound is also the quickest way to check a block modifier is being read at all. |
| `minecraft/.../block_modifiers/glowstone.json` | Glowstone sheds end rod particles from its faces. |
| `minecraft/.../item_modifiers/diamond.json` | Epic rarity (purple name) plus a tooltip line. |
| `minecraft/.../item_modifiers/iron_pickaxe_bar.png` | Durability bar goes green to red. A png named `<item>_bar.png` is enough. |
| `polytone_sample/.../overlay_modifiers/wide_hotbar.json` | Hotbar sprite 10px wider. |
| `minecraft/.../creative_tab_modifiers/combat.json` | Removes trident and mace from the combat tab. |
| `polytone_sample/.../config_entries/*.json` + `lang/en_us.json` | The two config entries shown on the paintbrush screen. `advanced` gates the overlay in `pack.mcmeta`, `night_darkness` is read by an expression in the overlay. |

## Advanced overlay

| File | What it does |
|---|---|
| `dimension_modifiers/overworld_sky.json` | Explicit `targets` on the overworld so it stacks with the base file. Sky color from a colormap, night brightness scaled by the `night_darkness` config (`v` is the vanilla value), twice the stars on a new moon via a global expression, orange sunsets, clouds lower in rain. |
| `global_expressions/moon_phase.json` | Computes the moon phase once a second. Used as `polytone_sample_moon_phase` above. |
| `colormaps/sky_by_height.json` + `.png` | Sampled by `day_time` on x and the block height on y. Sky gets darker the higher you climb. |
| `colormaps/noisy_grass.json` + `block_modifiers/noisy_grass.json` | Grass tinted by perlin noise on x and temperature on y. |
| `particles/ember.json` + `custom_particles/ember.json` | A new particle type: additive, unlit, drifts up and fades with age. Uses the vanilla `glow` sprite so no texture is needed. |
| `block_modifiers/ember_blocks.json` | Magma blocks and campfires emit it. |
| `minecraft/.../entity_modifiers/zombie.json` | Soul particles off the zombie head bone. |
| `gui_modifiers/cleric_trades.json` + `textures/gui/sprites/cleric_trades_bg.png` | Purple frame around the trade screen, but only for clerics (`condition` on `global.lastInteractedEntity`). |
| `minecraft/blockstates/cobblestone.json` | `polytone:expression` blockstate: cobblestone renders as mossy cobblestone in wet biomes. |
| `post_chains/sunset_tint.json` + `post_effect/` + `shaders/post/sunset_tint.fsh` | A one-pass post shader with a warm tint. Its strength is an expression uniform peaking at sunset. |

## Things this pack does on purpose

- Colors are `#rrggbb` strings. Integers work too but nobody can read them.
- Anything vanilla animates over the day (sky light, sunset color, star brightness) is edited in
  `post_process`, never in `base`.
- Scripts use the `o.` / `g.` / `r.` contexts. The uppercase `POS_X` / `TIME` style is the old exp4j
  syntax and is not used here.
- No `grid` colormap. That format depends on numeric biome ids and breaks with mods.
