plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Align serialization plugin with Kotlin version from version catalog
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.example.strive"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.strive"
        minSdk = 26
        targetSdk = 34
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    // Fragment KTX for viewModels delegate, fragment scopes, etc.
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")


    // Preferences and Work Manager
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.8.0")

    // Coordinator Layout for complex layouts
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // ViewPager2 for onboarding
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}