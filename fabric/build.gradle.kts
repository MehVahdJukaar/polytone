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
val modmenu_version: String by extra

dependencies {

    include("net.objecthunter:exp4j:${exp4j_version}")
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    include("org.mvel:mvel2:${mvel_version}")
    implementation("org.mvel:mvel2:${mvel_version}")

    // Declarative codec->schema engine — bundled (JiJ) into polytone
    modImplementation("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    include("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    // Nautilus Studio pack editor UI — separate mod, NOT bundled

    // Mirror of common deps (the new setup needs every modCompileOnly/modImplementation in common to also live here)
    modCompileOnly("curse.maven:irisshaders-455508:5726475")
    modCompileOnly("maven.modrinth:sodium:mc1.21.1-0.8.12-fabric") // sodium 0.8.x line for 1.21.1

    modImplementation("net.mehvahdjukaar:nautilus_studio-fabric:${nautilus_studio_version}")
    modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    modCompileOnly("curse.maven:fabric-seasons-413523:5789846")

    // Runtime-only: Entity Model Features (EMF) + Entity Texture Features (ETF), for testing OptiFine-style CEM/CET
    modRuntimeOnly("maven.modrinth:entity-model-features:3.2.4-fabric-1.21")
    modRuntimeOnly("maven.modrinth:entitytexturefeatures:7.1-fabric-1.21")

    // Runtime test deps from Modrinth (per-loader version IDs; version number in comment)
    modRuntimeOnly("maven.modrinth:moonlight:M2v3yoSl")       // moonlight 1.21.1-3.1.0 (fabric)
    modRuntimeOnly("maven.modrinth:supplementaries:5pbVz5qU") // supplementaries 1.21.1-3.8.0 (fabric)
    // Mod Menu — fabric only
    modRuntimeOnly("com.terraformersmc:modmenu:${modmenu_version}")
}
