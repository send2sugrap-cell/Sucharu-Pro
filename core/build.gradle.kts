plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.postgresql)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.test {
    useJUnit()
    jvmArgs("-Xmx2g", "-XX:+UseG1GC")
    testLogging {
        events("passed", "skipped", "failed")
    }
}
