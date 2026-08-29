import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    kotlin("jvm")
    id("com.github.gmazzo.buildconfig")
}

dependencies {
    compileOnly(kotlin("compiler"))
    implementation(project(":formver.common"))
    implementation(project(":formver.compiler-plugin:plugin"))
    implementation(project(":formver.compiler-plugin:locality"))
    implementation(project(":formver.compiler-plugin:uniqueness"))
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    packageName("org.jetbrains.kotlin.formver.cli")

    // Checked at load time against the running compiler's version.
    buildConfigField("String", "BUILT_AGAINST_KOTLIN_VERSION", "\"${getKotlinPluginVersion()}\"")
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDir("resources")
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}
