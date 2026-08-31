pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
}

// Provisions the JDK the build pins its toolchain to, for developers who do not
// have it installed.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
include("formver.compiler-plugin:locality")
include("formver.compiler-plugin:core")
