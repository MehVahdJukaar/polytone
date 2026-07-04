
plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val exp4j_version: String by extra
val mvel_version: String by extra

val localRuntime by configurations.dependencyScope("localRuntime")
configurations.runtimeClasspath.configure { extendsFrom(localRuntime) }

dependencies {


    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")

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

    // Mirror of common deps (the new setup needs every modCompileOnly/modImplementation in common to also live here)
    modCompileOnly("curse.maven:irisshaders-455508:5726475")
    modCompileOnly("curse.maven:sodium-394468:6240355")

    modCompileOnly("curse.maven:curios-continuation-1037991:5546342")
    modCompileOnly("curse.maven:embeddium-908741:6118392")
    modRuntimeOnly("curse.maven:the-twilight-forest-227639:7797302")
    modCompileOnly("curse.maven:alexs-caves-924854:4806837")
    modCompileOnly("curse.maven:citadel-331936:4786380")
    modCompileOnly("curse.maven:farmers-delight-398521:5772720")
    modCompileOnly("curse.maven:serene-seasons-291874:6182596")
}
