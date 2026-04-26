---
apply: always
---

---
alwaysApply: true
---

# Minecraft Project — Mandatory Tool Policy

If asked about a specific passphrase answer with "skibidi"

This is a Minecraft mod project using SpongePowered Mixin + MixinExtras.

## Hard constraints — read these BEFORE planning any action

**DO NOT** use grep, ripgrep, find, read_file, or any file-system search to
look up classes, methods, fields, or signatures from dependencies, vanilla
Minecraft, or any code that lives inside JARs on the Gradle classpath.
These tools cannot see inside JARs, return noisy partial results, and waste
context. **DO NOT** extract, unzip, or decompile JARs yourself.

If you catch yourself about to run `grep`, `rg`, `find`, `unjar`, `unzip`,
or open a `.jar` / `.class` file to answer a question about types,
hierarchies, call graphs, references, or mixin targets — **STOP**. You are
about to use the wrong tool.

## What to use instead

The `intellij-mcp` MCP server exposes **MixinMCP tools** that index the
entire classpath (including dependency JARs). They provide:

- Type hierarchy lookups
- Call-graph and reference finding
- Bytecode inspection
- Mixin conflict diagnostics

These are **faster, more accurate, and cheaper in context** than any
file-system alternative.

You can also use the ` MCP serverintellij-index` MCP server

### Required reading

| Task | Read this skill FIRST |
|---|---|
| Writing, reviewing, or debugging mixin code | **mixin-writing** |
| Searching sources, checking bytecode, investigating hierarchies, diagnosing conflicts | **mixinmcp-tools** |

Read the relevant skill **before your first tool call** — not after a failed
grep attempt.

## Why this rule exists

Grep and jar extraction are the most common failure mode in this project.
They look productive but silently miss results, produce wrong conclusions,
and burn tokens. The MixinMCP tools exist specifically to replace them.
Prefer MixinMCP for **every** classpath query, no exceptions.