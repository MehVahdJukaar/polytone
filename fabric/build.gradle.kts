plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))

    accessWidener(project(":common"))
}

val exp4j_version: String by extra
val mvel_version: String by extra

dependencies {

    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")

    apiInclude("net.objecthunter:exp4j:${exp4j_version}")
    apiInclude("org.mvel:mvel2:${mvel_version}")


    modCompileOnly ("curse.maven:fabric-seasons-413523:5789846")


    // modRuntimeOnly("maven.modrinth:sodium:mc1.21-0.6.0-beta.1-fabric")
    //modImplementation "curse.maven:continuity-531351:5425853"
    modCompileOnly("curse.maven:sodium-394468:7366772")
    //modRuntimeOnly("curse.maven:irisshaders-455508:7805348")
    // modImplementation "curse.maven:distant-horizons-508933:6387715"
    modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    modCompileOnly("com.terraformersmc:modmenu:4.0.6")

}
