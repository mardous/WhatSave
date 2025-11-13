import java.util.Properties

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.safeargs)
}

android {
    compileSdk = 36
    namespace = "com.simplified.wsstatussaver"

    defaultConfig {
        minSdk = 24
        targetSdk = 35

        applicationId = namespace
        versionCode = 2101
        versionName = "2.1.1"
    }

    val signingProperties = getProperties("keystore.properties")
    val releaseSigning = if (signingProperties != null) {
        signingConfigs.create("release") {
            keyAlias = signingProperties.property("keyAlias")
            keyPassword = signingProperties.property("keyPassword")
            storePassword = signingProperties.property("storePassword")
            storeFile = file(signingProperties.property("storeFile"))
        }
    } else {
        signingConfigs.getByName("debug")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
        }
        debug {
            versionNameSuffix = " DEBUG"
            applicationIdSuffix = ".debug"
            signingConfig = releaseSigning
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("normal") {
            dimension = "version"
        }
        create("fdroid") {
            dimension = "version"
        }
    }
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "WhatSave-v${defaultConfig.versionName}-${name}.apk"
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    packaging {
        resources {
            excludes += listOf("META-INF/LICENSE", "META-INF/NOTICE", "META-INF/java.properties")
        }
    }
    lint {
        abortOnError = true
        warning += listOf("ImpliedQuantity", "Instantiatable", "MissingQuantity", "MissingTranslation")
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
    }
    jvmToolchain(21)
}

fun getProperties(fileName: String): Properties? {
    val file = rootProject.file(fileName)
    return if (file.exists()) {
        Properties().also { properties ->
            file.inputStream().use { properties.load(it) }
        }
    } else null
}

fun Properties.property(key: String) =
    this.getProperty(key) ?: "$key missing"

dependencies {
    // Google/JetPack
    //https://developer.android.com/jetpack/androidx/versions
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.preference)
    implementation(libs.material.components)

    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(libs.bundles.kotlinx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.navigation)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.markwon)

    implementation(libs.photoview)
    implementation(libs.versioncompare)
    implementation(libs.libphonenumber)
    implementation(libs.prettytime)
    implementation(libs.advrecyclerview)
}