# Polytone Wiki Style Guide

Working notes for the wiki rewrite. The wiki lives in its **own** repo
(`git@github.com:MehVahdJukaar/polytone.wiki.git`, checked out at `wiki/`), so this file stays
here in the main repo, unpublished. Drafts under review are named `MOCKUP-*.md` in `wiki/`.

## Goal

A **gentle learning curve**. The wiki should not be:

- **Overwhelming**: no wall of every option up front.
- **Verbose**: no restating the obvious, no filler ("As you can see...", "It is important to
  note that...").
- **Obvious**: don't explain what JSON brackets are on a feature page; link to the basics once.
- **Shallow**: but don't be so brief that a reader can't actually do the thing.

Aim for: **one idea, one short paragraph, one example.** If a section is growing past ~4 lines of
prose, it probably wants a table, a list, a collapsible, or a split.

## Voice

- Friendly and conversational, not stiff/formal. Keep the original wiki's approachable register.
- **Setup steps**: second person, active. "Create a file named...", not "A file should be created...".
- **Walking through an example**: use the informal "here we..." / "this one..." / "this assigns..."
  register, NOT a clipped imperative. Say "Here we assign the `foliage_color` colormap...", not
  "Assign the `foliage_color` colormap...". Descriptive beats commanding when explaining a snippet.
- Avoid the opposite extreme too: cut filler tics like "You know the drill", "bare with me",
  "Well you made it!". Friendly, not chatty-for-its-own-sake.
- Lead each page with a **one-sentence** statement of what the feature does, in plain terms.
- Reference-heavy material (all the fields, all the enum values) goes in a **table**, not prose.
- **Bold the key terms** in dense prose so the eye catches the structure instead of a gray block
  (e.g. the two or three nouns a paragraph really turns on). Don't over-bold: a few per paragraph
  at most, or it stops meaning anything. This is a cheap, asset-free way to de-wall a page.

## Hard rules

- **No em dashes or en dashes.** Use commas, colons, parentheses, or split the sentence. (`—` `–`
  both banned.)
- **Verify every claim against the code** before writing it. The old hand-written wiki has real
  errors (wrong types, stale fields). Trust `common/src/.../content/<x>/` over the old page.
- **Flag anything unverified** with an inline `<!-- VERIFY: ... -->` comment so a later pass catches
  it. Don't ship a confident sentence you didn't check.
- Field names, ids, enum values, file paths: always in `backticks`.
- Cross-version behavior must say **which version** ("Newer versions (1.21.11+)..."). One MC
  version per branch; the code you read is for the branch you're on.

## Why visual variety matters

A page that is one unbroken wall of same-colored text reads as intimidating and gets skimmed or
abandoned. GitHub color-codes several elements (alert boxes are blue/green/purple/yellow/red,
diagrams are boxed, code is highlighted). Every ~1 screen of prose should be broken by *something*:
an alert box, a table, a diagram, an image, a code block, a collapsible. This is not decoration, it
is what makes docs usable. When editing a page, if you scroll and see a solid block of white text,
that block wants breaking up.

## GitHub wiki markdown: what actually renders

GFM pipeline, sanitized. Inline CSS, `style=`, `<font>`, `<script>` are **stripped**.

**Use these to break up walls of text (roughly in order of how much they stand out):**

| Tool | Syntax | Use for |
|---|---|---|
| Alert boxes | `> [!NOTE]` / `[!TIP]` / `[!IMPORTANT]` / `[!WARNING]` / `[!CAUTION]` | Emphasis, gotchas, deprecations. 5 distinct colors, the main "color" lever. |
| Images / GIFs | `![alt](url)` | **Highest value for a visual mod.** Before/after shots; animated GIFs autoplay on GitHub, perfect for showing an effect in motion. Several pages already embed screenshots; use more. |
| Mermaid diagrams | ```` ```mermaid ```` | Flowcharts (targeting, the debug decision flow), the two-file Custom-Particle flow, the layer/group ordering in Environment Attributes, a "which page do I need?" map. Many diagram types: `flowchart`, `sequenceDiagram`, `stateDiagram`, `mindmap`, `erDiagram`, `pie`. |
| Tables | standard GFM | Field references, enum values, "I want to..." maps. Support column alignment. |
| Syntax-highlighted code | ```` ```json ```` / ```` ```glsl ```` / ```` ```properties ```` | GitHub colorizes by language, big readability win over bare blocks. |
| Collapsibles | `<details><summary>...</summary>` | Advanced detail, long examples, legacy notes. Keeps the default view short. |
| Color swatch | `` `#4a8f3c` `` in backticks | Renders a small color square. Great for a color/colormap mod. (Renders where GitHub supports it; verify on the wiki.) |
| Math | `$...$` inline, `$$...$$` block (LaTeX) | Colormap/expression math, e.g. the sunset sampler. (MathJax; verify it renders on the wiki, it can be flaky there.) |
| Key caps | `<kbd>F3</kbd>` + `<kbd>T</kbd>` | Keypresses. |
| Footnotes | `text[^1]` + `[^1]: note` | Move a tangent out of the main flow without a parenthetical. |
| Task lists | `- [ ]` / `- [x]` | Checklists (setup steps, "did you..." lists). |
| Sub/superscript | `<sub>` / `<sup>` | Rare, math/units. |
| Emoji | `:warning:` `:bulb:` or unicode | Light signposting, sparingly. |

**Does NOT work:** colored/sized/custom-font text, arbitrary HTML/CSS/JS. The only real text "color"
is alert boxes and the hex swatch. If styled output is ever truly needed, embed a committed SVG/PNG
(avoid unless a diagram demands it).

## Alert box conventions

Pick by intent so they stay meaningful:

- `[!NOTE]`: a clarification or "how it actually parses" aside.
- `[!TIP]`: a shortcut or best practice.
- `[!IMPORTANT]`: something the reader will get wrong if they miss it.
- `[!WARNING]`: deprecated fields, footguns, data loss, "this errors if...".
- `[!CAUTION]`: experimental/unstable features.

## Canonical conventions (decided)

Locked choices for the consistency sweeps. When a page violates one, fix it.

- **Internal links are relative**: `[Text](Page-Name)`, hyphens, no `.md`, no full
  `https://github.com/.../wiki/` URL. (14 legacy pages still use full URLs; sweep pending.)
- **One `#` H1 per page**, and its text matches the page slug (e.g. slug `Custom-Particle-Types`
  → H1 `Custom Particle Types`). Everything below is `##` / `###`; never use `#` as a mid-page
  divider.
- **Opening how-to section is `## Getting Started`** (that exact casing).
- **The format/reference section is `## JSON Format`** (that exact casing). Not "Json Structure",
  "Json content", "Json Syntax", etc.
- **Notes/warnings/disclaimers are alert boxes**, never headings. No `## Note` / `### Disclaimer`
  / `### Reminder` in the TOC.
- **Page names**: `Environment-Attributes` (not `-Modifiers`; it's a shared concept, not a
  modifier). `Math-Expressions` = the legacy exp4j system; `Scripting-Expressions` = the current
  MVEL system. Both stay; do **not** rewrite or merge their content, style only.

## Linking

- Wiki-internal: `[Environment Attributes](Environment-Attributes)` (page slug with hyphens,
  no `.md`).
- Section anchor: `[Targeting](Shared-Concepts#targeting)` (lowercase, spaces to hyphens).
- Shared concepts (targeting, conditions, priority, ids, modularity) live on **Shared-Concepts**.
  Feature pages **link** to it instead of re-explaining. Never re-teach targeting on a feature page.

## Preserve information

We are doing **style, not content pruning**. The wiki spans many MC versions and this repo branch
(26.2) only runs one of them. A feature may be removed/absent on the current branch (Lightmaps gone
in 1.21.5, CIM is 1.21.1-only, Math Expressions deprecated, etc.) and its page **still stays** with
its info intact. When restructuring a page, every fact, field, caveat and version note that was
there must survive the edit. If unsure whether something is still true, keep it and tag
`<!-- VERIFY -->` rather than delete.

## Canonical snippets

**File-path sentence** (Getting Started). One wording everywhere:
> The file goes in `assets/[namespace]/polytone/<subfolder>/[path].json`.

Then a concrete example. **Two placeholder tokens, and the distinction matters** (a reviewer
flagged this):
- `[namespace]` / `[path]` = the **target object's** id parts. Use these when the path is derived
  from what you're modifying, i.e. a modifier's implicit path (`biome_modifiers/forest.json` →
  `minecraft:forest`), or a lightmap named after its dimension. `[namespace]` reads as "the thing
  we target".
- `[your namespace]` = **your resource pack's own** namespace. Use this whenever the file lives
  under your pack rather than the target's: all folder/global pages (Colormaps, Custom-Colors,
  Configs, Sounds, Shaders, CIM, Models...), pack-owned sub-resources (custom sound types, custom
  block sets, worn `custom_models`, registered creative tabs), and field-target pages where the
  file name is arbitrary (Gui, Screen-Sprite).
