plugins {
    kotlin("jvm")
    id("maven-publish")
    `java-library`
    id("formver.source-layout")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("compiler"))
    api(kotlin("compiler-internal-test-framework"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
