plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.reportamelo.commons"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"development-api.reportamelo.app\"")
            buildConfigField("String", "SHARE_URL", "\"development-share.reportamelo.app\"")
        }
        release {
            buildConfigField("String", "API_URL", "\"api.reportamelo.app\"")
            buildConfigField("String", "SHARE_URL", "\"share.reportamelo.app\"")
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}