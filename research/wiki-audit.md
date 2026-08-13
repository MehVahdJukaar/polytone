# Wiki Consistency Audit (first parse)

Living checklist for the incremental wiki cleanup. Companion to `wiki-style-guide.md`. Covers the
live pages in `wiki/` (the `mocups_i_dont_like/` folder is discarded, ignore it). Check items off as
they're fixed. This is a *first parse*: it catalogs patterns, it is not yet exhaustive per-page.

Severity: **[BUG]** = factual/broken, fix first. **[STYLE]** = consistency. **[CLARITY]** = reads poorly.

## [BUG] Broken or wrong internal links

- [x] `Fluid-Properties-Modifiers.md` links to `Custom-Biome-Effects` - no such page. Fixed to
      relative `[Biome Effect Modifiers](Biome-Effect-Modifiers)`.
- [x] `Biome-Effect-Modifiers.md:113` / `Dimension-Effects-Modifiers.md:127` linked to
      `Environment-Attributes` vs `Environment-Attributes-Modifiers` inconsistently. Resolved by
      renaming the page to `Environment-Attributes.md` (matches its H1) and pointing both links at
      the relative slug `Environment-Attributes`.

## [BUG] Copy-paste leaks (wrong content pasted from another page)

- [x] `Entity-Modifiers.md:10,12` - the "explicit targeting" boilerplate said
      `polytone/biome_modifiers/` and "valid **Biome** ids". Fixed to `entity_modifiers` and
      "valid Entity ids". (Still a symptom of the hand-pasted targeting block; see the targeting
      sweep below.)

## [STYLE] Internal links: full URL vs. relative

14 pages hardcode `https://github.com/MehVahdJukaar/polytone/wiki/Page` for internal links; the new
pages (Home, Shared-Concepts) use relative `[Text](Page-Name)`. Pick one (relative is shorter and
survives repo moves) and convert all.

- [ ] Convert full-URL internal links to relative across: Custom-Colors, Fluid-Properties,
      Item-Modifiers, Biome-Effect, Scripting-Expressions, Custom-Item-Models, Block-Properties,
      Custom-Particle-Types, Extra-Features, Particle-Modifiers, Math-Expressions, Dimension-Effects,
      Colormaps, Polytone-Configs.

## [STYLE] Page title (H1) does not match page name

- [x] `Particle-Modifiers.md` - had **no H1**. Added `# Particle Modifiers`.
- [x] `Custom-Particle-Types.md` - H1 "Custom Particle Type" (singular) → "Custom Particle Types".
- [x] `Environment-Attributes` - page renamed so slug matches H1 "Environment Attributes"; links
      fixed. (Legacy `Math Expressions` H1 also fixed; the two expression systems stay separate,
      content untouched per owner.)
- [x] `Screen-Sprite-Modifiers.md` - H1 "Sprites Modifiers" → "Screen Sprite Modifiers".
- [x] `Math-Expressions.md` - H1 "Expressions" → "Math Expressions" (matches slug). It's the
      legacy exp4j system; `Scripting-Expressions` is the current MVEL one. Both kept, style only.
- [ ] `Models-Improvements.md` - H1 "Model Improvements" vs slug "Models-Improvements" (minor,
      cosmetic, left for now).

## [STYLE] "JSON" section-heading casing is all over the place

`Json Format`, `Json Structure`, `JSON Structure`, `Json Syntax`, `Json content`, `Json Example`.
- [ ] Pick one canonical heading (proposal: `## JSON Format`) and apply everywhere.

## [STYLE] "Getting Started" casing / naming

`Getting Started` vs `Getting started`; some pages open with `## Json Format` instead. 
- [ ] Standardize the opening section name and casing (proposal: `## Getting Started`).

## [STYLE] Notes/disclaimers as headings instead of alert boxes

