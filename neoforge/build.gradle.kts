
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
val veil_version: String by extra

val localRuntime by configurations.dependencyScope("localRuntime")
configurations.runtimeClasspath.configure { extendsFrom(localRuntime) }

dependencies {


    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")

    // Declarative codec->schema engine, bundled (JiJ) into polytone
    implementation("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")
    jarJar("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")

    apiInclude("net.objecthunter:exp4j:${exp4j_version}")
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    localRuntime("net.objecthunter:exp4j:${exp4j_version}")
    serverAdditionalRuntimeClasspath("net.objecthunter:exp4j:${exp4j_version}")
    clientAdditionalRuntimeClasspath("net.objecthunter:exp4j:${exp4j_version}")
    apiInclude("org.mvel:mvel2:${mvel_version}")
    implementation("org.mvel:mvel2:${mvel_version}")
    localRuntime("org.mvel:mvel2:${mvel_version}")
    serverAdditionalRuntimeClasspath("org.mvel:mvel2:${mvel_version}")
    clientAdditionalRuntimeClasspath("org.mvel:mvel2:${mvel_version}")

    modCompileOnly("net.mehvahdjukaar:nautilus_studio-neoforge:${nautilus_studio_version}")
    modRuntimeOnly("net.mehvahdjukaar:nautilus_studio-neoforge:${nautilus_studio_version}")
    modCompileOnly("curse.maven:irisshaders-455508:5726475")
    modCompileOnly(":sodium-neoforge-mod:0.8.12")
    localRuntime(":sodium-neoforge-mod:0.8.12")
    localRuntime(":fabric-api-base:0.4.42")
    localRuntime(":fabric-block-view-api-v2:1.0.10")
    localRuntime(":fabric-renderer-api-v1:3.4.1")
    localRuntime(":fabric-rendering-data-attachment-v1:0.3.48")
    clientAdditionalRuntimeClasspath(":sodium-neoforge-mod:0.8.12")
    clientAdditionalRuntimeClasspath(":fabric-api-base:0.4.42")
    clientAdditionalRuntimeClasspath(":fabric-block-view-api-v2:1.0.10")
    clientAdditionalRuntimeClasspath(":fabric-renderer-api-v1:3.4.1")
    clientAdditionalRuntimeClasspath(":fabric-rendering-data-attachment-v1:0.3.48")

    modCompileOnly("curse.maven:curios-continuation-1037991:5546342")
    modCompileOnly("curse.maven:embeddium-908741:6118392")
    modRuntimeOnly("curse.maven:the-twilight-forest-227639:7797302")
    modCompileOnly("curse.maven:alexs-caves-924854:4806837")
    modCompileOnly("curse.maven:citadel-331936:4786380")
    modCompileOnly("curse.maven:farmers-delight-398521:5772720")
    modCompileOnly("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("foundry.veil:veil-neoforge-1.21.1:${veil_version}") { exclude(group = "maven.modrinth") }

    modRuntimeOnly("maven.modrinth:moonlight:9003gPaE")       // moonlight 1.21.1-3.1.0 (neoforge)
    modRuntimeOnly("maven.modrinth:supplementaries:HAFNpQz4") // supplementaries 1.21.1-3.8.0 (neoforge)
}
