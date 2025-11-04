import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.xiaopeng.xposed.instrument.compass.ribbon"
    compileSdk {
        version = release(libs.versions.sdkCompile.get().toInt())
    }
    defaultConfig {
        applicationId = "com.xiaopeng.xposed.instrument.compass.ribbon"
        minSdk = libs.versions.sdkMin.get().toInt()
        targetSdk = libs.versions.sdkTarget.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = false
        base.archivesName = "XPluginInstrumentCompassRibbon-$versionName-$versionCode-" + SimpleDateFormat("yyyyMMdd").format(Date())
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        aidl = true
        viewBinding = false
        buildConfig = false
    }
    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/README.txt"
            excludes += "kotlin-tooling-metadata.json"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin/**"
        }
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("app.jks")
            storePassword = "123456"
            keyAlias = "debug"
            keyPassword = "123456"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
        create("release") {
            storeFile = file("app.jks")
            storePassword = "123456"
            keyAlias = "release"
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), file("proguard-rules.pro"), file("proguard-log.pro"))
            isShrinkResources = true
        }
    }
}

dependencies {
    implementation(libs.common.joor)

    debugImplementation(libs.androidx.core.ktx)
    debugImplementation(libs.androidx.appcompat)
    debugImplementation(libs.androidx.constraint)

    compileOnly(libs.androidx.core.ktx)
    compileOnly(libs.androidx.appcompat)
    compileOnly(libs.androidx.constraint)
    compileOnly(libs.common.xposed)
    compileOnly(libs.common.framework)
    compileOnly(libs.common.xpeng) { artifact { type = "aar" } }
}
