plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "nz.co.zsd.floatingoverlay"
    compileSdk = 34

    defaultConfig {
        applicationId = "nz.co.zsd.floatingoverlay"
        minSdk = 28
        targetSdk = 34
        versionCode = 11
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // If keystore path provided sign the debug APK, otherwise generate unsigned
            if (properties.containsKey("debug_keystore")) {
                println("Signing debug APK")
                storeFile = file(properties["debug_keystore"] as String)
                keyAlias = properties.getOrDefault("key_alias", "floatingoverlay") as String
                storePassword = properties.getOrDefault("store_pass", "") as String
                keyPassword = properties.getOrDefault("key_pass", "") as String
            } else {
                println("No debug keystore file path property!")
            }
        }
        create("release") {
            // If keystore path provided sign the release APK, otherwise generate unsigned
            if (properties.containsKey("release_keystore")) {
                println("Signing release APK")
                storeFile = file(properties["release_keystore"] as String)
                keyAlias = properties.getOrDefault("key_alias", "floatingoverlay") as String
                storePassword = properties.getOrDefault("store_pass", "") as String
                keyPassword = properties.getOrDefault("key_pass", "") as String
            } else {
                println("No release keystore file path property!")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            versionNameSuffix = "-RC"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Navigation components plugin
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.7.5")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.5")
}
