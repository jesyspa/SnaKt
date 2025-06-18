import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0-dev-15683" apply false
    id("com.github.gmazzo.buildconfig") version "5.6.5"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3" apply false
}

allprojects {
    group = "org.jetbrains.kotlin.formver"
    version = "0.1.0-SNAPSHOT"

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }
}
