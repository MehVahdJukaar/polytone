pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven { url = uri("https://maven.muon.rip/releases") }
    }
}

plugins {
    id("com.possible-triangle.helper") version ("1.4")
}

include("common", "fabric", "neoforge")