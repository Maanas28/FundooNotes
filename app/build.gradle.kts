plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")

}

android {
    namespace = "com.example.fundoonotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fundoonotes"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.lifecycle.runtime.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.cardview)
    implementation(libs.firebase.messaging)
    implementation(libs.cloudinary.android)
    implementation(libs.glide)
    ksp(libs.compiler)
    implementation ("androidx.room:room-runtime:2.7.0")
    ksp ("androidx.room:room-compiler:2.7.0")
    implementation ("androidx.room:room-ktx:2.7.0")
    implementation(libs.play.services.auth) // or latest
    implementation("com.google.android.gms:play-services-identity:18.1.0")
    implementation("androidx.credentials:credentials:1.2.0-alpha01")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0-alpha01")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1") // Credential Manager with Google
    implementation ("com.google.code.gson:gson:2.10.1")
}