Several pages use `## Note`, `### Note`, `### Reminder`, `### Disclaimer` as real headings (they
pollute the sidebar/TOC). Style guide says these should be `> [!NOTE]` / `[!WARNING]` / `[!CAUTION]`.
- [ ] Convert in: Biome-Effect (`## Note`), Colormaps (`### Note`, `### Reminder`),
      Custom-Sound-Events (`### Disclaimer`), Variant-Textures (`### Disclaimer`),
      Scripting-Expressions (`### Disclaimer`).

## [STYLE] Heading levels: H1 used mid-page as a divider

Pages should have exactly one H1 (the title); sections are `##`/`###`.
- [ ] `Creative-Tab-Modifiers.md:197` `# Custom Creative Tabs` (mid-page H1).
- [ ] `Shaders.md` uses `#` for `Extra Shader Uniforms` (42) and `Expression Uniforms on Any Shader`
      (108).
- [ ] `Math-Expressions.md:188` `# Custom Noises` (mid-page H1).

## [STYLE] Per-page "Preliminary concepts" now redundant with Shared-Concepts

- [ ] `Creative-Tab-Modifiers.md:13` and `Custom-Item-Models-(CIM).md:19` have their own
      "Preliminary concepts" sections. Trim to a link to the relevant Shared-Concepts anchors.

## [STYLE] "Explicit targeting" re-explained on every feature page

Biome, Block, Creative-Tab, Dimension, Fluid, Item, Particle, Variant-Textures each hand-write an
`### Explicit targeting` block. Per the style guide this should be ~2 lines + a link to
`Shared-Concepts#targeting`. (This is also the source of the Entity copy-paste bug above.)
- [ ] Replace the boilerplate with the short + link pattern, page by page.

## [CLARITY] Typos / wording (spot-checked, not exhaustive)

- [ ] `Colormaps.md:293` `## Compound Compound Colormap` (doubled word).
- [ ] Many pages have minor typos ("partice", "yout", "bare with me", "reccommended"). Low priority;
      fix opportunistically while editing a page for other reasons.

## STATUS: canon applied to all feature pages (Home excluded, deferred)

All pages except `Home.md` were restyled to the canon in one batch (Shared-Concepts and Fluid were
already done). Post-batch automated sweep is clean: zero em/en dashes, zero full-URL internal links,
zero `### Explicit targeting` blocks, zero mid-page H1s, zero known-bad slugs, zero leftover
note/disclaimer/reminder headings.

**Code-verified corrections made during/after the batch (not just style):**
- `Math-Expressions.md` - `state_prop`/`state_prop_i` were wrongly collapsed as a "duplicate"; they
  are registered in BOTH the block context and the colormap provider (`BlockContextExpression`,
  `ColormapExpressionProvider`), so the Block Expressions scope legitimately has them. RESTORED.
- `Particle-Modifiers.md` - codec (`ParticleModifier.java`) fields are `colormap, color, life, size,
  red, green, blue, alpha, speed`. Table was missing `size` (added); example used `lifetime` →
  corrected to `life`.
- `Item-Modifiers.md` - codec field is `removed_tooltips` (`ItemModifier.java:59`); the example used
  `remove_tooltips` → corrected.
- `Environment-Attributes.md` - agent verified `REMOVED`→`REMOVE` against
  `EnvironmentAttributeMapMod.Removal`, fixed a broken-brace JSON example and an arithmetic slip.
- `Lightmaps.md` - `sorch_lerp_factor`→`torch_lerp_factor` (confirmed by the example below it).
- Home is intentionally NOT yet converted (user deferred it).

## Third parse applied (all non-Home pages)

Fixed: brand casing (`Neoforge`→`NeoForge`, `Optifine`/`optifine`→`OptiFine`; code
`neoforge:`/`'neoforge'` left lowercase); `Json`→`JSON` in prose/headings (format = acronym) while
the `sounds.json` filename went lowercase-in-backticks; Polytone-Configs table headers
`attribute name | explanation`→`Field | Description`; bare parenthetical version lines on
Biome/Dimension (`(1.21.5 or less)` etc.) → `[!NOTE]` boxes; all-caps prose emphasis
(NOT/ONLY/MUST/VERY/SAME/JUST/PARTIALLY) → **bold**; trailing whitespace + squeezed blank lines
across all files.

