plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":formver.compiler-plugin:core"))
    compileOnly(project(":formver.compiler-plugin:uniqueness"))
    compileOnly(project(":formver.common"))
    compileOnly(project(":formver.compiler-plugin:viper"))

    compileOnly(kotlin("compiler"))
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
