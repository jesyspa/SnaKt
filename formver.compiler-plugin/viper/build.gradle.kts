plugins {
    kotlin("jvm")
}

dependencies {
    implementation("viper:silicon_2.13:1.2-SNAPSHOT")
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
