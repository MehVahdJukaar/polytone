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
val modmenu_version: String by extra
val veil_version: String by extra

dependencies {

    include("net.objecthunter:exp4j:${exp4j_version}")
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    include("hollowpoint:nexp:${nexp_version}")
    implementation("hollowpoint:nexp:${nexp_version}")

    modImplementation("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    include("net.mehvahdjukaar:codecui-fabric:${codecui_version}")

    modCompileOnly("maven.modrinth:iris:1.8.14-beta.1+1.21.1-fabric")
    modLocalRuntime("maven.modrinth:iris:1.8.14-beta.1+1.21.1-fabric")
    modCompileOnly("maven.modrinth:sodium:mc1.21.1-0.8.13-fabric")
    modLocalRuntime("maven.modrinth:sodium:mc1.21.1-0.8.13-fabric")
    //iris nested libs, loom doesnt put them on the dev classpath
    localRuntime("org.anarres:jcpp:1.4.14")
    localRuntime("io.github.douira:glsl-transformer:3.0.0-pre3")
    localRuntime("org.antlr:antlr4-runtime:4.13.1")

    modImplementation("net.mehvahdjukaar:nautilus_studio-fabric:${nautilus_studio_version}")
    modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    modCompileOnly("curse.maven:fabric-seasons-413523:5789846")
    modCompileOnly("foundry.veil:veil-fabric-1.21.1:${veil_version}") { exclude(group = "maven.modrinth") }
    modRuntimeOnly("foundry.veil:veil-fabric-1.21.1:${veil_version}") { exclude(group = "maven.modrinth") }

   // modRuntimeOnly("maven.modrinth:entity-model-features:3.2.4-fabric-1.21")
   // modRuntimeOnly("maven.modrinth:entitytexturefeatures:7.1-fabric-1.21")

   // modRuntimeOnly("maven.modrinth:moonlight:M2v3yoSl")       // moonlight 1.21.1-3.1.0 (fabric)
   // modRuntimeOnly("maven.modrinth:supplementaries:5pbVz5qU") // supplementaries 1.21.1-3.8.0 (fabric)
    modRuntimeOnly("maven.modrinth:moonlight:przKUDWo")  // moonlight 1.21.1-3.7.1 (fabric)
    modRuntimeOnly("maven.modrinth:amendments:TuWUel70") // amendments 1.21-2.1.10 (fabric)
    modCompileOnly("com.terraformersmc:modmenu:${modmenu_version}")
    modRuntimeOnly("com.terraformersmc:modmenu:${modmenu_version}")
}
