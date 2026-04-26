
This is a multi project Minecraft mod project using SpongePowered Mixin + MixinExtras.

## Hard constraints — read these BEFORE planning any action

**DO NOT** use grep, ripgrep, find, read_file, or any file-system search to
look up classes, methods, fields, or signatures from dependencies, vanilla
Minecraft, or any code that lives inside JARs on the Gradle classpath.
These tools cannot see inside JARs, return noisy partial results, and waste
context. **DO NOT** extract, unzip, or decompile JARs yourself.

If you catch yourself about to run `grep`, `rg`, `find`, `unjar`, `unzip`,  `cat`,
or open a `.jar` / `.class` file to answer a question about types,
hierarchies, call graphs, references, or mixin targets — **STOP**. You are
about to use the wrong tool.
Always prefer navigating by symbols instead such as references, implementations, super calls.

The intellij MPC servers you have access to will give you tools to reference symbols, navigate hierarchy, across the entire classpath including dependencies.
These are **faster, more accurate, and cheaper in context** than any
file-system alternative. Prefer using the mixin ones when possible.

If you have to search for things, only use regex or keyword search tools if all else fails as these are slower and less accurate.
Use more specialized tools instead like search symbols or method.

Many tools require you to specify the project, so remember to pass that along. 

Notice: tool usage is a precious resource, so be smart about it. For instance, avoid reading too much of a file, for unless you think you'll likely need to read more of it later, making a single big read more efficent.

Avoid reading entire classes unless you are sure you need them. Prefer more targeted reads instead.

If you feel like you are doing too many MCP calls for some info the user might just give you, ask the user instead.

When tasked with something, try simpler approach solutions first unless asked.

Only run syntax/lint checks on big tasks that have a high likelihood of needing them.