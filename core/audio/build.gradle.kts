plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.amusic.core.audio"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":core:common"))
    implementation(project(":core:storage"))
    implementation(project(":data"))
    implementation(libs.media3.exoplayer)
    api(libs.media3.session)
    implementation(libs.guava)
    implementation(libs.androidx.annotation.experimental)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
