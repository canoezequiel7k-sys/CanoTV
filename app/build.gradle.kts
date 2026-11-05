plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseSigningProperties = listOf(
    "canotvReleaseStoreFile",
    "canotvReleaseStorePassword",
    "canotvReleaseKeyAlias",
    "canotvReleaseKeyPassword"
).associateWith { providers.gradleProperty(it).orNull }
val hasReleaseSigning = releaseSigningProperties.values.all { it != null }
require(hasReleaseSigning || releaseSigningProperties.values.all { it == null }) {
    "La firma release requiere las cuatro propiedades canotvRelease* en gradle.properties local."
}

android {
    namespace = "com.cano.launcher"
    compileSdk = 34

    lint {
        disable += "ExpiredTargetSdkVersion"
    }

    defaultConfig {
        applicationId = "com.cano.launcher"
        minSdk = 25
        targetSdk = 25
        versionCode = 2
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseSigningProperties.getValue("canotvReleaseStoreFile")!!)
                storePassword = releaseSigningProperties.getValue("canotvReleaseStorePassword")
                keyAlias = releaseSigningProperties.getValue("canotvReleaseKeyAlias")
                keyPassword = releaseSigningProperties.getValue("canotvReleaseKeyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
