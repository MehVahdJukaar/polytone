plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val exp4j_version: String by extra
val mvel_version: String by extra
val codecui_version: String by extra
val nautilus_studio_version: String by extra

dependencies {
    // Declarative codec schema API — remapped mod dep for dev + bundled (JiJ) into the shipped jar.
    modImplementation("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    include("net.mehvahdjukaar:codecui-fabric:${codecui_version}")

    // The editor UI is a SEPARATE mod — mod dep for dev, NOT bundled (users install it themselves).
    modImplementation("net.mehvahdjukaar:nautilus_studio-fabric:${nautilus_studio_version}")

    apiInclude("net.objecthunter:exp4j:${exp4j_version}")
    apiInclude("org.mvel:mvel2:${mvel_version}")

    modCompileOnly ("curse.maven:fabric-seasons-413523:5789846")


    // modRuntimeOnly("maven.modrinth:sodium:mc1.21-0.6.0-beta.1-fabric")
    //modImplementation "curse.maven:continuity-531351:5425853"
    modCompileOnly("curse.maven:sodium-394468:8111043") // 0.8.12+mc1.21.11
    // re-enables vanilla core-shader replacement under Sodium (terrain/block shaders); version-locked to Sodium above
    modCompileOnly("curse.maven:sodium-core-shader-support-956376:8120366") // 1.5.0-sodium0.8.12-fabric-mc1.21.11
    //modRuntimeOnly("curse.maven:irisshaders-455508:7805348")
    // modImplementation "curse.maven:distant-horizons-508933:6387715"
    modCompileOnly("curse.maven:irisshaders-455508:6369436")
    modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    modImplementation("com.terraformersmc:modmenu:17.0.0")
    modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
