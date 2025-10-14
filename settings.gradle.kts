// smartcli/settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application", "com.android.library" -> {
                    useModule("com.android.tools.build:gradle:8.1.2")
                }
                "org.jetbrains.kotlin.android", 
                "org.jetbrains.kotlin.jvm",
                "kotlin-parcelize",
                "kotlin-kapt" -> {
                    useVersion("1.9.22")
                }
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SmartCLI"
include(":core")
include(":fdroid")

// Configure projects
gradle.beforeProject { project ->
    project.extensions.extraProperties.set("buildDir", project.layout.buildDirectory.get().asFile.absolutePath)
}
