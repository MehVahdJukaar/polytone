plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val candlelight_version: String by extra
val exp4j_version: String by extra
val mvel_version: String by extra
val codecui_version: String by extra
val nautilus_studio_version: String by extra


dependencies {
    // Declarative codec schema API - compile against the common (named-mappings) artifact.
    compileOnly ("net.mehvahdjukaar:codecui-common:${codecui_version}")
    // The pack editor UI is a SEPARATE mod (not bundled) - compile against it; the in-game
    // "open editor" button grays out at runtime when it isn't installed.
    compileOnly ("net.mehvahdjukaar:nautilus_studio-common:${nautilus_studio_version}")

    implementation ("net.objecthunter:exp4j:${exp4j_version}")
    implementation ("org.mvel:mvel2:${mvel_version}")

    modCompileOnly ("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("curse.maven:irisshaders-455508:6369436")
    // Sodium: compile against the NeoForge distribution's internal (mojmap-mapped) jar dropped in mods/,
    // so common's Sodium shadow code resolves Minecraft types (ChunkSectionLayerGroup / GpuSampler /
    // Camera) by their mojmap names. Plain compileOnly (NOT modCompileOnly): the jar is already mojmap,
    // so it must not be run through loom remapping. The curse.maven Sodium (intermediary MC refs) is gone
    // on purpose - having both on the classpath makes the same class read under two mappings.
    compileOnly(files("mods/net.caffeinemc.sodium-neoforge-0.8.13+mc1.21.11-mod.jar"))

    modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
