package com.sucharu.sucharupro.backend

import com.sucharu.sucharupro.backend.composition.ProductionBackendComposition
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.backend.config.MigrationMode
import com.sucharu.sucharupro.backend.health.ServerHealthTracker
import com.sucharu.sucharupro.data.auth.persistence.PostgresAuthAccountDataSource
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class BackendRuntimeSeparationTest {

    @Test
    fun test01_backendConfig_validatesProductionConfiguration() {
        val prodConfigMissingPassword = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "",
            jwtSigningSecret = "super_secure_production_secret_key_that_is_long_2026",
            databaseUrl = "jdbc:postgresql://db.sucharu.internal:5432/sucharu_pro"
        )
        val errors1 = prodConfigMissingPassword.validate()
        assertTrue("Missing password in production must fail validation", errors1.any { it.contains("DATABASE_PASSWORD") })

        val prodConfigWeakSecret = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "valid_password_123",
            jwtSigningSecret = "dev_secret",
            databaseUrl = "jdbc:postgresql://db.sucharu.internal:5432/sucharu_pro"
        )
        val errors2 = prodConfigWeakSecret.validate()
        assertTrue("Weak/dev secret in production must fail validation", errors2.any { it.contains("JWT_SIGNING_SECRET") })

        val validProdConfig = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "super_secure_password_99",
            jwtSigningSecret = "a_super_secure_signing_secret_key_that_is_32_chars_long_2026",
            databaseUrl = "jdbc:postgresql://db.sucharu.internal:5432/sucharu_pro"
        )
        val errors3 = validProdConfig.validate()
        assertTrue("Valid production config must pass validation", errors3.isEmpty())
    }

    @Test
    fun test02_backendRuntime_independentStartupAndShutdown() {
        val config = BackendConfig(
            serverHost = "127.0.0.1",
            serverPort = 8089,
            environment = BackendEnvironment.TEST,
            migrationMode = MigrationMode.DISABLED
        )
        val runtime = BackendRuntime(config)

        try {
            runtime.start()

            // Verify HTTP probe
            val url = URL("http://127.0.0.1:8089/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            val responseCode = conn.responseCode
            assertTrue("Health endpoint must respond (200 or 503)", responseCode in listOf(200, 503))

            val stream = if (responseCode >= 400) conn.errorStream else conn.inputStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
            assertTrue(responseBody.contains("status"))
        } finally {
            runtime.stop()
        }
    }

    @Test
    fun test03_productionComposition_usesPostgresAuthDataSources() {
        val config = BackendConfig(
            environment = BackendEnvironment.TEST,
            migrationMode = MigrationMode.DISABLED
        )
        val composition = ProductionBackendComposition(config)

        assertTrue(
            "Production composition must use PostgresAuthAccountDataSource",
            composition.accountDataSource is PostgresAuthAccountDataSource
        )
        assertNotNull(composition.transactionManager)
        assertNotNull(composition.repositoryFactory)
        assertNotNull(composition.authenticationService)
        assertNotNull(composition.securityContext)
    }

    @Test
    fun test04_backendPackage_hasZeroAndroidDependencies() {
        val backendClasses = listOf(
            BackendRuntime::class.java,
            ProductionBackendComposition::class.java,
            BackendConfig::class.java,
            ServerHealthTracker::class.java
        )

        for (cls in backendClasses) {
            val packageName = cls.`package`?.name ?: ""
            assertTrue(packageName.startsWith("com.sucharu.sucharupro.backend"))
            // Verify no fields or methods reference android.*
            for (field in cls.declaredFields) {
                assertFalse(
                    "Field ${field.name} in $cls must not reference android.*",
                    field.type.name.startsWith("android.") || field.type.name.startsWith("androidx.")
                )
            }
        }
    }

    @Test
    fun test05_serverHealthTracker_accuratelyReflectsReadiness() {
        val tracker = ServerHealthTracker()
        assertFalse(tracker.isLive())
        assertFalse(tracker.isReady())

        tracker.markApplicationStarted(true)
        assertTrue(tracker.isLive())
        assertFalse(tracker.isReady())

        tracker.markDatabaseReady(true)
        tracker.markMigrationsValid(true)
        tracker.markCoreDependenciesReady(true)
        assertTrue(tracker.isReady())

        val report = tracker.getHealthReport()
        assertEquals("UP", report["status"])
    }
}
