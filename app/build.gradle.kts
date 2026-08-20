plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

tasks.register<Exec>("buildGoTunnel") {
    val tunnelDir = rootProject.file("tunnel")
    val outputAar = file("libs/tunnel.aar")

    inputs.dir(tunnelDir)
    outputs.files(outputAar, file("libs/tunnel-sources.jar"))
    workingDir = tunnelDir
    environment("GOFLAGS", "-buildvcs=false")
    commandLine(
        "gomobile", "bind",
        "-target=android",
        "-androidapi", "29",
        "-trimpath",
        "-ldflags=-s -w -buildid= -extldflags=-Wl,-z,max-page-size=16384",
        "-o", outputAar.absolutePath,
        "."
    )
}

android {
    namespace = "dev.chiraitori.anis"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.chiraitori.anis"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.androidx.core.splashscreen)

    // GPL-3.0 Go/gVisor tunnel engine ported from BlockAds.
    implementation(files("libs/tunnel.aar"))

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
