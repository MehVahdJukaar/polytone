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
val veil_version: String by extra

dependencies {

    include("net.objecthunter:exp4j:${exp4j_version}")
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    include("org.mvel:mvel2:${mvel_version}")
    implementation("org.mvel:mvel2:${mvel_version}")

    modImplementation("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    include("net.mehvahdjukaar:codecui-fabric:${codecui_version}")

    modCompileOnly("curse.maven:irisshaders-455508:5726475")
    modImplementation("maven.modrinth:sodium:mc1.21.1-0.8.12-fabric") // sodium 0.8.x line for 1.21.1

    modImplementation("net.mehvahdjukaar:nautilus_studio-fabric:${nautilus_studio_version}")
    modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    modCompileOnly("curse.maven:fabric-seasons-413523:5789846")
    modCompileOnly("foundry.veil:veil-fabric-1.21.1:${veil_version}") { exclude(group = "maven.modrinth") }
    modRuntimeOnly("foundry.veil:veil-fabric-1.21.1:${veil_version}") { exclude(group = "maven.modrinth") }

    modRuntimeOnly("maven.modrinth:entity-model-features:3.2.4-fabric-1.21")
    modRuntimeOnly("maven.modrinth:entitytexturefeatures:7.1-fabric-1.21")

    modRuntimeOnly("maven.modrinth:moonlight:M2v3yoSl")       // moonlight 1.21.1-3.1.0 (fabric)
    modRuntimeOnly("maven.modrinth:supplementaries:5pbVz5qU") // supplementaries 1.21.1-3.8.0 (fabric)
    modCompileOnly("com.terraformersmc:modmenu:${modmenu_version}")
    modRuntimeOnly("com.terraformersmc:modmenu:${modmenu_version}")
}
