package com.sucharu.sucharupro.backend.persistence

import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.MigrationMode
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import java.io.BufferedReader
import java.io.InputStreamReader

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
            val flyway = Flyway.configure(FlywayMigrationManager::class.java.classLoader)
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .configuration(mapOf("flyway.validateMigrationNaming" to "false"))
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

                    if (result.migrationsExecuted == 0 && !isSchemaInitialized()) {
                        logger.warn("EMERGENCY FALLBACK TRIGGERED: Native Flyway scanner returned 0 migrations on an uninitialized schema. Executing fallback direct SQL schema migrations...")
                        executeDirectMigrations()
                    } else {
                        logger.info("Native Flyway migration engine operated normally ({} migrations executed/validated). Emergency direct migration fallback was NOT triggered.", result.migrationsExecuted)
                    }
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

    private fun isSchemaInitialized(): Boolean {
        return try {
            dataSource.connection.use { conn ->
                val rs = conn.metaData.getTables(null, "public", "auth_accounts", arrayOf("TABLE"))
                val exists = rs.next()
                rs.close()
                exists
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun executeDirectMigrations() {
        val migrationFiles = listOf(
            "V20260801__canonical_postgresql_schema.sql",
            "V20260824__add_missing_indexes_and_constraints.sql",
            "V20260830__create_auth_and_session_tables.sql",
            "V20260901__user_identity_lifecycle_and_verification_tables.sql",
            "V20260905__create_persistent_event_store_and_outbox.sql",
            "V20260906__create_integration_delivery_records.sql",
            "V20260907__create_background_job_execution_tables.sql",
            "V20260908__create_workflow_orchestration_and_approval_tables.sql",
            "V20260910__notification_security.sql",
            "V20260911__ai_agent_notification_boundary.sql",
            "V20260912__observability_and_operational_readiness.sql",
            "V20260913__force_row_level_security.sql",
            "V20260914__create_integrations_and_webhooks.sql",
            "V20260915__create_vendor_master.sql",
            "V20260916__create_vendor_profile_and_capabilities.sql",
            "V20260917__create_vendor_service_rates.sql",
            "V20260918__create_vendor_work_orders.sql",
            "V20260919__create_vendor_purchase_orders.sql",
            "V20260920__create_vendor_delivery_receipts.sql",
            "V20260921__create_vendor_invoices_and_3way_matching.sql",
            "V20260922__create_vendor_quality_rejection_disputes.sql",
            "V20260923__create_vendor_performance_evaluation_compliance.sql",
            "V20260924__create_vendor_settlement_analytics_integration.sql",
            "V20260925__create_vendor_portal_foundation_and_secure_access.sql",
            "V20260926__create_vendor_rfq_quotation_bid_management.sql",
            "V20260927__create_vendor_portal_po_work_order_collaboration.sql",
            "V20260928__create_vendor_portal_delivery_receiving_quality.sql",
            "V20260929__create_vendor_portal_invoice_billing_payment_workspace.sql",
            "V20260930__create_vendor_portal_quality_capa_dispute_workspace.sql",
            "V20261001__create_vendor_portal_performance_compliance_workspace.sql",
            "V20261002__vendor_portal_settlement_workspace.sql",
            "V20261003__vendor_portal_analytics_notifications_search.sql",
            "V20261004__vendor_portal_workflow_orchestration.sql",
            "V20261005__create_customer_financial_accounts.sql",
            "V20261006__create_customer_invoices.sql",
            "V20261007__create_customer_payments.sql",
            "V20261013__create_customer_financial_document_delivery.sql",
            "V20261014__create_customer_financial_alerts_and_schedules.sql",
            "V20261015__create_business_expenses.sql",
            "V20261016__create_vendor_payables.sql",
            "V20261017__create_business_ledger_and_cost_allocations.sql",
            "V20261018__create_business_cost_centers_and_tracking.sql",
            "V20261019__create_business_cost_commitments_accruals_and_period_controls.sql",
            "V20261020__create_business_financial_reconciliation.sql",
            "V20261021__create_business_financial_adjustments_refunds_writeoffs.sql",
            "V20261022__create_business_financial_governance_and_budget_control.sql",
            "V20261024__create_profit_and_cost_analysis_foundation.sql",
            "V20261025__create_job_wise_actual_cost_engine.sql",
            "V20261026__create_product_profitability_unit_economics.sql",
            "V20261027__create_customer_profitability_contribution_analysis.sql",
            "V20261028__create_vendor_profitability_supplier_economics.sql",
            "V20261029__create_period_profitability_financial_trends.sql",
            "V20261103__create_printing_quote_tables.sql",
            "V20261104__create_commercial_commitment_conversion.sql",
            "V20261105__create_production_planning_readiness.sql",
            "V20261106__create_production_job_execution.sql",
            "V20261108__create_shop_floor_tracking_tables.sql",
            "V20261109__create_final_qc_and_packaging_release_tables.sql",
            "V20261110__create_production_job_costing_variance_tables.sql",
            "V20261111__create_production_job_closure_governance_tables.sql",
            "V20261112__create_substrate_stock_reservation_tables.sql",
            "V20261113__extend_substrate_reservations_soft_hard_allocation.sql",
            "V20261114__create_imposition_layout_tables.sql",
            "V20261115__create_gang_run_batch_tables.sql",
            "V20261116__create_dynamic_nesting_tables.sql",
            "V20261117__create_signature_imposition_tables.sql",
            "V20261118__create_ctp_prepress_output_tables.sql",
            "V20261119__create_imposition_final_orchestration_tables.sql",
            "V20261120__create_substrate_batch_lot_selection_tables.sql",
            "V20261121__create_substrate_replenishment_tables.sql",
            "V20261122__create_substrate_release_governance_tables.sql",
            "V20261123__create_substrate_enterprise_audit_and_ai_handoff_tables.sql",
            "V20261124__create_affiliate_management_foundation_tables.sql",
            "V20261125__create_affiliate_program_and_enrollment_tables.sql",
            "V20261126__create_affiliate_profile_and_verification_tables.sql",
            "V20261127__create_affiliate_communication_and_notification_tables.sql",
            "V20261128__create_affiliate_command_center_and_governance_work_item_tables.sql",
            "V20261129__create_affiliate_governance_integrity_and_readiness_tables.sql",
            "V20261130__create_finished_product_inventory_integration.sql"
        )

        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                // Ensure flyway_schema_history table exists
                conn.createStatement().use { stmt ->
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS flyway_schema_history (
                            installed_rank INT NOT NULL,
                            version VARCHAR(50),
                            description VARCHAR(200) NOT NULL,
                            type VARCHAR(20) NOT NULL,
                            script VARCHAR(1000) NOT NULL,
                            checksum INTEGER,
                            installed_by VARCHAR(100) NOT NULL,
                            installed_on TIMESTAMP NOT NULL DEFAULT now(),
                            execution_time INTEGER NOT NULL,
                            success BOOLEAN NOT NULL,
                            CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
                        );
                    """.trimIndent())
                }
                conn.commit()

                var rank = 1
                for (fileName in migrationFiles) {
                    val stream = javaClass.classLoader.getResourceAsStream("db/migration/$fileName")
                    if (stream != null) {
                        val sql = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                        if (sql.isNotBlank()) {
                            conn.createStatement().use { stmt ->
                                stmt.execute(sql)
                            }

                            val version = fileName.substringBefore("__").removePrefix("V")
                            val description = fileName.substringAfter("__").removeSuffix(".sql")

                            val crc32 = java.util.zip.CRC32()
                            crc32.update(sql.toByteArray(Charsets.UTF_8))
                            val checksum = crc32.value.toInt()

                            val insertStmt = conn.prepareStatement("""
                                INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
                                VALUES (?, ?, ?, 'SQL', ?, ?, 'sucharu_app', 1, true)
                                ON CONFLICT (installed_rank) DO NOTHING;
                            """.trimIndent())
                            insertStmt.setInt(1, rank)
                            insertStmt.setString(2, version)
                            insertStmt.setString(3, description)
                            insertStmt.setString(4, fileName)
                            insertStmt.setInt(5, checksum)
                            insertStmt.executeUpdate()
                            insertStmt.close()

                            conn.commit()
                            logger.info("Executed direct migration [rank={}]: {}", rank, fileName)
                            rank++
                        }
                    } else {
                        logger.warn("Migration resource not found on classpath: db/migration/{}", fileName)
                    }
                }
            } catch (e: Exception) {
                conn.rollback()
                logger.error("Direct migration failed: {}", e.message, e)
                throw e
            }
        }
    }
}
