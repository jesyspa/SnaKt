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

    // The plugin binds to compiler internals, so the compiler that loads it has
    // to be close enough to the one it was built against.
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
