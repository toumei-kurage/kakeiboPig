plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.ksp.gradle.plugin)
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "com.websarva.wings.android.kakeibo0422"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.websarva.wings.android.kakeibo0422"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = "rekadon1"
            keyAlias = "kakeiboAppKey"
            keyPassword = "rekadon1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))  // Firebase BOM (Bill of Materials)で、依存関係のバージョンを管理
    implementation(libs.firebase.analytics)  // Firebase Analytics
    implementation(libs.firebase.auth.ktx)  // Firebase Authentication (Kotlin拡張)
    implementation(libs.firebase.firestore)  // Firebase Firestore
    implementation(libs.play.services.auth.v2001)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.oss.licenses.v1700)
}