Deliberately NOT changed: example intro phrasing (owner wants it kept varied/colorful); `Parameter`
field-table headers (fine as-is for particle/emitter params); CIM `## Tips` (a real multi-tip
section, not a redundant box); the `Custom-Item-Models-(CIM)` links with parens (balanced parens are
CommonMark-legal; verify render but likely fine); **Home** (deferred for a full pass - still has
`pack_format: 24`, typos, and cheat-sheet page-name mismatches).

## Second parse: commonalities & uniformization (deep read of all 24 pages)

### Pages fall into 3 archetypes (one template will NOT fit all)

1. **Modifier pages** (file name = target, optional `targets` list): Biome, Block, Dimension,
   Fluid, Item, Particle-Modifiers, Creative-Tab, Variant-Textures, Entity. These share the
   copy-paste targeting boilerplate.
2. **Field-target pages** (target chosen by an in-JSON field, file name irrelevant): Gui-Modifiers
   (`target_type`/`target`), Screen-Sprite (`texture`), Custom-Item-Models (`item`).
3. **Folder / global / key pages** (no per-object target): Colormaps (attach-by-reference),
   Custom-Colors (keys), Custom-Sound-Events (registry), Polytone-Configs, Shaders (path-based),
   Environment-Attributes (host-field), Math/Scripting-Expressions (embedded strings),
   Models-Improvements, Extra-Features.

→ **Action:** the style guide needs ~3 page skeletons, one per archetype, not one.

### The single biggest uniformization win: kill the targeting boilerplate

8 modifier pages hand-copy the same paragraph: *"Alternatively, if you want to manually specify your
targets [X], you can place this json in `assets/[your pack namespace]/polytone/[folder]/[some
name].json` (Any path will work but this is recommended to avoid overwriting Implicit defined
targets)."* + *"Then you can add the `targets` field..."* + the typo line *"Useful if you want yo
modify more than 1 target for the same json."*

This copy-paste is the ROOT CAUSE of a cluster of bugs. Replacing it with a 2-line + link to
`Shared-Concepts#targeting` fixes them all at once:

### [BUG] more copy-paste leaks found (same family as the Entity one)

- [ ] `Dimension-Effects-Modifiers.md` - Getting Started example path says
      `assets/minecraft/polytone/biome_effects/the_nether.json` (should be `dimension_modifiers/`).
- [ ] `Creative-Tab-Modifiers.md` - example path says `.../biome_effects/combat.json` AND targeting
      calls them "a list of valid **Dimension** ids" (should be creative-tab / tab ids).
- [x] `Fluid-Properties-Modifiers.md` - fixed in the reference rewrite (block→fluid, targeting
      boilerplate replaced with the canonical block + link, notes → alert boxes, links relative).
- [ ] `Item-Modifiers.md` - "Say for example the **block** you want to edit is
      `supplementaries:quiver`" (should say item).
- [ ] `Custom-Item-Models-(CIM).md` - `name_pattern` table row listed **twice** (duplicate).
- [ ] `Math-Expressions.md` - `state_prop_i`/`state_prop` rows duplicated verbatim (L131-132 vs
      L174-175).
- [ ] "Useful if you want **yo** modify..." typo on ~5 pages (Block, Fluid, Item, Particle, Variant).

### [STYLE] Placeholder tokens for file paths are all different

Observed: `[your pack namespace]`, `[block name]`, `[fluidname]` (no space), `[biome path]`,
`[dimension path]`, `[particle id name]`, `[tab namespace]`, `[any namespace]`, `[your namespace]`,
`[your pack name]`, `[target dimension namespace]`, `yourmodid`, `[namespace]`.
- [ ] Pick ONE convention (proposal: `[namespace]` + `[name]`, or `[target-namespace]` +
      `[target-path]`) and use it everywhere.

### [STYLE] "Where to put the file" sentence is reworded on every page

"The file will have to be in" / "First you'll need to create a .json file in your resource pack
folder in" / "To start you'll need to create" / "You can start by adding" / "define a .json in".
- [ ] Standardize one canonical sentence.

