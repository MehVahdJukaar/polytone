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
val packed_packs_neoforge_version: String by extra
val packed_packs_api_version: String by extra


dependencies {
    compileOnly ("net.mehvahdjukaar:codecui-common:${codecui_version}")
    compileOnly ("net.mehvahdjukaar:nautilus_studio-common:${nautilus_studio_version}")

    implementation ("net.objecthunter:exp4j:${exp4j_version}")
    implementation ("org.mvel:mvel2:${mvel_version}")

    modCompileOnly("maven.modrinth:iris:1.11.2+26.1-neoforge")
    modCompileOnly("maven.modrinth:packed-packs:${packed_packs_neoforge_version}")
    compileOnly("io.github.fishstiz.packed_packs.api:packed_packs_api-neoforge:${packed_packs_api_version}")
    compileOnly(files(layout.buildDirectory.file("sodium/sodium-neoforge-mod.jar")).builtBy(tasks.named("extractSodiumNeoforge")))

    // modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    // modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
