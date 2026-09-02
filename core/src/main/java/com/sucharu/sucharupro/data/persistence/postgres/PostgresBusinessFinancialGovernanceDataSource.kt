package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.BusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.BusinessFinancialBudgetFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAlertFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAuditFilter
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * Production PostgreSQL JDBC Data Source for Business Financial Governance & Budget Control.
 */
class PostgresBusinessFinancialGovernanceDataSource(
    private val transactionManager: TransactionManager
) : BusinessFinancialGovernanceDataSource {

    // =========================================================================
    // 1. BUDGETS
    // =========================================================================

    override suspend fun saveBudget(budget: BusinessFinancialBudget): BusinessFinancialBudget {
        return transactionManager.inTransaction(TenantContext(budget.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_budgets (
                    id, tenant_id, project_id, budget_name, period_id, dimension_type,
                    dimension_id, allocated_amount, currency, status, version,
                    effective_start_date, effective_end_date, description, created_by,
                    reviewed_by, approved_by, approved_at, rejection_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, budget.id)
                ps.setString(2, budget.tenantId)
                ps.setString(3, budget.projectId)
                ps.setString(4, budget.budgetName)
                ps.setString(5, budget.periodId)
                ps.setString(6, budget.dimensionType.name)
                ps.setString(7, budget.dimensionId)
                ps.setBigDecimal(8, budget.allocatedAmount)
                ps.setString(9, budget.currency)
                ps.setString(10, budget.status.name)
                ps.setLong(11, budget.version)
                ps.setLong(12, budget.effectiveStartDate)
                ps.setLong(13, budget.effectiveEndDate)
                ps.setString(14, budget.description)
                ps.setString(15, budget.createdBy)
                ps.setString(16, budget.reviewedBy)
                ps.setString(17, budget.approvedBy)
                if (budget.approvedAt != null) ps.setLong(18, budget.approvedAt) else ps.setNull(18, java.sql.Types.BIGINT)
                ps.setString(19, budget.rejectionReason)
                ps.setLong(20, budget.createdAt)
                ps.setLong(21, budget.updatedAt)
                ps.executeUpdate()
            }
            budget
        }
    }

    override suspend fun updateBudget(budget: BusinessFinancialBudget): BusinessFinancialBudget {
        return transactionManager.inTransaction(TenantContext(budget.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_budgets SET
                    budget_name = ?, allocated_amount = ?, currency = ?, status = ?, version = ?,
                    effective_start_date = ?, effective_end_date = ?, description = ?,
                    reviewed_by = ?, approved_by = ?, approved_at = ?, rejection_reason = ?,
                    updated_at = ?
                WHERE tenant_id = ? AND project_id = ? AND id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, budget.budgetName)
                ps.setBigDecimal(2, budget.allocatedAmount)
                ps.setString(3, budget.currency)
                ps.setString(4, budget.status.name)
                ps.setLong(5, budget.version)
                ps.setLong(6, budget.effectiveStartDate)
                ps.setLong(7, budget.effectiveEndDate)
                ps.setString(8, budget.description)
                ps.setString(9, budget.reviewedBy)
                ps.setString(10, budget.approvedBy)
                if (budget.approvedAt != null) ps.setLong(11, budget.approvedAt) else ps.setNull(11, java.sql.Types.BIGINT)
                ps.setString(12, budget.rejectionReason)
                ps.setLong(13, budget.updatedAt)
                ps.setString(14, budget.tenantId)
                ps.setString(15, budget.projectId)
                ps.setString(16, budget.id)
                ps.executeUpdate()
            }
            budget
        }
    }

    override suspend fun findBudgetById(tenantId: String, projectId: String, budgetId: String): BusinessFinancialBudget? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_budgets
                WHERE tenant_id = ? AND project_id = ? AND id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, budgetId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapBudget(rs) else null
                }
            }
        }
    }

    override suspend fun findBudgetByDimension(
        tenantId: String,
        projectId: String,
        periodId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): BusinessFinancialBudget? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_budgets
                WHERE tenant_id = ? AND project_id = ? AND period_id = ?
                  AND dimension_type = ? AND dimension_id = ?
                ORDER BY version DESC LIMIT 1
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, periodId)
                ps.setString(4, dimensionType.name)
                ps.setString(5, dimensionId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapBudget(rs) else null
                }
            }
        }
    }

    override suspend fun listBudgets(tenantId: String, projectId: String, filter: BusinessFinancialBudgetFilter): List<BusinessFinancialBudget> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.dimensionType != null) {
                conditions.add("dimension_type = ?")
                params.add(filter.dimensionType.name)
            }
            if (filter.dimensionId != null) {
                conditions.add("dimension_id = ?")
                params.add(filter.dimensionId)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }
            if (filter.currency != null) {
                conditions.add("currency = ?")
                params.add(filter.currency)
            }

            val sql = """
                SELECT * FROM business_financial_budgets
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY created_at DESC
                LIMIT ${filter.limit} OFFSET ${filter.offset}
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is String -> ps.setString(i + 1, p)
                        is Long -> ps.setLong(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialBudget>()
                    while (rs.next()) {
                        list.add(mapBudget(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun deleteDraftBudget(tenantId: String, projectId: String, budgetId: String): Boolean {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                DELETE FROM business_financial_budgets
                WHERE tenant_id = ? AND project_id = ? AND id = ? AND status = 'DRAFT'
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, budgetId)
                ps.executeUpdate() > 0
            }
        }
    }

    // =========================================================================
    // 2. BUDGET REVISIONS
    // =========================================================================

    override suspend fun saveBudgetRevision(revision: BusinessFinancialBudgetRevision): BusinessFinancialBudgetRevision {
        return transactionManager.inTransaction(TenantContext(revision.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_budget_revisions (
                    id, budget_id, tenant_id, project_id, version,
                    previous_allocated_amount, new_allocated_amount, revision_reason,
                    revised_by, approved_by, revised_at, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, revision.id)
                ps.setString(2, revision.budgetId)
                ps.setString(3, revision.tenantId)
                ps.setString(4, revision.projectId)
                ps.setLong(5, revision.version)
                ps.setBigDecimal(6, revision.previousAllocatedAmount)
                ps.setBigDecimal(7, revision.newAllocatedAmount)
                ps.setString(8, revision.revisionReason)
                ps.setString(9, revision.revisedBy)
                ps.setString(10, revision.approvedBy)
                ps.setLong(11, revision.revisedAt)
                ps.setString(12, revision.status)
                ps.executeUpdate()
            }
            revision
        }
    }

    override suspend fun listBudgetRevisions(tenantId: String, projectId: String, budgetId: String): List<BusinessFinancialBudgetRevision> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_budget_revisions
                WHERE tenant_id = ? AND project_id = ? AND budget_id = ?
                ORDER BY revised_at DESC
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, budgetId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialBudgetRevision>()
                    while (rs.next()) {
                        list.add(mapRevision(rs))
                    }
                    list
                }
            }
        }
    }

    // =========================================================================
    // 3. THRESHOLDS
    // =========================================================================

    override suspend fun saveThreshold(threshold: BusinessFinancialBudgetThreshold): BusinessFinancialBudgetThreshold {
        return transactionManager.inTransaction(TenantContext(threshold.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_budget_thresholds (
                    id, tenant_id, project_id, threshold_name, dimension_type,
                    dimension_id, warning_utilization_pct, critical_utilization_pct,
                    large_expense_threshold_amount, commitment_exposure_threshold_pct,
                    is_active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, project_id, dimension_type, dimension_id)
                DO UPDATE SET
                    threshold_name = EXCLUDED.threshold_name,
                    warning_utilization_pct = EXCLUDED.warning_utilization_pct,
                    critical_utilization_pct = EXCLUDED.critical_utilization_pct,
                    large_expense_threshold_amount = EXCLUDED.large_expense_threshold_amount,
                    commitment_exposure_threshold_pct = EXCLUDED.commitment_exposure_threshold_pct,
                    is_active = EXCLUDED.is_active,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, threshold.id)
                ps.setString(2, threshold.tenantId)
                ps.setString(3, threshold.projectId)
                ps.setString(4, threshold.thresholdName)
                ps.setString(5, threshold.dimensionType.name)
                ps.setString(6, threshold.dimensionId)
                ps.setBigDecimal(7, threshold.warningUtilizationPct)
                ps.setBigDecimal(8, threshold.criticalUtilizationPct)
                ps.setBigDecimal(9, threshold.largeExpenseThresholdAmount)
                ps.setBigDecimal(10, threshold.commitmentExposureThresholdPct)
                ps.setBoolean(11, threshold.isActive)
                ps.setLong(12, threshold.createdAt)
                ps.setLong(13, threshold.updatedAt)
                ps.executeUpdate()
            }
            threshold
        }
    }

    override suspend fun findThreshold(
        tenantId: String,
        projectId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): BusinessFinancialBudgetThreshold? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_budget_thresholds
                WHERE tenant_id = ? AND project_id = ? AND dimension_type = ? AND dimension_id = ? AND is_active = TRUE
                UNION ALL
                SELECT * FROM business_financial_budget_thresholds
                WHERE tenant_id = ? AND project_id = ? AND dimension_type = 'OVERALL_BUSINESS' AND dimension_id = 'ALL' AND is_active = TRUE
                LIMIT 1
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, dimensionType.name)
                ps.setString(4, dimensionId)
                ps.setString(5, tenantId)
                ps.setString(6, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapThreshold(rs) else null
                }
            }
        }
    }

    override suspend fun listThresholds(tenantId: String, projectId: String): List<BusinessFinancialBudgetThreshold> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_budget_thresholds
                WHERE tenant_id = ? AND project_id = ?
                ORDER BY threshold_name
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialBudgetThreshold>()
                    while (rs.next()) {
                        list.add(mapThreshold(rs))
                    }
                    list
                }
            }
        }
    }

    // =========================================================================
    // 4. FORECASTS
    // =========================================================================

    override suspend fun saveForecast(forecast: BusinessFinancialForecast): BusinessFinancialForecast {
        return transactionManager.inTransaction(TenantContext(forecast.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_forecasts (
                    id, tenant_id, project_id, forecast_name, period_id, dimension_type,
                    dimension_id, currency, actual_ytd_amount, projected_remaining_amount,
                    forecast_total_amount, run_rate_per_day, generated_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, forecast.id)
                ps.setString(2, forecast.tenantId)
                ps.setString(3, forecast.projectId)
                ps.setString(4, forecast.forecastName)
                ps.setString(5, forecast.periodId)
                ps.setString(6, forecast.dimensionType.name)
                ps.setString(7, forecast.dimensionId)
                ps.setString(8, forecast.currency)
                ps.setBigDecimal(9, forecast.actualYtdAmount)
                ps.setBigDecimal(10, forecast.projectedRemainingAmount)
                ps.setBigDecimal(11, forecast.forecastTotalAmount)
                ps.setBigDecimal(12, forecast.runRatePerDay)
                ps.setLong(13, forecast.generatedAt)
                ps.setString(14, forecast.createdBy)
                ps.executeUpdate()
            }
            forecast
        }
    }

    override suspend fun findForecastById(tenantId: String, projectId: String, forecastId: String): BusinessFinancialForecast? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_forecasts
                WHERE tenant_id = ? AND project_id = ? AND id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, forecastId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapForecast(rs) else null
                }
            }
        }
    }

    override suspend fun listForecasts(tenantId: String, projectId: String, periodId: String?): List<BusinessFinancialForecast> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = if (periodId != null) {
                "SELECT * FROM business_financial_forecasts WHERE tenant_id = ? AND project_id = ? AND period_id = ? ORDER BY generated_at DESC"
            } else {
                "SELECT * FROM business_financial_forecasts WHERE tenant_id = ? AND project_id = ? ORDER BY generated_at DESC"
            }
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                if (periodId != null) ps.setString(3, periodId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialForecast>()
                    while (rs.next()) {
                        list.add(mapForecast(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveForecastScenario(scenario: BusinessFinancialForecastScenario): BusinessFinancialForecastScenario {
        return transactionManager.inTransaction(TenantContext(scenario.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_forecast_scenarios (
                    id, forecast_id, tenant_id, project_id, scenario_type,
                    projected_amount, variance_vs_budget, assumptions_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, forecast_id, scenario_type)
                DO UPDATE SET
                    projected_amount = EXCLUDED.projected_amount,
                    variance_vs_budget = EXCLUDED.variance_vs_budget,
                    assumptions_json = EXCLUDED.assumptions_json
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, scenario.id)
                ps.setString(2, scenario.forecastId)
                ps.setString(3, scenario.tenantId)
                ps.setString(4, scenario.projectId)
                ps.setString(5, scenario.scenarioType.name)
                ps.setBigDecimal(6, scenario.projectedAmount)
                ps.setBigDecimal(7, scenario.varianceVsBudget)
                ps.setString(8, scenario.assumptionsJson)
                ps.setLong(9, scenario.createdAt)
                ps.executeUpdate()
            }
            scenario
        }
    }

    override suspend fun listForecastScenarios(tenantId: String, projectId: String, forecastId: String): List<BusinessFinancialForecastScenario> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_forecast_scenarios
                WHERE tenant_id = ? AND project_id = ? AND forecast_id = ?
                ORDER BY created_at
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, forecastId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialForecastScenario>()
                    while (rs.next()) {
                        list.add(mapScenario(rs))
                    }
                    list
                }
            }
        }
    }

    // =========================================================================
    // 5. ALERTS
    // =========================================================================

    override suspend fun saveAlert(alert: BusinessFinancialGovernanceAlert): BusinessFinancialGovernanceAlert {
        return transactionManager.inTransaction(TenantContext(alert.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_governance_alerts (
                    id, tenant_id, project_id, alert_type, severity,
                    source_dimension_type, source_dimension_id, message,
                    threshold_value, current_value, status, acknowledged_by,
                    acknowledged_at, acknowledgement_notes, resolved_by,
                    resolved_at, resolution_notes, dismissal_reason,
                    period_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, alert.id)
                ps.setString(2, alert.tenantId)
                ps.setString(3, alert.projectId)
                ps.setString(4, alert.alertType.name)
                ps.setString(5, alert.severity.name)
                ps.setString(6, alert.sourceDimensionType.name)
                ps.setString(7, alert.sourceDimensionId)
                ps.setString(8, alert.message)
                ps.setBigDecimal(9, alert.thresholdValue)
                ps.setBigDecimal(10, alert.currentValue)
                ps.setString(11, alert.status.name)
                ps.setString(12, alert.acknowledgedBy)
                if (alert.acknowledgedAt != null) ps.setLong(13, alert.acknowledgedAt) else ps.setNull(13, java.sql.Types.BIGINT)
                ps.setString(14, alert.acknowledgementNotes)
                ps.setString(15, alert.resolvedBy)
                if (alert.resolvedAt != null) ps.setLong(16, alert.resolvedAt) else ps.setNull(16, java.sql.Types.BIGINT)
                ps.setString(17, alert.resolutionNotes)
                ps.setString(18, alert.dismissalReason)
                ps.setString(19, alert.periodId)
                ps.setLong(20, alert.createdAt)
                ps.setLong(21, alert.updatedAt)
                ps.executeUpdate()
            }
            alert
        }
    }

    override suspend fun updateAlert(alert: BusinessFinancialGovernanceAlert): BusinessFinancialGovernanceAlert {
        return transactionManager.inTransaction(TenantContext(alert.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_governance_alerts SET
                    status = ?, acknowledged_by = ?, acknowledged_at = ?, acknowledgement_notes = ?,
                    resolved_by = ?, resolved_at = ?, resolution_notes = ?, dismissal_reason = ?,
                    updated_at = ?
                WHERE tenant_id = ? AND project_id = ? AND id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, alert.status.name)
                ps.setString(2, alert.acknowledgedBy)
                if (alert.acknowledgedAt != null) ps.setLong(3, alert.acknowledgedAt) else ps.setNull(3, java.sql.Types.BIGINT)
                ps.setString(4, alert.acknowledgementNotes)
                ps.setString(5, alert.resolvedBy)
                if (alert.resolvedAt != null) ps.setLong(6, alert.resolvedAt) else ps.setNull(6, java.sql.Types.BIGINT)
                ps.setString(7, alert.resolutionNotes)
                ps.setString(8, alert.dismissalReason)
                ps.setLong(9, alert.updatedAt)
                ps.setString(10, alert.tenantId)
                ps.setString(11, alert.projectId)
                ps.setString(12, alert.id)
                ps.executeUpdate()
            }
            alert
        }
    }

    override suspend fun findAlertById(tenantId: String, projectId: String, alertId: String): BusinessFinancialGovernanceAlert? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM business_financial_governance_alerts
                WHERE tenant_id = ? AND project_id = ? AND id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, alertId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAlert(rs) else null
                }
            }
        }
    }

    override suspend fun findOpenAlert(
        tenantId: String,
        projectId: String,
        alertType: GovernanceAlertType,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        periodId: String?
    ): BusinessFinancialGovernanceAlert? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = if (periodId != null) {
                """
                    SELECT * FROM business_financial_governance_alerts
                    WHERE tenant_id = ? AND project_id = ? AND alert_type = ?
                      AND source_dimension_type = ? AND source_dimension_id = ?
                      AND status = 'OPEN' AND period_id = ?
                    LIMIT 1
                """.trimIndent()
            } else {
                """
                    SELECT * FROM business_financial_governance_alerts
                    WHERE tenant_id = ? AND project_id = ? AND alert_type = ?
                      AND source_dimension_type = ? AND source_dimension_id = ?
                      AND status = 'OPEN'
                    LIMIT 1
                """.trimIndent()
            }
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, alertType.name)
                ps.setString(4, dimensionType.name)
                ps.setString(5, dimensionId)
                if (periodId != null) ps.setString(6, periodId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAlert(rs) else null
                }
            }
        }
    }

    override suspend fun listAlerts(tenantId: String, projectId: String, filter: GovernanceAlertFilter): List<BusinessFinancialGovernanceAlert> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }
            if (filter.severity != null) {
                conditions.add("severity = ?")
                params.add(filter.severity.name)
            }
            if (filter.alertType != null) {
                conditions.add("alert_type = ?")
                params.add(filter.alertType.name)
            }
            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.dimensionType != null) {
                conditions.add("source_dimension_type = ?")
                params.add(filter.dimensionType.name)
            }
            if (filter.dimensionId != null) {
                conditions.add("source_dimension_id = ?")
                params.add(filter.dimensionId)
            }

            val sql = """
                SELECT * FROM business_financial_governance_alerts
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY created_at DESC
                LIMIT ${filter.limit} OFFSET ${filter.offset}
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is String -> ps.setString(i + 1, p)
                        is Long -> ps.setLong(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialGovernanceAlert>()
                    while (rs.next()) {
                        list.add(mapAlert(rs))
                    }
                    list
                }
            }
        }
    }

    // =========================================================================
    // 6. AUDIT TRAIL
    // =========================================================================

    override suspend fun saveAuditEvent(event: BusinessFinancialGovernanceAuditEvent): BusinessFinancialGovernanceAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_governance_audit_events (
                    id, tenant_id, project_id, actor_id, actor_role,
                    event_type, outcome, target_id, target_type, timestamp,
                    details_json, client_ip, correlation_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.id)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.actorId)
                ps.setString(5, event.actorRole)
                ps.setString(6, event.eventType)
                ps.setString(7, event.outcome)
                ps.setString(8, event.targetId)
                ps.setString(9, event.targetType)
                ps.setLong(10, event.timestamp)
                ps.setString(11, event.detailsJson)
                ps.setString(12, event.clientIp)
                ps.setString(13, event.correlationId)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, filter: GovernanceAuditFilter): List<BusinessFinancialGovernanceAuditEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.targetId != null) {
                conditions.add("target_id = ?")
                params.add(filter.targetId)
            }
            if (filter.eventType != null) {
                conditions.add("event_type = ?")
                params.add(filter.eventType)
            }
            if (filter.actorId != null) {
                conditions.add("actor_id = ?")
                params.add(filter.actorId)
            }
            if (filter.fromTimestamp != null) {
                conditions.add("timestamp >= ?")
                params.add(filter.fromTimestamp)
            }
            if (filter.toTimestamp != null) {
                conditions.add("timestamp <= ?")
                params.add(filter.toTimestamp)
            }

            val sql = """
                SELECT * FROM business_financial_governance_audit_events
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY timestamp DESC
                LIMIT ${filter.limit} OFFSET ${filter.offset}
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is String -> ps.setString(i + 1, p)
                        is Long -> ps.setLong(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialGovernanceAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    list
                }
            }
        }
    }

    // =========================================================================
    // MAPPING HELPERS
    // =========================================================================

    private fun mapBudget(rs: ResultSet): BusinessFinancialBudget {
        return BusinessFinancialBudget(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            budgetName = rs.getString("budget_name"),
            periodId = rs.getString("period_id"),
            dimensionType = BusinessFinancialBudgetDimensionType.valueOf(rs.getString("dimension_type")),
            dimensionId = rs.getString("dimension_id"),
            allocatedAmount = rs.getBigDecimal("allocated_amount").setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            status = BusinessFinancialBudgetStatus.valueOf(rs.getString("status")),
            version = rs.getLong("version"),
            effectiveStartDate = rs.getLong("effective_start_date"),
            effectiveEndDate = rs.getLong("effective_end_date"),
            description = rs.getString("description"),
            createdBy = rs.getString("created_by"),
            reviewedBy = rs.getString("reviewed_by"),
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getLong("approved_at").takeIf { !rs.wasNull() },
            rejectionReason = rs.getString("rejection_reason"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapRevision(rs: ResultSet): BusinessFinancialBudgetRevision {
        return BusinessFinancialBudgetRevision(
            id = rs.getString("id"),
            budgetId = rs.getString("budget_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            version = rs.getLong("version"),
            previousAllocatedAmount = rs.getBigDecimal("previous_allocated_amount").setScale(4, RoundingMode.HALF_UP),
            newAllocatedAmount = rs.getBigDecimal("new_allocated_amount").setScale(4, RoundingMode.HALF_UP),
            revisionReason = rs.getString("revision_reason"),
            revisedBy = rs.getString("revised_by"),
            approvedBy = rs.getString("approved_by"),
            revisedAt = rs.getLong("revised_at"),
            status = rs.getString("status")
        )
    }

    private fun mapThreshold(rs: ResultSet): BusinessFinancialBudgetThreshold {
        return BusinessFinancialBudgetThreshold(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            thresholdName = rs.getString("threshold_name"),
            dimensionType = BusinessFinancialBudgetDimensionType.valueOf(rs.getString("dimension_type")),
            dimensionId = rs.getString("dimension_id"),
            warningUtilizationPct = rs.getBigDecimal("warning_utilization_pct").setScale(4, RoundingMode.HALF_UP),
            criticalUtilizationPct = rs.getBigDecimal("critical_utilization_pct").setScale(4, RoundingMode.HALF_UP),
            largeExpenseThresholdAmount = rs.getBigDecimal("large_expense_threshold_amount").setScale(4, RoundingMode.HALF_UP),
            commitmentExposureThresholdPct = rs.getBigDecimal("commitment_exposure_threshold_pct").setScale(4, RoundingMode.HALF_UP),
            isActive = rs.getBoolean("is_active"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapForecast(rs: ResultSet): BusinessFinancialForecast {
        return BusinessFinancialForecast(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            forecastName = rs.getString("forecast_name"),
            periodId = rs.getString("period_id"),
            dimensionType = BusinessFinancialBudgetDimensionType.valueOf(rs.getString("dimension_type")),
            dimensionId = rs.getString("dimension_id"),
            currency = rs.getString("currency"),
            actualYtdAmount = rs.getBigDecimal("actual_ytd_amount").setScale(4, RoundingMode.HALF_UP),
            projectedRemainingAmount = rs.getBigDecimal("projected_remaining_amount").setScale(4, RoundingMode.HALF_UP),
            forecastTotalAmount = rs.getBigDecimal("forecast_total_amount").setScale(4, RoundingMode.HALF_UP),
            runRatePerDay = rs.getBigDecimal("run_rate_per_day").setScale(4, RoundingMode.HALF_UP),
            generatedAt = rs.getLong("generated_at"),
            createdBy = rs.getString("created_by")
        )
    }

    private fun mapScenario(rs: ResultSet): BusinessFinancialForecastScenario {
        return BusinessFinancialForecastScenario(
            id = rs.getString("id"),
            forecastId = rs.getString("forecast_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            scenarioType = ForecastScenarioType.valueOf(rs.getString("scenario_type")),
            projectedAmount = rs.getBigDecimal("projected_amount").setScale(4, RoundingMode.HALF_UP),
            varianceVsBudget = rs.getBigDecimal("variance_vs_budget").setScale(4, RoundingMode.HALF_UP),
            assumptionsJson = rs.getString("assumptions_json"),
            createdAt = rs.getLong("created_at")
        )
    }

    private fun mapAlert(rs: ResultSet): BusinessFinancialGovernanceAlert {
        return BusinessFinancialGovernanceAlert(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            alertType = GovernanceAlertType.valueOf(rs.getString("alert_type")),
            severity = GovernanceAlertSeverity.valueOf(rs.getString("severity")),
            sourceDimensionType = BusinessFinancialBudgetDimensionType.valueOf(rs.getString("source_dimension_type")),
            sourceDimensionId = rs.getString("source_dimension_id"),
            message = rs.getString("message"),
            thresholdValue = rs.getBigDecimal("threshold_value").setScale(4, RoundingMode.HALF_UP),
            currentValue = rs.getBigDecimal("current_value").setScale(4, RoundingMode.HALF_UP),
            status = GovernanceAlertStatus.valueOf(rs.getString("status")),
            acknowledgedBy = rs.getString("acknowledged_by"),
            acknowledgedAt = rs.getLong("acknowledged_at").takeIf { !rs.wasNull() },
            acknowledgementNotes = rs.getString("acknowledgement_notes"),
            resolvedBy = rs.getString("resolved_by"),
            resolvedAt = rs.getLong("resolved_at").takeIf { !rs.wasNull() },
            resolutionNotes = rs.getString("resolution_notes"),
            dismissalReason = rs.getString("dismissal_reason"),
            periodId = rs.getString("period_id"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): BusinessFinancialGovernanceAuditEvent {
        return BusinessFinancialGovernanceAuditEvent(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            eventType = rs.getString("event_type"),
            outcome = rs.getString("outcome"),
            targetId = rs.getString("target_id"),
            targetType = rs.getString("target_type"),
            timestamp = rs.getLong("timestamp"),
            detailsJson = rs.getString("details_json"),
            clientIp = rs.getString("client_ip"),
            correlationId = rs.getString("correlation_id")
        )
    }
}
