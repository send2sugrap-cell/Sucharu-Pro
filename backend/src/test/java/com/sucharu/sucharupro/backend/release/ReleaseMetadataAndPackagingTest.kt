package com.sucharu.sucharupro.backend.release

import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.data.observability.model.ReleaseMetadata
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.jar.JarFile

/**
 * Release Metadata & Packaging Integrity Test (INFRA-05 Step 07).
 * Verifies release identity model, JAR manifest attributes, and zero-secret packaging.
 */
class ReleaseMetadataAndPackagingTest {

    @Test
    fun testReleaseMetadataModelAndSafeMap() {
        val metadata = ReleaseMetadata(
            appName = "sucharu-backend",
            appVersion = "1.0.0",
            buildVersion = "1.0.0-PROD",
            gitRevision = "commit-abc1234",
            environment = "production",
            buildTimestamp = "2026-08-25T12:00:00Z"
        )

        val map = metadata.toSafeMap()
        assertEquals("sucharu-backend", map["appName"])
        assertEquals("1.0.0", map["appVersion"])
        assertEquals("1.0.0-PROD", map["buildVersion"])
        assertEquals("commit-abc1234", map["gitRevision"])
        assertEquals("production", map["environment"])
        assertEquals("2026-08-25T12:00:00Z", map["buildTimestamp"])

        // Ensure zero secret leakage in release metadata
        assertFalse(map.containsKey("password"))
        assertFalse(map.containsKey("jwtSecret"))
        assertFalse(map.containsKey("apiKey"))
    }

    @Test
    fun testConfigProvidesReleaseMetadata() {
        val config = BackendConfig(
            appName = "sucharu-custom-service",
            appVersion = "2.1.0",
            buildVersion = "2.1.0-RELEASE"
        )
        val metadata = config.getReleaseMetadata()
        assertEquals("sucharu-custom-service", metadata.appName)
        assertEquals("2.1.0", metadata.appVersion)
        assertEquals("2.1.0-RELEASE", metadata.buildVersion)
    }

    @Test
    fun testJarArtifactManifestAttributesIfJarExists() {
        val jarFile = File("build/libs/sucharu-server.jar")
        if (jarFile.exists()) {
            JarFile(jarFile).use { jar ->
                val manifest = jar.manifest
                assertNotNull("Manifest must exist in fat JAR", manifest)
                val mainAttributes = manifest.mainAttributes
                assertEquals(
                    "com.sucharu.sucharupro.backend.BackendApplicationKt",
                    mainAttributes.getValue("Main-Class")
                )
                assertEquals(
                    "Sucharu Pro Standalone Backend",
                    mainAttributes.getValue("Implementation-Title")
                )
                assertEquals(
                    "1.0.0",
                    mainAttributes.getValue("Implementation-Version")
                )
            }
        }
    }

    @Test
    fun testNoSecretsInArtifactTree() {
        val forbiddenSubstrings = listOf(".env", ".pem", ".key", "local.properties", "id_rsa")
        val jarFile = File("build/libs/sucharu-server.jar")
        if (jarFile.exists()) {
            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    for (forbidden in forbiddenSubstrings) {
                        assertFalse(
                            "JAR artifact must not contain sensitive file: ${entry.name}",
                            name.contains(forbidden)
                        )
                    }
                }
            }
        }
    }
}
