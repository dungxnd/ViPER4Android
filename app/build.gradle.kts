import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProps =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use(::load)
    }

android {
    namespace = "com.dxnd.viper4android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dxnd.viper4android"
        minSdk = 28
        targetSdk = 37
        versionCode = 20260820
        versionName = "2.3.8"
    }

    androidResources {
        generateLocaleConfig = true
    }

    val keystoreFile = localProps.getProperty("KEYSTORE_FILE", "")
    signingConfigs {
        if (keystoreFile.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = localProps.getProperty("KEYSTORE_PASSWORD", "")
                keyAlias = localProps.getProperty("KEY_ALIAS", "")
                keyPassword = localProps.getProperty("KEY_PASSWORD", "")
            }
        }
        // "debug" signing config is provided automatically by AGP; no declaration needed.
    }

    buildTypes {
        release {
            // Use production keystore when available, otherwise fall back to the
            // debug keystore so the APK is always installable (sideload / CI).
            signingConfig = if (keystoreFile.isNotEmpty())
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.service)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.coroutines.android)

    implementation(libs.okhttp)
}
