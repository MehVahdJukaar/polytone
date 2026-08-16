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
val fabric_loader_version: String by extra
val fabric_api_version: String by extra

dependencies {
    // Declarative codec schema API - remapped mod dep for dev + bundled (JiJ) into the shipped jar.
    modImplementation("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    include("net.mehvahdjukaar:codecui-fabric:${codecui_version}")

    // The editor UI is a SEPARATE mod - mod dep for dev, NOT bundled (users install it themselves).
    modCompileOnly("net.mehvahdjukaar:nautilus_studio-fabric:${nautilus_studio_version}")

    apiInclude("net.objecthunter:exp4j:${exp4j_version}")
    apiInclude("org.mvel:mvel2:${mvel_version}")




    // No 26.1 build yet
    // modCompileOnly ("curse.maven:fabric-seasons-413523:5789846")


    // modRuntimeOnly("maven.modrinth:sodium:mc1.21-0.6.0-beta.1-fabric")
//modImplementation "curse.maven:continuity-531351:5425853"
    // modImplementation ("curse.maven:continuity-531351:5425853")
    // 0.9.1, not 0.9.2-alpha: Iris 1.11.2+26.2 pins 0.9.1 and its sodium-compat mixins fail to apply on the alphas
    modImplementation("maven.modrinth:sodium:mc26.2-0.9.1-fabric")
    // re-enables vanilla core-shader replacement under Sodium (terrain/block shaders); no 26.2 build yet,
    // newest is still built against Sodium 0.9.0-beta.1 on 26.1.2 - we only need its own API classes to compile
    modCompileOnly("curse.maven:sodium-core-shader-support-956376:8267839") // 1.5.0-mc26.1.2-sodium0.9.0beta.1
     //modImplementation ("curse.maven:distant-horizons-508933:6387715")
    // Compile-only: pulling Iris into the dev runtime changes how the whole render path behaves.
    modImplementation("maven.modrinth:iris:1.11.2+26.2-fabric")
    // No 26.1 build yet
    // modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    // modmenu 4.0.6 is for 1.18.2 - no 26.1 build available
    // modCompileOnly("curse.maven:modmenu-308702:5810603")

    // modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    // modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
