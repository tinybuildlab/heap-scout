plugins {
    kotlin("jvm") version "2.2.21" apply false
    id("org.springframework.boot") version "3.5.16" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

val heapScoutVersion = providers.gradleProperty("releaseVersion").getOrElse("0.1.0-SNAPSHOT")

allprojects {
    group = "io.heapscout"
    version = heapScoutVersion

    repositories {
        mavenCentral()
    }
}
