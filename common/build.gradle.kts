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
    compileOnly ("net.mehvahdjukaar:codecui-common:${codecui_version}")
    compileOnly ("net.mehvahdjukaar:nautilus_studio-common:${nautilus_studio_version}")

    implementation ("net.objecthunter:exp4j:${exp4j_version}")
    implementation ("org.mvel:mvel2:${mvel_version}")

    modCompileOnly("maven.modrinth:iris:1.11.2+26.1-neoforge")
    compileOnly(files("mods/net.caffeinemc.sodium-neoforge-0.9.2-alpha.3+mc26.2-mod.jar"))

    // modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    // modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
