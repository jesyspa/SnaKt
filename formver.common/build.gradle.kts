plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlin("compiler"))
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
