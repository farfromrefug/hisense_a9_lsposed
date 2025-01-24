plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "com.akylas.hisensea9.lsposed"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        archivesBaseName = "$applicationId-v$versionCode($versionName)"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }

    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x42")
    }

    kotlin {
        jvmToolchain(17)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        checkReleaseBuilds = false
        disable +=
            listOf(
                "Internationalization",
                "UnsafeIntentLaunch",
                "SetJavaScriptEnabled",
                "UnspecifiedRegisterReceiverFlag",
                "Usability:Icons")
    }
}

dependencies {
//    compileOnly(libs.libxposed.api)
//    implementation(libs.libxposed.service)
    dependencies { compileOnly(libs.api) }
    implementation(libs.core.ktx)
    implementation(libs.preference.ktx)
    implementation (libs.material)
}

