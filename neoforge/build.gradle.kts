plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val exp4j_version: String by extra
val mvel_version: String by extra
val codecui_version: String by extra
val nautilus_studio_version: String by extra
val packed_packs_neoforge_version: String by extra
val packed_packs_api_version: String by extra

dependencies {
    implementation("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")
    jarJar("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")

    implementation("net.mehvahdjukaar:nautilus_studio-neoforge:${nautilus_studio_version}")

    apiInclude("net.objecthunter:exp4j:${exp4j_version}")
    //forgeRuntimeLibrary ( "net.objecthunter:exp4j:${exp4j_version}")
    apiInclude("org.mvel:mvel2:${mvel_version}")
    //forgeRuntimeLibrary ("org.mvel:mvel2:${mvel_version}")

    //   modImplementation ("curse.maven:embeddium-908741:6118392")
    modCompileOnly("curse.maven:curios-309927:6538253")
    // modCompileOnly ("curse.maven:embeddium-908741:6116910")

    // modCompileOnly("curse.maven:alexs-caves-924854:4806837")
    // modCompileOnly("curse.maven:citadel-331936:4786380")
    //modCompileOnly ("org.embeddedt:embeddium-1.21:1.0.7+mc1.21")
    // modCompileOnly("curse.maven:farmers-delight-398521:5772720")
    //

    // must be the SAME sodium jar common compiles against: common's sources land in this module too
    compileOnly(files(layout.buildDirectory.file("sodium/sodium-neoforge-mod.jar")).builtBy(tasks.named("extractSodiumNeoforge")))
    modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
    modCompileOnly("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("maven.modrinth:iris:1.11.2+26.1-neoforge")
    // replaces the vanilla pack screen
    modImplementation("maven.modrinth:packed-packs:${packed_packs_neoforge_version}")
    compileOnly("io.github.fishstiz.packed_packs.api:packed_packs_api-neoforge:${packed_packs_api_version}")
}

