plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    js(IR) { browser(); nodejs() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies { }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "io.kparchment"
    compileSdk = 35
    defaultConfig { minSdk = 21 }
}
