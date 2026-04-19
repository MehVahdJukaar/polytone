plugins {
    id("com.possible-triangle.neoforge")
    id("net.mehvahdjukaar.candlelight")

}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

neoForge {
    parchment {
        mappingsVersion = "2024.11.17"
        minecraftVersion = "1.21.1"
    }
}


val exp4j_version: String by extra
val mvel_version: String by extra

dependencies {


    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")

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

    modCompileOnly("curse.maven:sodium-394468:7366772")
    modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
    modCompileOnly("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("curse.maven:irisshaders-455508:6369436")
    modCompileOnly("curse.maven:sodium-394468:7366772")
}

