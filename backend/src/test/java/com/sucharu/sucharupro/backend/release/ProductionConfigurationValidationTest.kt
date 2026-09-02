package com.sucharu.sucharupro.backend.release

import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.backend.config.MigrationMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Production Configuration & Fail-Fast Validation Test Suite (INFRA-05 Step 07).
 * Verifies environment variable mapping, production security guards, secret redaction,
 * and fail-fast validation.
 */
class ProductionConfigurationValidationTest {

    @Test
    fun testDefaultConfigurationIsValidInDevelopment() {
        val config = BackendConfig()
        val errors = config.validate()
        assertTrue("Default config in development mode must have zero validation errors", errors.isEmpty())
        assertEquals(8080, config.serverPort)
        assertEquals(BackendEnvironment.DEVELOPMENT, config.environment)
        assertEquals(MigrationMode.AUTO_APPLY, config.migrationMode)
        assertTrue(config.flywayEnabled)
    }

    @Test
    fun testInvalidServerPortRejected() {
        val configNegative = BackendConfig(serverPort = -1)
        val errorsNegative = configNegative.validate()
        assertTrue(errorsNegative.any { it.contains("Invalid server port") })

        val configZero = BackendConfig(serverPort = 0)
        val errorsZero = configZero.validate()
        assertTrue(errorsZero.any { it.contains("Invalid server port") })

        val configTooHigh = BackendConfig(serverPort = 70000)
        val errorsTooHigh = configTooHigh.validate()
        assertTrue(errorsTooHigh.any { it.contains("Invalid server port") })
    }

    @Test
    fun testInvalidPoolSizesAndZeroTimeoutsRejected() {
        val config = BackendConfig(
            databasePoolSize = 0,
            workerPoolSize = 0,
            workerPollIntervalMs = 0L,
            workerLeaseDurationMs = -5L,
            healthCheckTimeoutMs = 0L,
            slowRequestThresholdMs = 0L,
            gracefulShutdownTimeoutMs = -100L
        )
        val errors = config.validate()
        assertEquals(7, errors.size)
        assertTrue(errors.any { it.contains("Database pool size") })
        assertTrue(errors.any { it.contains("Worker pool size") })
        assertTrue(errors.any { it.contains("Worker poll interval") })
        assertTrue(errors.any { it.contains("Worker lease duration") })
        assertTrue(errors.any { it.contains("Health check timeout") })
        assertTrue(errors.any { it.contains("Slow request threshold") })
        assertTrue(errors.any { it.contains("Graceful shutdown timeout") })
    }

    @Test
    fun testProductionRequiresPasswordAndStrongJwtSecret() {
        val prodConfigMissingCreds = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "",
            jwtSigningSecret = "dev_secret"
        )
        val errors = prodConfigMissingCreds.validate()
        assertTrue(errors.any { it.contains("DATABASE_PASSWORD is required in PRODUCTION") })
        assertTrue(errors.any { it.contains("JWT_SIGNING_SECRET must be at least 32 characters") })
    }

    @Test
    fun testProductionRejectsLocalhostDatabaseUrlWithoutOverride() {
        val prodConfigLocalDb = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "strong_production_password_123",
            jwtSigningSecret = "a_very_secure_cryptographic_key_at_least_32_characters_long",
            databaseUrl = "jdbc:postgresql://localhost:5432/sucharu_pro"
        )
        val errors = prodConfigLocalDb.validate()
        assertTrue(errors.any { it.contains("DATABASE_URL cannot point to localhost in PRODUCTION") })
    }

    @Test
    fun testProductionRejectsMissingRedisUrlWhenRedisEnabled() {
        val prodConfigRedis = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "strong_production_password_123",
            jwtSigningSecret = "a_very_secure_cryptographic_key_at_least_32_characters_long",
            databaseUrl = "jdbc:postgresql://postgres-cluster.internal:5432/sucharu_pro",
            redisEnabled = true,
            redisUrl = null
        )
        val errors = prodConfigRedis.validate()
        assertTrue(errors.any { it.contains("REDIS_URL must be specified when REDIS_ENABLED=true in PRODUCTION") })
    }

    @Test
    fun testProductionValidConfigurationPasses() {
        val prodConfigValid = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "strong_production_password_123",
            jwtSigningSecret = "a_very_secure_cryptographic_key_at_least_32_characters_long",
            databaseUrl = "jdbc:postgresql://postgres-cluster.internal:5432/sucharu_pro",
            redisEnabled = true,
            redisUrl = "redis://:redis_secret_password@redis:6379/0"
        )
        val errors = prodConfigValid.validate()
        assertTrue("Valid production configuration must pass validation without errors", errors.isEmpty())
    }

    @Test
    fun testToSafeStringMasksAllSensitiveSecrets() {
        val config = BackendConfig(
            databasePassword = "SuperSecretPassword123!",
            jwtSigningSecret = "SuperSecretJwtSigningKeyMustBeMasked999!"
        )
        val safeStr = config.toSafeString()
        assertFalse("Database password must never appear in safe string", safeStr.contains("SuperSecretPassword123!"))
        assertFalse("JWT secret must never appear in safe string", safeStr.contains("SuperSecretJwtSigningKeyMustBeMasked999!"))
        assertTrue("Should contain REDACTED marker", safeStr.contains("[REDACTED]"))
    }

    @Test
    fun testEnvironmentParsing() {
        assertEquals(BackendEnvironment.PRODUCTION, BackendEnvironment.fromString("production"))
        assertEquals(BackendEnvironment.PRODUCTION, BackendEnvironment.fromString("PROD"))
        assertEquals(BackendEnvironment.TEST, BackendEnvironment.fromString("test"))
        assertEquals(BackendEnvironment.TEST, BackendEnvironment.fromString("STAGING"))
        assertEquals(BackendEnvironment.DEVELOPMENT, BackendEnvironment.fromString("dev"))
        assertEquals(BackendEnvironment.DEVELOPMENT, BackendEnvironment.fromString(null))
    }
}
