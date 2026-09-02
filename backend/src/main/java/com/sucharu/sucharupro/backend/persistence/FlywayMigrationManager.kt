package com.sucharu.sucharupro.backend.persistence

import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.MigrationMode
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Server-owned Flyway migration orchestrator.
 * Ensures database schema is up to date and validated at backend startup.
 */
class FlywayMigrationManager(
    private val dataSource: DataSource,
    private val config: BackendConfig
) {

    private val logger = LoggerFactory.getLogger(FlywayMigrationManager::class.java)

    fun runMigrations(): Boolean {
        if (config.migrationMode == MigrationMode.DISABLED) {
            logger.info("Flyway database migrations are DISABLED by configuration.")
            return true
        }

        return try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load()

            when (config.migrationMode) {
                MigrationMode.AUTO_APPLY -> {
                    logger.info("Executing Flyway AUTO_APPLY schema migrations...")
                    val result: MigrateResult = flyway.migrate()
                    logger.info(
                        "Flyway migration completed: {} migrations executed (target version: {})",
                        result.migrationsExecuted,
                        result.targetSchemaVersion ?: "baseline"
                    )
                    true
                }
                MigrationMode.VALIDATE_ONLY -> {
                    logger.info("Executing Flyway VALIDATE_ONLY schema validation...")
                    flyway.validate()
                    logger.info("Flyway schema validation passed successfully.")
                    true
                }
                MigrationMode.DISABLED -> true
            }
        } catch (e: Exception) {
            logger.error("Flyway schema migration / validation failed: {}", e.message, e)
            false
        }
    }
}
