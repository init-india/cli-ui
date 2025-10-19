// smartcli/core/build.gradle.kts
plugins {
    id("com.android.library")  // ← CHANGE to Android library
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.smartcli.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

sourceSets {
        getByName("main") {
            java.srcDirs("cli", "system")
            res.srcDirs("res") 
            manifest.srcFile("AndroidManifest.xml")

        }
    }





}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    testImplementation("junit:junit:4.13.2")


}




tasks.withType<Test> {
    useJUnit()
}
