import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.petmaker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.petmaker"
        minSdk = 26 // 안드로이드 8.0 (API 26) 이상 구동 보장
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // .env 파일 로드하여 API 키 읽기
        val envProperties = Properties()
        val envFile = rootProject.file("../.env")
        if (envFile.exists()) {
            envFile.inputStream().use { stream ->
                envProperties.load(stream)
            }
        }

        val weatherApiKey = envProperties.getProperty("OPENWEATHER_API_KEY") ?: ""
        val geminiApiKey = envProperties.getProperty("GEMINI_API_KEY") ?: ""
        val hfApiKey = envProperties.getProperty("HF_API_KEY") ?: ""
        val openAiApiKey = envProperties.getProperty("OPENAI_API_KEY") ?: ""
        
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "HF_API_KEY", "\"$hfApiKey\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")

        // 지도 API 키: .env를 우선하고 없으면 local.properties 사용
        var mapsApiKey = envProperties.getProperty("MAPS_API_KEY") ?: ""
        if (mapsApiKey.isEmpty()) {
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { stream ->
                    localProperties.load(stream)
                }
            }
            mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: "YOUR_API_KEY"
        }
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Coil (Image Loading)
    implementation(libs.coil.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)

    // Play Services Location
    implementation(libs.play.services.location)

    // Google Maps SDK & Compose Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Navigation Compose
    implementation(libs.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}