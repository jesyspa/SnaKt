pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/kotlin-formver/maven")
        }
    }
}

rootProject.name = "kotlin-formver"

include("formver.compiler-plugin")
include("formver.gradle-plugin")
include("formver.annotations")
include("formver.common")
include("formver.compiler-plugin:cli")
include("formver.compiler-plugin:uniqueness")
include("formver.compiler-plugin:viper")
include("formver.compiler-plugin:plugin")
include("formver.compiler-plugin:core")
