plugins {
    id("com.possible-triangle.common")
}

neoForge {
    parchment {
        mappingsVersion = "2024.11.17"
        minecraftVersion = "1.21.1"
    }
}

common {
    accessWidener()
}

val candlelight_version: String by extra
val exp4j_version: String by extra
val mvel_version: String by extra


dependencies {
    implementation ("net.objecthunter:exp4j:${exp4j_version}")
    implementation ("org.mvel:mvel2:${mvel_version}")

    modCompileOnly ("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("curse.maven:irisshaders-455508:6369436")
    modCompileOnly("curse.maven:sodium-394468:7366772")
    modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
    modImplementation ("curse.maven:packed-packs-1260514:7313138")

    implementation ("org.ow2.asm:asm:9.5")
    implementation ("org.ow2.asm:asm-commons:9.5")
}
