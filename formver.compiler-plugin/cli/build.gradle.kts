plugins {
    kotlin("jvm")
    id("formver.source-layout")
}

dependencies {
    compileOnly(kotlin("compiler"))
    implementation(project(":formver.common"))
    implementation(project(":formver.compiler-plugin:plugin"))
    implementation(project(":formver.compiler-plugin:locality"))
    implementation(project(":formver.compiler-plugin:uniqueness"))
}