### [STYLE] "Here's an example" intro phrase varies

"Here is an example of how the `.json` could look" / "Here's how one of this files could look like" /
"Here's a couple of examples" / "Here's an example" / "Let's see an example". Minor; pick one.

### [STYLE] Notes/warnings: 6+ different mechanisms, zero alert boxes

Headings-as-notes: `## Note` (Biome), `### Note`+`### Reminder` (Colormaps), `### Disclaimer`
(Sound-Events L10, Scripting L373, Variant-Textures), malformed `#Disclaimer` (Creative-Tab).
Also: bold `**NOTE**:`, all-caps `NOTE:`/`REMINDER:`, bare "Note that...", and a plain `>` blockquote
(Item, the only one close to an alert box).
- [ ] Convert all to GitHub alert boxes. Map: caveat→`[!NOTE]`, footgun/deprecated→`[!WARNING]`,
      experimental→`[!CAUTION]`, best-practice→`[!TIP]`.

### [STYLE] Version-note conventions: 6 styles, no standard

Inline sentence (Math, Scripting, Lightmaps); bare first line "1.20.4+ only." (Screen-Sprite);
bare bold line `**1.21.11+**` (Extra-Features); bold inline `**1.21.11+ only**` (Configs);
parenthetical in heading `## Expression-Driven Block Models (1.21.1+)` (Models); dedicated
`### 1.21.5 and before` / `### 1.21.11 and above` subheadings (Dimension - the clearest).
- [ ] Adopt a 3-tier convention: whole-feature gate → `[!NOTE]` box under H1; per-field/value →
      inline `(1.21.11+)`; big behavior split → `### <ver> and above/below` subheadings (Dimension
      is the model).

### [STYLE] Field-doc tables: inconsistent columns

Most pages use tables but with different columns: `Field | Function` (2-col, most), `Field | Type |
Description` (Entity), `parameter | type | explanation` (Screen-Sprite). A few use bullets/prose
instead (Biome, Shaders, Models, Extra-Features).
- [ ] Standard column set (proposal: `Field | Type | Default | Description`, drop Default where N/A).

### [STYLE] More mid-page H1 dividers + malformed headings

- [ ] `Creative-Tab-Modifiers.md` `#Disclaimer` (no space → renders as H1) and `# Custom Creative
      Tabs`. Plus Shaders + Math-Expressions already listed above.

### [BUG] More broken / non-links found

- [ ] `Scripting-Expressions.md:337` links config page as `.../wiki/Custom-Configs` - real slug is
      `Polytone-Configs`.
- [ ] `Block-Properties-Modifiers.md` + `Particle-Modifiers.md` link `.../wiki/Scripting` - no such
      page; should be `Scripting-Expressions`.
- [ ] `Custom-Colors.md:61` literal `(link)` placeholder; `Custom-Particle-Types.md:245`
      `[official MC wiki]` with no URL.
- [ ] `Gui-Modifiers.md` "further down" link points at the **old external Slotify wiki**, not this
      page's own section.
- [ ] Many sibling-page references are plain italic text ("See Colormaps Section",
      "See the scripting expressions page") that should be real relative links.

### [CLARITY] Register drifts chatty on some pages

"You know the drill", "bare with me", "Well you made it!", "Advanced one, leave alone". Fine in
moderation but inconsistent. Style guide could set a register (friendly but not filler).

## Proposed increment order

1. **[BUG] batch** - fix the 2 broken links + the Entity copy-paste leak. Small, high value.
2. **Agree the canonical choices** (JSON heading, Getting Started, page-name mismatches, relative
   links) and record them in `wiki-style-guide.md`.
3. **Mechanical sweeps** - one pass each: relative links, JSON heading casing, notes-to-alerts,
   H1 levels. Each is a self-contained, low-risk pass.
4. **Targeting boilerplate** - trim per-page to link into Shared-Concepts (bigger, per-page review).
5. **Clarity/typo pass** - folded into whatever page we're already editing.
