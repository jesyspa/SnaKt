plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":formver.common"))
    compileOnly(project(":formver.compiler-plugin:viper"))
    compileOnly(kotlin("compiler"))

    // TODO: figure out how to avoid this dependency
    compileOnly("viper:silicon_2.13:1.2-SNAPSHOT")
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
