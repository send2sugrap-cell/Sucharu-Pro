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
    implementation(libs.gson)
    // firebase-admin: compileOnly so it does NOT propagate to the Android :app module via transitive
    // dependency resolution. This prevents duplicate class conflicts with firebase-auth-ktx (Android SDK).
    // Server-side deployments must include firebase-admin on their runtime classpath separately.
    compileOnly(libs.firebase.admin)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
    // firebase-admin needed at test runtime so FirebaseTokenVerifier can be instantiated in unit tests
    testImplementation(libs.firebase.admin)
}

tasks.test {
    useJUnit()
    jvmArgs("-Xmx2g", "-XX:+UseG1GC")
    testLogging {
        events("passed", "skipped", "failed")
    }
}
