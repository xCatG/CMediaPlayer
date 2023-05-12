buildscript {
    val ext = {
        val compose_version = "1.4.3"
        val compose_material_version = "1.4.3"
        val compose_compiler_version = "1.4.4"
        val lifecycle_version = "2.6.1"
        val nav_version = "2.5.3"
        val media3_version = "1.0.1"
        val okhttp3 = "4.10.0"
    }
}

plugins {
    id("com.android.application") version "8.0.1" apply false
    id("com.android.library") version "8.0.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

tasks.register("clean") {
    delete(rootProject.buildDir)
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
