import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.binary.compatibility.validator) apply false
    alias(libs.plugins.plugin.publish) apply false
    alias(libs.plugins.detekt)
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

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "21"
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
            xml.required.set(true)
            sarif.required.set(false)
            md.required.set(false)
        }
    }
}
