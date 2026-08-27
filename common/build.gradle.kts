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


val sodiumJar = file("mods/net.caffeinemc.sodium-neoforge-0.9.1+mc26.1.2-mod.jar")
require(sodiumJar.exists()) {
    "Missing ${sodiumJar.name}. Download the NeoForge build of sodium 0.9.1+mc26.1.2 and extract " +
            "META-INF/jarjar/${sodiumJar.name} into common/mods"
}

dependencies {
    compileOnly ("net.mehvahdjukaar:codecui-common:${codecui_version}")
    compileOnly ("net.mehvahdjukaar:nautilus_studio-common:${nautilus_studio_version}")

    implementation ("net.objecthunter:exp4j:${exp4j_version}")
    implementation ("org.mvel:mvel2:${mvel_version}")

    modCompileOnly("maven.modrinth:iris:1.11.2+26.1-neoforge")
    compileOnly(files(sodiumJar))

    // modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    // modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
