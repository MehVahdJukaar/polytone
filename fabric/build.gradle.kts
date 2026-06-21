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

    include("net.objecthunter:exp4j:${exp4j_version}")
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    include("org.mvel:mvel2:${mvel_version}")
    implementation("org.mvel:mvel2:${mvel_version}")

    // Mirror of common deps (the new setup needs every modCompileOnly/modImplementation in common to also live here)
    modCompileOnly("curse.maven:irisshaders-455508:5726475")
    modCompileOnly("curse.maven:sodium-394468:6240355")

    modCompileOnly("curse.maven:sodium-394468:6211305")
    modCompileOnly("curse.maven:serene-seasons-291874:6182595")
    modCompileOnly("curse.maven:fabric-seasons-413523:5789846")
}
