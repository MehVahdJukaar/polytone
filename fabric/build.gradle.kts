plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val exp4j_version: String by extra
val nexp_version: String by extra
val codecui_version: String by extra
val nautilus_studio_version: String by extra
val fabric_loader_version: String by extra
val fabric_api_version: String by extra
val packed_packs_fabric_version: String by extra
val packed_packs_api_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    include("net.mehvahdjukaar:codecui-fabric:${codecui_version}")

    modCompileOnly("net.mehvahdjukaar:nautilus_studio-fabric:${nautilus_studio_version}")

    apiInclude("net.objecthunter:exp4j:${exp4j_version}")
    apiInclude("hollowpoint:nexp:${nexp_version}")




    // modCompileOnly ("curse.maven:fabric-seasons-413523:5789846")


    // modRuntimeOnly("maven.modrinth:sodium:mc1.21-0.6.0-beta.1-fabric")
//modImplementation "curse.maven:continuity-531351:5425853"
    // modImplementation ("curse.maven:continuity-531351:5425853")
    modCompileOnly("maven.modrinth:sodium:mc26.3-0.9.2-fabric")
    modCompileOnly("curse.maven:sodium-core-shader-support-956376:8267839") // 1.5.0-mc26.1.2-sodium0.9.0beta.1
     //modImplementation ("curse.maven:distant-horizons-508933:6387715")
    modCompileOnly("maven.modrinth:iris:1.11.7+26.3-fabric")
    modCompileOnly("maven.modrinth:distanthorizons:3.3.3-26.2")
    modCompileOnly("maven.modrinth:packed-packs:${packed_packs_fabric_version}")
    compileOnly("io.github.fishstiz.packed_packs.api:packed_packs_api-fabric:${packed_packs_api_version}")
    // modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    // modCompileOnly("curse.maven:modmenu-308702:5810603")

    // modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    // modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
