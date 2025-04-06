plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.example.hectoclash"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hectoclash"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.appcompat)
    implementation(libs.retrofit)
    implementation(libs.exp4j)
    implementation(libs.java.websocket)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.converter.gson)
    implementation (libs.cloudinary.android)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.compiler)
    implementation (libs.androidx.ui)
    implementation(libs.dotlottie.android)
    implementation (libs.circleimageview)
    implementation (libs.glide)
    implementation(libs.photoview)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}