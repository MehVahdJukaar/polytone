pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }

    /*
    // This is how you could still have it in gradle.properties instead of a version catalog
    // obviously a lot more verbose, but if you really want to have it there I don't think there is another way
    val candlelight_version: String by settings
    plugins {
        id("net.mehvahdjukaar.candlelight") version candlelight_version
    }*/
}

plugins {
    id("com.possible-triangle.helper") version ("1.3")
}

include("common", "fabric", "neoforge")