plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val candlelight_version: String by extra
val exp4j_version: String by extra
val mvel_version: String by extra
val codecui_version: String by extra
val pack_editor_version: String by extra


dependencies {
    // Declarative codec schema API — compile against the common (named-mappings) artifact.
    compileOnly ("net.mehvahdjukaar:codecui-common:${codecui_version}")
    // The pack editor UI is a SEPARATE mod (not bundled) — compile against it; the in-game
    // "open editor" button grays out at runtime when it isn't installed.
    compileOnly ("net.mehvahdjukaar:pack_editor-common:${pack_editor_version}")

    implementation ("net.objecthunter:exp4j:${exp4j_version}")
    implementation ("org.mvel:mvel2:${mvel_version}")

    modCompileOnly ("curse.maven:serene-seasons-291874:6182596")
    modCompileOnly("curse.maven:irisshaders-455508:6369436")
    modCompileOnly("curse.maven:sodium-394468:7366772")
    modCompileOnly("curse.maven:entity-model-features-844662:7400754")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:7392425")
}
