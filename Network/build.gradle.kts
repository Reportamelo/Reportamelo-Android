plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.reportamelo.network"
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

}
val kotlinxSerializationMsgPackVersion = project.findProperty("kotlinxSerializationMsgPackVersion")
val ktor_version = project.findProperty("ktor_version")

dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.ensarsarajcic.kotlinx:serialization-msgpack:${kotlinxSerializationMsgPackVersion}")
    implementation("com.ensarsarajcic.kotlinx:serialization-msgpack-timestamp-extension:${kotlinxSerializationMsgPackVersion}")
    implementation("io.ktor:ktor-client-core:${ktor_version}")
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")
    implementation(project(":Models"))
    implementation(project(":Commons"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}