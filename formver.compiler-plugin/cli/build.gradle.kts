plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlin("compiler"))
    implementation(project(":formver.common"))
    implementation(project(":formver.compiler-plugin:plugin"))
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