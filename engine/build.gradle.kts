plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "256m"
    systemProperty(
        "heapscout.runHprofIntegration",
        System.getProperty("heapscout.runHprofIntegration", "false"),
    )
    systemProperty(
        "heapscout.runLargeHprofIntegration",
        System.getProperty("heapscout.runLargeHprofIntegration", "false"),
    )
}
