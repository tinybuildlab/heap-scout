plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(project(":engine"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("heapscout.jar")
    val frontendDistribution = rootProject.layout.projectDirectory.dir("frontend/dist")
    doFirst {
        check(frontendDistribution.asFile.isDirectory) {
            "Frontend distribution is missing. Run `npm ci && npm run build` in frontend first."
        }
    }
    from(frontendDistribution) {
        into("BOOT-INF/classes/static")
    }
}
