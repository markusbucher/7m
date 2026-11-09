plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.tanpau.sevenm"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.tanpau.sevenm"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"
    }

    signingConfigs {
        // Echter Schlüssel, falls über Umgebungsvariablen/Gradle-Properties angegeben (z. B. in der CI).
        val storeFilePath = providers.environmentVariable("SIGNING_STORE_FILE").orNull
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Ohne eigenen Schlüssel mit dem Debug-Schlüssel signiert, damit die APK direkt installierbar ist.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += setOf("META-INF/*.kotlin_module", "META-INF/INDEX.LIST") }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}
