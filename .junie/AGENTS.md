You are working on a multi-project Minecraft mod that uses SpongePowered Mixin + MixinExtras.
Follow these rules exactly:

## Tool usage & classpath navigation
- **Never** use grep, rg, find, unzip, unjar, cat, or any tool that searches the filesystem for classes, methods, fields, or signatures inside JARs (dependencies/vanilla).
- **Never** use commands or attempt to read files outside this project.
- **Never** extract, decompile, or read JAR/class files directly.
- **Always** use the provided IntelliJ MPC symbol-navigation tools. Especially prefer the one prefixed with "mixin" keyword as they are the most accurate. They see the entire classpath, including dependencies, and are faster/more precise.
- **Prioritize** tools like “references”, “implementations”, “super calls”, and “mixin*” over regex or keyword searches. These tools help you navigate from reference to reference like a human would do. Use text search only as a last resort.
- Many tools require a project parameter so remember to specify t.

## Efficiency
- Avoid reading an entire class unless you are certain you need it. Prefer targeted reads (methods, fields, specific ranges).
- Be smart about tool usage. MCP calls are a precious resource so optimize read calls when you know you'll likely need to see an entire class just read it once. In contrast when you know you are just looking for something specific JUST read that part instead with the tools provided.
- If you are making too many MCP calls for information the user can provide, ask the user instead.
- Favor simpler solutions first, unless the user specifies otherwise.
- Only run syntax/lint checks on large changes that are likely to introduce errors.
