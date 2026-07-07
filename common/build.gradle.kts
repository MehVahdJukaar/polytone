plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val exp4j_version: String by extra
val mvel_version: String by extra

dependencies {
    implementation("net.objecthunter:exp4j:${exp4j_version}")
    implementation("org.mvel:mvel2:${mvel_version}")

    modCompileOnly("curse.maven:irisshaders-455508:5726475")
    // sodium 0.8.12 neoforge: the distributed jar just JiJs the actual mod jar, extracted into mods/ (flatDir)
    // compileOnly only: common is never launched, and a flatDir dep (empty group) would otherwise be published
    // into the module metadata as "group": null and crash modifyMetadataFile (JsonNull).
    modCompileOnly(":sodium-neoforge-mod:0.8.12")
    modCompileOnly("curse.maven:serene-seasons-291874:6182596")
}
