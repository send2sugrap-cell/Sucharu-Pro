plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("com.sucharu.sucharupro.backend.BackendApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.sucharu.sucharupro.backend.BackendApplicationKt"
        attributes["Implementation-Title"] = "Sucharu Pro Standalone Backend"
        attributes["Implementation-Version"] = "1.0.0"
        attributes["Implementation-Vendor"] = "Sucharu Pro Engineering"
        attributes["Build-Timestamp"] = "2026-08-25T00:00:00Z"
    }
    archiveBaseName.set("sucharu-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.test {
    useJUnit()
    jvmArgs("-Xmx2g", "-XX:+UseG1GC")
    testLogging {
        events("passed", "skipped", "failed")
    }
}
