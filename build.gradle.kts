import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0" apply false
    id("com.github.gmazzo.buildconfig") version "5.6.5"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3" apply false
    id("com.gradle.plugin-publish") version "1.3.1" apply false
    id("dev.detekt") version "2.0.0-alpha.6"
}

// The JDK this project compiles against and targets. Pinned rather than
// inherited from the launching JVM so the bytecode does not depend on which
// JDK a developer happens to run Gradle on.
val jdkVersion = 21

allprojects {
    group = "org.jetbrains.kotlin.formver"
    version = "0.1.0-SNAPSHOT"

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(jdkVersion)
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }
}

subprojects {
    apply(plugin = "dev.detekt")

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        baseline = rootProject.file("config/detekt/baseline.xml")
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = jdkVersion.toString()
        // Project uses a custom layout (src/ instead of src/main/kotlin) — list the
        // source roots explicitly so detekt scans them. test-gen is not listed: it
        // contains only generated Java test runners (produced by generateTests),
        // which detekt does not analyze.
        setSource(
            files(
                "src",
                "test",
                "test-fixtures",
            ).filter { it.exists() }
        )
        include("**/*.kt")
        include("**/*.kts")
        exclude("**/build/**")
        reports {
            html.required.set(true)
            checkstyle.required.set(true)
            sarif.required.set(false)
            markdown.required.set(false)
        }
    }
}
