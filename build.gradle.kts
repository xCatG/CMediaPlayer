buildscript {
    val composeBomVersion by extra {"2023.08.00"}
    val ext = {
        val okhttp3 = "4.10.0"
    }
}

plugins {
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
}

tasks.register("clean") {
    delete(rootProject.buildDir)
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
