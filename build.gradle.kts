// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
    id("androidx.navigation.safeargs") version "2.7.6" apply false
}

buildscript {
    val isNormalBuild by extra {
        gradle.startParameter.taskNames.none { task ->
            task.contains("fdroid", ignoreCase = true)
        }
    }
    dependencies {
        if (isNormalBuild) {
            classpath("com.google.gms:google-services:4.4.0")
            classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
        }
    }
}