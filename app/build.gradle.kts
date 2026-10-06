import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// ---------------------------------------------------------------------------------------------
// Release signing. NOTHING secret is stored in this project.
// The release key comes from EITHER
//   1) a file named keystore.properties in the project root (never committed to git), OR
//   2) environment variables (used by the GitHub cloud build, filled from GitHub "Secrets").
// See PUBLISHING.md for the exact values you must provide.
// ---------------------------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(propName: String, envName: String): String? =
    (keystoreProps.getProperty(propName) ?: System.getenv(envName))?.trim()?.takeIf { it.isNotEmpty() }

val releaseStoreFile = signingValue("storeFile", "RELEASE_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")

// The keystore file (path may be absolute, or relative to the project root / app folder).
val releaseStore = releaseStoreFile?.let { p ->
    listOf(rootProject.file(p), file(p)).firstOrNull { it.exists() }
}

val hasReleaseSigning = releaseStore != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

val minifyRelease = (findProperty("minifyRelease") as String?) != "false"

android {
    namespace = "com.taka.personalfinance"
    compileSdk = 36

    defaultConfig {
        // The application ID must be unique worldwide on Google Play. Change it ONCE, before the
        // first upload, if you want your own (for example com.yourname.personalfinance).
        applicationId = "com.taka.personalfinance"
        minSdk = 26
        targetSdk = 36
        // Every update you publish must have a HIGHER versionCode. The cloud build passes its run
        // number automatically (-PversionCode=...). versionName is the number users see.
        versionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "1.0.0"
    }

    signingConfigs {
        // Standard Android debug key (public, well-known defaults). Debug builds only.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = minifyRelease
            isShrinkResources = minifyRelease
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without your own key the release build is signed with the debug key so that it can
            // still be installed for testing. Such a build must NOT be published or shared widely.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Room writes its database layout (needed to write safe upgrades later) into app/schemas/.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}
