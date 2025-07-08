plugins {
    kotlin("jvm")
    id("com.github.gmazzo.buildconfig")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("maven-publish")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf<String>())
        resources.setSrcDirs(listOf<String>())
    }
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
    implementation(project(":formver.common"))
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField("String", "GRADLE_PLUGIN_ID", "\"${rootProject.group}\"")

    val pluginProject = project(":formver.compiler-plugin")
    buildConfigField("String", "COMPILER_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "COMPILER_PLUGIN_NAME", "\"${pluginProject.name}\"")
    buildConfigField("String", "COMPILER_PLUGIN_VERSION", "\"${pluginProject.version}\"")

    val annotationsProject = project(":formver.annotations")
    buildConfigField(
        type = "String",
        name = "ANNOTATIONS_LIBRARY_COORDINATES",
        expression = "\"${annotationsProject.group}:${annotationsProject.name}:${annotationsProject.version}\""
    )
}

gradlePlugin {
    website = "https://github.com/jesyspa/SnaKt"
    vcsUrl = "https://github.com/jesyspa/SnaKt.git"
    plugins {
        create("SnaKtFormverPlugin") {
            id = rootProject.group.toString()
            displayName = "SnaKt"
            description = "Kotlin plugin adding formal verification support"
            implementationClass = "org.jetbrains.kotlin.formver.gradle.FormVerGradleSubplugin"
            tags = listOf("kotlin", "formal-verification")
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name = rootProject.group.toString()
                description = "FormVer Gradle Plugin for SnaKt"
            }
        }
    }
}