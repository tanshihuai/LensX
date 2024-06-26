import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.example.terraforming"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.terraforming"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "googleMapsAPIKey", localProperties["googleMapsAPIKey"] as String? ?: "\"\"")
        buildConfigField("String", "openWeatherAPIKey", localProperties["openWeatherAPIKey"] as String? ?: "\"\"")
        buildConfigField("String", "openAIAPIKey", localProperties["openAIAPIKey"] as String? ?: "\"\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation(platform("com.aallam.openai:openai-client-bom:3.6.1"))
    implementation("com.squareup.picasso:picasso:2.8")


    implementation("com.aallam.openai:openai-client")
    runtimeOnly("io.ktor:ktor-client-okhttp")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("com.google.android.libraries.places:places:3.3.0")

    implementation("com.google.android.material:material:1.5.0")
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("uk.co.samuelwall:material-tap-target-prompt:3.3.2")
    implementation ("io.github.aghajari:ZoomHelper:1.1.0")
}