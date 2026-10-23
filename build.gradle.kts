// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 ships a built-in Kotlin compiler (2.2.x). miuix 0.9.4 is compiled with
        // Kotlin 2.4 metadata, so pin the Kotlin Gradle plugin to a matching version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
