plugins {
    kotlin("jvm")
    id("formver.source-layout")
}

dependencies {
    compileOnly(kotlin("compiler"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.7")
    implementation(project(":formver.compiler-plugin:locality"))
}