- `[name]` = an arbitrary file name that isn't an id (only on field-target / folder pages).

Never invent per-page tokens like `[block name]`, `[fluidname]`, `[tab namespace]`, `yourmodid`.

**Targeting block** (modifier pages) — replaces the hand-copied paragraph. Keep it short, link out:
> By default the file name is the target: `<subfolder>/redstone_wire.json` targets
> `minecraft:redstone_wire`. To target something else (a list, a `#tag`, or a regex), add a
> `targets` field. See **[Targeting](Shared-Concepts#targeting)** for the full rules.

**Example intro**: `Here is an example:` (one phrasing, drop the variants).

## Version notes — 3 tiers

1. **Whole feature gated** to a version → a `[!NOTE]` box right under the H1:
   `> [!NOTE]\n> This feature is 1.21.11+.` (Also fine as it applies: "removed in 1.21.5".)
2. **A single field / value** differs → inline parenthetical: `depth (1.21.8+) ...`.
3. **A big behavior split** between versions → dedicated subheadings, e.g.
   `### 1.21.5 and below` / `### 1.21.11 and above`. (The Dimension page is the model.)

Never bury a whole-feature version gate as a bare first line or a lone bold `**1.21.11+**`.

## Field tables

Reference a feature's fields in a table, not prose. Columns: `Field | Type | Default | Description`
(drop `Default` only when nothing has one). All fields optional unless the Description says
otherwise.

## Three page skeletons (pick by archetype)

**A. Modifier page** (file name = target: Biome, Block, Dimension, Fluid, Item, Particle-Modifiers,
Creative-Tab, Variant-Textures, Entity):
```
# <Feature> Modifiers
<one sentence: what it does> + short bullet list of what you can change
## Getting Started
<canonical file-path sentence + one concrete example + smallest JSON>
## JSON Format        (or ## Fields)
<field table>
## <feature subsections>
## Targeting
<canonical targeting block → Shared-Concepts#targeting>
```

**B. Field-target page** (target set by an in-JSON field: Gui, Screen-Sprite, Custom-Item-Models):
```
# <Feature>
<one sentence>
## Getting Started
<file path (name doesn't matter, say so) + smallest example>
## JSON Format
<field table — the target field is just one of the fields, document it here>
## <subsections>
```
(No "## Targeting" section; targeting is a field. Still link Shared-Concepts for conditions/priority
if used.)

**C. Folder / global page** (no per-object target: Colormaps, Custom-Colors, Sounds, Configs,
Shaders, Environment-Attributes, Expressions, Models, Extra-Features):
```
# <Feature>
<one sentence>
## Getting Started / Usage
<where the file(s) live + how the thing is referenced/attached>
## <reference sections, tables where there are fields>
```
(These vary the most; keep their existing shape, just apply the naming/notes/version/link canon.)

## Open decisions

- MOCKUP- prefix is the review-staging convention; drop it when a page is approved.
- Placeholder token final form (`[namespace]`/`[path]` vs `[target-namespace]`/`[target-path]`) —
  going with `[namespace]`/`[path]` unless it reads ambiguously on a given page.
