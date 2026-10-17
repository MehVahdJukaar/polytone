
plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val exp4j_version: String by extra
val nexp_version: String by extra
val codecui_version: String by extra
val nautilus_studio_version: String by extra
val veil_version: String by extra

val localRuntime by configurations.dependencyScope("localRuntime")
configurations.runtimeClasspath.configure { extendsFrom(localRuntime) }

dependencies {


    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")

    implementation("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")
    jarJar("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")

    apiInclude("net.objecthunter:exp4j:${exp4j_version}")
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    localRuntime("net.objecthunter:exp4j:${exp4j_version}")
    serverAdditionalRuntimeClasspath("net.objecthunter:exp4j:${exp4j_version}")
    clientAdditionalRuntimeClasspath("net.objecthunter:exp4j:${exp4j_version}")
    apiInclude("hollowpoint:nexp:${nexp_version}")
    implementation("hollowpoint:nexp:${nexp_version}")
    localRuntime("hollowpoint:nexp:${nexp_version}")
    serverAdditionalRuntimeClasspath("hollowpoint:nexp:${nexp_version}")
    clientAdditionalRuntimeClasspath("hollowpoint:nexp:${nexp_version}")

    modCompileOnly("net.mehvahdjukaar:nautilus_studio-neoforge:${nautilus_studio_version}")
    modRuntimeOnly("net.mehvahdjukaar:nautilus_studio-neoforge:${nautilus_studio_version}")
    modCompileOnly("maven.modrinth:iris:1.8.14-beta.1+1.21.1-neoforge")
    modCompileOnly(":sodium-neoforge-mod:0.8.13")
    localRuntime(":sodium-neoforge-mod:0.8.12")
    localRuntime(":fabric-api-base:0.4.42")
    localRuntime(":fabric-block-view-api-v2:1.0.10")
    localRuntime(":fabric-renderer-api-v1:3.4.1")
    localRuntime(":fabric-rendering-data-attachment-v1:0.3.48")

    modCompileOnly("curse.maven:curios-continuation-1037991:5546342")
    modCompileOnly("curse.maven:embeddium-908741:6118392")
    modRuntimeOnly("curse.maven:the-twilight-forest-227639:7797302")
    modCompileOnly("curse.maven:alexs-caves-924854:4806837")
    modCompileOnly("curse.maven:citadel-331936:4786380")
    modCompileOnly("curse.maven:farmers-delight-398521:5772720")
    modCompileOnly("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("foundry.veil:veil-neoforge-1.21.1:${veil_version}") { exclude(group = "maven.modrinth") }
    modRuntimeOnly("foundry.veil:veil-neoforge-1.21.1:${veil_version}") { exclude(group = "maven.modrinth") }

    modRuntimeOnly("net.mehvahdjukaar:moonlight-neoforge:1.21.1-3.7.0") { isTransitive = false }
    modRuntimeOnly("net.mehvahdjukaar:supplementaries-neoforge:1.21.1-4.0.2") { isTransitive = false }
    modRuntimeOnly("net.mehvahdjukaar:hauntedharvest-neoforge:1.21-3.5.0") { isTransitive = false }
    modRuntimeOnly("net.mehvahdjukaar:snowyspirit-neoforge:1.21.1-3.1.15") { isTransitive = false }
}
