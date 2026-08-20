import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    `java-test-fixtures`
    id("com.github.gmazzo.buildconfig")
    idea
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

sourceSets {
    main {
        java.setSrcDirs(listOf<String>())
        resources.setSrcDirs(listOf<String>())
    }
    testFixtures {
        java.setSrcDirs(listOf("test-fixtures"))
    }
    test {
        java.setSrcDirs(listOf("test", "test-gen"))
        resources.setSrcDirs(listOf("testData", "test-resources"))
    }
}

idea {
    module.generatedSourceDirs.add(projectDir.resolve("test-gen"))
}

val annotationsRuntimeClasspath: Configuration by configurations.creating { isTransitive = false }

dependencies {
    implementation(project(":formver.compiler-plugin:core")) { isTransitive = false }
    implementation(project(":formver.compiler-plugin:uniqueness")) { isTransitive = false }
    implementation(project(":formver.compiler-plugin:viper")) { isTransitive = true }
    implementation(project(":formver.compiler-plugin:cli")) { isTransitive = false }
    implementation(project(":formver.compiler-plugin:plugin")) { isTransitive = false }
    implementation(project(":formver.compiler-plugin:locality")) { isTransitive = false }
    implementation(project(":formver.common")) { isTransitive = false }
    // Provided by whichever compiler runs the plugin. Shading a second copy in would
    // put compiler classes in the plugin's own class loader next to the ones the
    // running compiler uses, and the two sets do not interoperate.
    compileOnly(kotlin("compiler"))

    testFixturesApi(kotlin("test-junit5"))
    testFixturesApi(kotlin("compiler-internal-test-framework"))
    testFixturesApi(kotlin("compiler"))
    testFixturesImplementation(project(":formver.common"))
    testFixturesImplementation(project(":formver.compiler-plugin:plugin"))
    testFixturesApi(project(":formver.compiler-plugin:viper"))
    testFixturesApi("viper:silicon_2.13:1.2-SNAPSHOT")
    testFixturesImplementation(project(":formver.compiler-plugin:core"))
    testFixturesImplementation(project(":formver.compiler-plugin:locality"))
    testFixturesImplementation(project(":formver.compiler-plugin:uniqueness"))

    annotationsRuntimeClasspath(project(":formver.annotations"))

    testImplementation(project(":formver.compiler-plugin:plugin"))
    testImplementation(project(":formver.compiler-plugin:locality"))
    testImplementation(project(":formver.common"))
    testImplementation(project(":formver.compiler-plugin:uniqueness"))
    testRuntimeOnly(project(":formver.compiler-plugin:core"))
    testRuntimeOnly(project(":formver.compiler-plugin:viper"))

    // Dependencies required to run the internal test framework.
    testRuntimeOnly("junit:junit:4.13.2")
    testRuntimeOnly(kotlin("reflect"))
    testRuntimeOnly(kotlin("test"))
    testRuntimeOnly(kotlin("script-runtime"))
    testRuntimeOnly(kotlin("annotations-jvm"))
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    packageName(group.toString())
}

fun Test.configureFormverTest() {
    dependsOn(annotationsRuntimeClasspath)

    useJUnitPlatform()
    workingDir = rootDir

    systemProperty("annotationsRuntime.classpath", annotationsRuntimeClasspath.asPath)

    // Properties required to run the internal test framework.
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

    systemProperty("idea.ignore.disabled.plugins", "true")
    systemProperty("formver.testRun", "true")
    systemProperty("idea.home.path", rootDir)

    project.findProperty("kotlin.test.update.test.data")?.let {
        systemProperty("kotlin.test.update.test.data", it)
    }

    jvmArgs = listOf("-Xss30M", "-Xmx2g", "-XX:MaxMetaspaceSize=512m")
}

// ./gradlew test — normal mode (full verification)
tasks.test {
    configureFormverTest()
    systemProperty("formver.testMode", "FULL")
}

tasks.register<Test>("untilConversion") {
    description = "Runs until conversion"
    group = "verification"
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    configureFormverTest()
    systemProperty("formver.testMode", "CHECK_CONVERSION")
}

tasks.register<Test>("update") {
    description = "Runs conversion and verification iff conversion changed"
    group = "verification"
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    configureFormverTest()
    systemProperty("formver.testMode", "UPDATE")
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

val generateTests by tasks.registering(JavaExec::class) {
    inputs.dir(layout.projectDirectory.dir("testData"))
        .withPropertyName("testData")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(layout.projectDirectory.dir("test-gen"))
        .withPropertyName("generatedTests")

    classpath = sourceSets.testFixtures.get().runtimeClasspath
    mainClass.set("org.jetbrains.kotlin.formver.plugin.GenerateTestsKt")
    workingDir = rootDir
}

tasks.compileTestKotlin {
    dependsOn(generateTests)
}

fun Test.setLibraryProperty(propName: String, jarName: String) {
    val path = project.configurations
        .testRuntimeClasspath.get()
        .files
        .find { """$jarName-\d.*jar""".toRegex().matches(it.name) }
        ?.absolutePath
        ?: return
    systemProperty(propName, path)
}

// This project has no sources of its own, so the plain jar task has nothing to put in
// its archive — and it names that archive exactly what shadowJar names its own, which
// leaves whichever ran last in build/libs.
tasks.jar {
    enabled = false
}

// The jar for `kotlinc`, which runs the plugin against the plain compiler.
tasks.shadowJar {
    archiveClassifier.set("")
}

// The jar for the Gradle plugin, which runs it against kotlin-compiler-embeddable.
//
// The embeddable compiler moves the libraries it bundles under its own package prefix,
// and some of them show up in the compiler API the plugin builds on: PSI elements in
// diagnostic factories, persistent maps in the control-flow signatures the plugin
// overrides. References compiled against the plain compiler have to be rewritten to
// match, or the plugin looks for classes the running compiler does not have.
//
// This list covers the relocated packages the plugin's own classes mention. Comparing
// the package layouts of kotlin-compiler and kotlin-compiler-embeddable shows which
// packages are subject to it, should the plugin come to depend on more of them.
val embeddableJar by tasks.registering(ShadowJar::class) {
    archiveBaseName.set("${project.name}-embeddable")
    archiveClassifier.set("")
    from(sourceSets.main.map { it.output })
    configurations.add(project.configurations.runtimeClasspath)
    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
    relocate("kotlinx.collections.immutable", "org.jetbrains.kotlin.kotlinx.collections.immutable")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar)
        }
        create<MavenPublication>("embeddable") {
            artifactId = "${project.name}-embeddable"
            artifact(embeddableJar)
        }
    }
}
