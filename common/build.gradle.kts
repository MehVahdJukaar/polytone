plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val exp4j_version: String by extra
val nexp_version: String by extra
val codecui_version: String by extra
val nautilus_studio_version: String by extra
val veil_version: String by extra

dependencies {
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    implementation("hollowpoint:nexp:${nexp_version}")

    compileOnly("net.mehvahdjukaar:codecui-common:${codecui_version}")
    compileOnly("net.mehvahdjukaar:nautilus_studio-common:${nautilus_studio_version}")

    modCompileOnly("curse.maven:irisshaders-455508:5726475")
    modCompileOnly(":sodium-neoforge-mod:0.8.12")
    modCompileOnly("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("foundry.veil:veil-common-1.21.1:${veil_version}")
}
