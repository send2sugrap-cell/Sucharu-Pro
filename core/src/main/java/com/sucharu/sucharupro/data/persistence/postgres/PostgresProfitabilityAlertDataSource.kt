package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityAlertDataSource
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of ProfitabilityAlertDataSource with TransactionManager & RLS.
 * Module 16 Step 09.
 */
class PostgresProfitabilityAlertDataSource(
    private val transactionManager: TransactionManager
) : ProfitabilityAlertDataSource {

    override suspend fun saveAlert(alert: ProfitabilityAlert) {
        transactionManager.inTransaction(TenantContext(alert.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_alerts (
                    alert_id, tenant_id, project_id, alert_type, severity, status,
                    dimension_type, dimension_id, dimension_label, period_id, source_module,
                    source_step, source_entity_type, source_entity_id, trigger_metric,
                    observed_value, threshold_value, direction, financial_impact,
                    detected_at, first_detected_at, last_detected_at, occurrence_count,
                    fingerprint, integrity_hash, explanation, recommended_action_code,
                    is_recurring, rule_id, acknowledged_at, acknowledged_by,
                    resolved_at, resolved_by, resolution_notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (alert_id) DO UPDATE SET
                    observed_value = EXCLUDED.observed_value,
                    financial_impact = EXCLUDED.financial_impact,
                    last_detected_at = EXCLUDED.last_detected_at,
                    occurrence_count = EXCLUDED.occurrence_count,
                    is_recurring = EXCLUDED.is_recurring,
                    integrity_hash = EXCLUDED.integrity_hash
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, alert.alertId)
                ps.setString(2, alert.tenantId)
                ps.setString(3, alert.projectId)
                ps.setString(4, alert.alertType.name)
                ps.setString(5, alert.severity.name)
                ps.setString(6, alert.status.name)
                ps.setString(7, alert.dimensionType.name)
                ps.setString(8, alert.dimensionId)
                ps.setString(9, alert.dimensionLabel)
                ps.setString(10, alert.periodId)
                ps.setString(11, alert.sourceModule)
                ps.setString(12, alert.sourceStep)
                ps.setString(13, alert.sourceEntityType)
                ps.setString(14, alert.sourceEntityId)
                ps.setString(15, alert.triggerMetric)
                ps.setBigDecimal(16, alert.observedValue)
                ps.setBigDecimal(17, alert.thresholdValue)
                ps.setString(18, alert.direction.name)
                ps.setBigDecimal(19, alert.financialImpact)
                ps.setLong(20, alert.detectedAt)
                ps.setLong(21, alert.firstDetectedAt)
                ps.setLong(22, alert.lastDetectedAt)
                ps.setInt(23, alert.occurrenceCount)
                ps.setString(24, alert.fingerprint)
                ps.setString(25, alert.integrityHash)
                ps.setString(26, alert.explanation)
                ps.setString(27, alert.recommendedActionCode?.name)
                ps.setBoolean(28, alert.isRecurring)
                ps.setString(29, alert.ruleId)
                if (alert.acknowledgedAt != null) ps.setLong(30, alert.acknowledgedAt) else ps.setNull(30, java.sql.Types.BIGINT)
                ps.setString(31, alert.acknowledgedBy)
                if (alert.resolvedAt != null) ps.setLong(32, alert.resolvedAt) else ps.setNull(32, java.sql.Types.BIGINT)
                ps.setString(33, alert.resolvedBy)
                ps.setString(34, alert.resolutionNotes)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun updateAlert(alert: ProfitabilityAlert) {
        saveAlert(alert)
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): ProfitabilityAlert? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alerts WHERE tenant_id = ? AND alert_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, alertId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAlert(rs) else null
                }
            }
        }
    }

    override suspend fun findAlertByFingerprint(tenantId: String, fingerprint: String): ProfitabilityAlert? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alerts WHERE tenant_id = ? AND fingerprint = ? ORDER BY detected_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, fingerprint)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAlert(rs) else null
                }
            }
        }
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        dimension: ProfitabilityAlertDimension?,
        severity: ProfitabilityAlertSeverity?,
        status: ProfitabilityAlertStatus?,
        isRecurring: Boolean?
    ): List<ProfitabilityAlert> {
        return transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val conn = ctx.connection
            val sql = StringBuilder("SELECT * FROM profitability_alerts WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (dimension != null) {
                sql.append(" AND dimension_type = ?")
                params.add(dimension.name)
            }
            if (severity != null) {
                sql.append(" AND severity = ?")
                params.add(severity.name)
            }
            if (status != null) {
                sql.append(" AND status = ?")
                params.add(status.name)
            }
            if (isRecurring != null) {
                sql.append(" AND is_recurring = ?")
                params.add(isRecurring)
            }
            sql.append(" ORDER BY last_detected_at DESC")

            conn.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is String -> ps.setString(i + 1, p)
                        is Boolean -> ps.setBoolean(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAlert>()
                    while (rs.next()) {
                        list.add(mapAlert(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveOccurrence(occurrence: ProfitabilityAlertOccurrence) {
        transactionManager.inTransaction(TenantContext(occurrence.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_alert_occurrences (
                    occurrence_id, alert_id, tenant_id, detected_at, observed_value,
                    financial_impact, previous_status, trigger_details, source_snapshot_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (occurrence_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, occurrence.occurrenceId)
                ps.setString(2, occurrence.alertId)
                ps.setString(3, occurrence.tenantId)
                ps.setLong(4, occurrence.detectedAt)
                ps.setBigDecimal(5, occurrence.observedValue)
                ps.setBigDecimal(6, occurrence.financialImpact)
                ps.setString(7, occurrence.previousStatus.name)
                ps.setString(8, occurrence.triggerDetails)
                ps.setString(9, occurrence.sourceSnapshotId)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listOccurrences(tenantId: String, alertId: String): List<ProfitabilityAlertOccurrence> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alert_occurrences WHERE tenant_id = ? AND alert_id = ? ORDER BY detected_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, alertId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAlertOccurrence>()
                    while (rs.next()) {
                        list.add(
                            ProfitabilityAlertOccurrence(
                                occurrenceId = rs.getString("occurrence_id"),
                                alertId = rs.getString("alert_id"),
                                tenantId = rs.getString("tenant_id"),
                                detectedAt = rs.getLong("detected_at"),
                                observedValue = rs.getBigDecimal("observed_value").setScale(4, RoundingMode.HALF_UP),
                                financialImpact = rs.getBigDecimal("financial_impact").setScale(4, RoundingMode.HALF_UP),
                                previousStatus = ProfitabilityAlertStatus.valueOf(rs.getString("previous_status")),
                                triggerDetails = rs.getString("trigger_details") ?: "",
                                sourceSnapshotId = rs.getString("source_snapshot_id")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveRule(rule: ProfitabilityAlertRule) {
        transactionManager.inTransaction(TenantContext(rule.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_alert_rules (
                    rule_id, tenant_id, project_id, rule_name, alert_type, dimension_type,
                    threshold_metric, threshold_value, comparison_operator, severity,
                    enabled, description, effective_from, effective_to, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (rule_id) DO UPDATE SET
                    rule_name = EXCLUDED.rule_name,
                    threshold_metric = EXCLUDED.threshold_metric,
                    threshold_value = EXCLUDED.threshold_value,
                    comparison_operator = EXCLUDED.comparison_operator,
                    severity = EXCLUDED.severity,
                    enabled = EXCLUDED.enabled,
                    description = EXCLUDED.description,
                    effective_to = EXCLUDED.effective_to,
                    version = EXCLUDED.version + 1,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, rule.ruleId)
                ps.setString(2, rule.tenantId)
                ps.setString(3, rule.projectId)
                ps.setString(4, rule.ruleName)
                ps.setString(5, rule.alertType.name)
                ps.setString(6, rule.dimensionType.name)
                ps.setString(7, rule.thresholdMetric)
                ps.setBigDecimal(8, rule.thresholdValue)
                ps.setString(9, rule.comparisonOperator.name)
                ps.setString(10, rule.severity.name)
                ps.setBoolean(11, rule.enabled)
                ps.setString(12, rule.description)
                ps.setLong(13, rule.effectiveFrom)
                if (rule.effectiveTo != null) ps.setLong(14, rule.effectiveTo) else ps.setNull(14, java.sql.Types.BIGINT)
                ps.setInt(15, rule.version)
                ps.setLong(16, rule.createdAt)
                ps.setLong(17, rule.updatedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun updateRule(rule: ProfitabilityAlertRule) {
        saveRule(rule)
    }

    override suspend fun getRuleById(tenantId: String, ruleId: String): ProfitabilityAlertRule? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alert_rules WHERE tenant_id = ? AND rule_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, ruleId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapRule(rs) else null
                }
            }
        }
    }

    override suspend fun listRules(
        tenantId: String,
        projectId: String,
        dimensionType: ProfitabilityAlertDimension?
    ): List<ProfitabilityAlertRule> {
        return transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val conn = ctx.connection
            val sql = StringBuilder("SELECT * FROM profitability_alert_rules WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<String>(tenantId, projectId)
            if (dimensionType != null) {
                sql.append(" AND dimension_type = ?")
                params.add(dimensionType.name)
            }
            sql.append(" ORDER BY created_at DESC")

            conn.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setString(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAlertRule>()
                    while (rs.next()) {
                        list.add(mapRule(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveAction(action: ProfitabilityManagementAction) {
        transactionManager.inTransaction(TenantContext(action.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_management_actions (
                    action_id, alert_id, tenant_id, project_id, action_code, action_title,
                    action_description, priority_score, status, assigned_to, assigned_by,
                    due_at, started_at, completed_at, verified_at, verified_by,
                    expected_financial_impact, realized_financial_impact, outcome_notes,
                    created_at, updated_at, integrity_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (action_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    assigned_to = EXCLUDED.assigned_to,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at,
                    verified_at = EXCLUDED.verified_at,
                    verified_by = EXCLUDED.verified_by,
                    realized_financial_impact = EXCLUDED.realized_financial_impact,
                    outcome_notes = EXCLUDED.outcome_notes,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, action.actionId)
                ps.setString(2, action.alertId)
                ps.setString(3, action.tenantId)
                ps.setString(4, action.projectId)
                ps.setString(5, action.actionCode.name)
                ps.setString(6, action.actionTitle)
                ps.setString(7, action.actionDescription)
                ps.setBigDecimal(8, action.priorityScore)
                ps.setString(9, action.status.name)
                ps.setString(10, action.assignedTo)
                ps.setString(11, action.assignedBy)
                if (action.dueAt != null) ps.setLong(12, action.dueAt) else ps.setNull(12, java.sql.Types.BIGINT)
                if (action.startedAt != null) ps.setLong(13, action.startedAt) else ps.setNull(13, java.sql.Types.BIGINT)
                if (action.completedAt != null) ps.setLong(14, action.completedAt) else ps.setNull(14, java.sql.Types.BIGINT)
                if (action.verifiedAt != null) ps.setLong(15, action.verifiedAt) else ps.setNull(15, java.sql.Types.BIGINT)
                ps.setString(16, action.verifiedBy)
                ps.setBigDecimal(17, action.expectedFinancialImpact)
                if (action.realizedFinancialImpact != null) ps.setBigDecimal(18, action.realizedFinancialImpact) else ps.setNull(18, java.sql.Types.NUMERIC)
                ps.setString(19, action.outcomeNotes)
                ps.setLong(20, action.createdAt)
                ps.setLong(21, action.updatedAt)
                ps.setString(22, action.integrityHash)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun updateAction(action: ProfitabilityManagementAction) {
        saveAction(action)
    }

    override suspend fun getActionById(tenantId: String, actionId: String): ProfitabilityManagementAction? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_management_actions WHERE tenant_id = ? AND action_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, actionId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAction(rs) else null
                }
            }
        }
    }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        alertId: String?,
        status: ManagementActionStatus?
    ): List<ProfitabilityManagementAction> {
        return transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val conn = ctx.connection
            val sql = StringBuilder("SELECT * FROM profitability_management_actions WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<String>(tenantId, projectId)
            if (alertId != null) {
                sql.append(" AND alert_id = ?")
                params.add(alertId)
            }
            if (status != null) {
                sql.append(" AND status = ?")
                params.add(status.name)
            }
            sql.append(" ORDER BY priority_score DESC")

            conn.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setString(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityManagementAction>()
                    while (rs.next()) {
                        list.add(mapAction(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveActionOutcome(outcome: ProfitabilityActionOutcome) {
        transactionManager.inTransaction(TenantContext(outcome.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_action_outcomes (
                    outcome_id, action_id, alert_id, tenant_id, evaluated_at,
                    metric_before, metric_after, improvement_percentage,
                    realized_savings_or_revenue, is_effective, evaluation_notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (outcome_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, outcome.outcomeId)
                ps.setString(2, outcome.actionId)
                ps.setString(3, outcome.alertId)
                ps.setString(4, outcome.tenantId)
                ps.setLong(5, outcome.evaluatedAt)
                ps.setBigDecimal(6, outcome.metricBefore)
                ps.setBigDecimal(7, outcome.metricAfter)
                ps.setBigDecimal(8, outcome.improvementPercentage)
                ps.setBigDecimal(9, outcome.realizedSavingsOrRevenue)
                ps.setBoolean(10, outcome.isEffective)
                ps.setString(11, outcome.evaluationNotes)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun saveCorrelation(correlation: ProfitabilityAlertCorrelation) {
        transactionManager.inTransaction(TenantContext(correlation.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_alert_correlations (
                    correlation_id, tenant_id, project_id, correlation_title,
                    primary_dimension, primary_entity_id, primary_entity_label,
                    correlated_alert_ids, composite_severity, total_financial_impact,
                    correlation_reason, detected_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
                ON CONFLICT (correlation_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, correlation.correlationId)
                ps.setString(2, correlation.tenantId)
                ps.setString(3, correlation.projectId)
                ps.setString(4, correlation.correlationTitle)
                ps.setString(5, correlation.primaryDimension.name)
                ps.setString(6, correlation.primaryEntityId)
                ps.setString(7, correlation.primaryEntityLabel)
                val jsonAlerts = correlation.correlatedAlertIds.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                ps.setString(8, jsonAlerts)
                ps.setString(9, correlation.compositeSeverity.name)
                ps.setBigDecimal(10, correlation.totalFinancialImpact)
                ps.setString(11, correlation.correlationReason)
                ps.setLong(12, correlation.detectedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listCorrelations(tenantId: String, projectId: String): List<ProfitabilityAlertCorrelation> {
        return transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alert_correlations WHERE tenant_id = ? AND project_id = ? ORDER BY detected_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAlertCorrelation>()
                    while (rs.next()) {
                        list.add(
                            ProfitabilityAlertCorrelation(
                                correlationId = rs.getString("correlation_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                correlationTitle = rs.getString("correlation_title"),
                                primaryDimension = ProfitabilityAlertDimension.valueOf(rs.getString("primary_dimension")),
                                primaryEntityId = rs.getString("primary_entity_id"),
                                primaryEntityLabel = rs.getString("primary_entity_label"),
                                correlatedAlertIds = emptyList(),
                                compositeSeverity = ProfitabilityAlertSeverity.valueOf(rs.getString("composite_severity")),
                                totalFinancialImpact = rs.getBigDecimal("total_financial_impact").setScale(4, RoundingMode.HALF_UP),
                                correlationReason = rs.getString("correlation_reason"),
                                detectedAt = rs.getLong("detected_at")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveEscalation(escalation: ProfitabilityAlertEscalation) {
        transactionManager.inTransaction(TenantContext(escalation.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_alert_escalations (
                    escalation_id, alert_id, tenant_id, escalation_level,
                    age_in_hours, recurrence_count, is_action_overdue,
                    financial_impact, justification, calculated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (escalation_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, escalation.escalationId)
                ps.setString(2, escalation.alertId)
                ps.setString(3, escalation.tenantId)
                ps.setString(4, escalation.escalationLevel.name)
                ps.setLong(5, escalation.ageInHours)
                ps.setInt(6, escalation.recurrenceCount)
                ps.setBoolean(7, escalation.isActionOverdue)
                ps.setBigDecimal(8, escalation.financialImpact)
                ps.setString(9, escalation.justification)
                ps.setLong(10, escalation.calculatedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listEscalations(tenantId: String, projectId: String): List<ProfitabilityAlertEscalation> {
        return transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alert_escalations WHERE tenant_id = ? ORDER BY calculated_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAlertEscalation>()
                    while (rs.next()) {
                        list.add(
                            ProfitabilityAlertEscalation(
                                escalationId = rs.getString("escalation_id"),
                                alertId = rs.getString("alert_id"),
                                tenantId = rs.getString("tenant_id"),
                                escalationLevel = AlertEscalationLevel.valueOf(rs.getString("escalation_level")),
                                ageInHours = rs.getLong("age_in_hours"),
                                recurrenceCount = rs.getInt("recurrence_count"),
                                isActionOverdue = rs.getBoolean("is_action_overdue"),
                                financialImpact = rs.getBigDecimal("financial_impact").setScale(4, RoundingMode.HALF_UP),
                                justification = rs.getString("justification"),
                                calculatedAt = rs.getLong("calculated_at")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveMonitoringSnapshot(snapshot: ProfitabilityMonitoringSnapshot) {
        transactionManager.inTransaction(TenantContext(snapshot.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_monitoring_snapshots (
                    snapshot_id, tenant_id, project_id, period_id, total_active_alerts,
                    critical_alert_count, high_alert_count, medium_alert_count, low_alert_count,
                    total_unresolved_financial_impact, open_action_count, overdue_action_count,
                    recurring_issue_count, escalated_alert_count, generated_at, integrity_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, snapshot.snapshotId)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.periodId)
                ps.setInt(5, snapshot.totalActiveAlerts)
                ps.setInt(6, snapshot.criticalAlertCount)
                ps.setInt(7, snapshot.highAlertCount)
                ps.setInt(8, snapshot.mediumAlertCount)
                ps.setInt(9, snapshot.lowAlertCount)
                ps.setBigDecimal(10, snapshot.totalUnresolvedFinancialImpact)
                ps.setInt(11, snapshot.openActionCount)
                ps.setInt(12, snapshot.overdueActionCount)
                ps.setInt(13, snapshot.recurringIssueCount)
                ps.setInt(14, snapshot.escalatedAlertCount)
                ps.setLong(15, snapshot.generatedAt)
                ps.setString(16, snapshot.integrityHash)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getLatestMonitoringSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): ProfitabilityMonitoringSnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val conn = ctx.connection
            val sql = StringBuilder("SELECT * FROM profitability_monitoring_snapshots WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<String>(tenantId, projectId)
            if (periodId != null) {
                sql.append(" AND period_id = ?")
                params.add(periodId)
            }
            sql.append(" ORDER BY generated_at DESC LIMIT 1")

            conn.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setString(i + 1, p) }
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val crit = rs.getInt("critical_alert_count")
                        val high = rs.getInt("high_alert_count")
                        val med = rs.getInt("medium_alert_count")
                        val low = rs.getInt("low_alert_count")
                        ProfitabilityMonitoringSnapshot(
                            snapshotId = rs.getString("snapshot_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            periodId = rs.getString("period_id"),
                            totalActiveAlerts = rs.getInt("total_active_alerts"),
                            criticalAlertCount = crit,
                            highAlertCount = high,
                            mediumAlertCount = med,
                            lowAlertCount = low,
                            totalUnresolvedFinancialImpact = rs.getBigDecimal("total_unresolved_financial_impact").setScale(4, RoundingMode.HALF_UP),
                            openActionCount = rs.getInt("open_action_count"),
                            overdueActionCount = rs.getInt("overdue_action_count"),
                            recurringIssueCount = rs.getInt("recurring_issue_count"),
                            escalatedAlertCount = rs.getInt("escalated_alert_count"),
                            severityDistribution = mapOf(
                                ProfitabilityAlertSeverity.CRITICAL to crit,
                                ProfitabilityAlertSeverity.HIGH to high,
                                ProfitabilityAlertSeverity.MEDIUM to med,
                                ProfitabilityAlertSeverity.LOW to low
                            ),
                            dimensionDistribution = emptyMap(),
                            generatedAt = rs.getLong("generated_at"),
                            integrityHash = rs.getString("integrity_hash")
                        )
                    } else null
                }
            }
        }
    }

    override suspend fun saveAuditEvent(event: ProfitabilityAlertAuditEvent) {
        transactionManager.inTransaction(TenantContext(event.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_alert_audit_events (
                    event_id, tenant_id, project_id, alert_id, action, actor_id,
                    actor_role, previous_state, new_state, notes, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.alertId)
                ps.setString(5, event.action)
                ps.setString(6, event.actorId)
                ps.setString(7, event.actorRole)
                ps.setString(8, event.previousState)
                ps.setString(9, event.newState)
                ps.setString(10, event.notes)
                ps.setLong(11, event.timestamp)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listAuditEvents(tenantId: String, alertId: String): List<ProfitabilityAlertAuditEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alert_audit_events WHERE tenant_id = ? AND alert_id = ? ORDER BY timestamp DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, alertId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAlertAuditEvent>()
                    while (rs.next()) {
                        list.add(
                            ProfitabilityAlertAuditEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                alertId = rs.getString("alert_id"),
                                action = rs.getString("action"),
                                actorId = rs.getString("actor_id"),
                                actorRole = rs.getString("actor_role"),
                                previousState = rs.getString("previous_state"),
                                newState = rs.getString("new_state"),
                                notes = rs.getString("notes"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveProvenance(provenance: ProfitabilityAlertProvenance) {
        transactionManager.inTransaction(TenantContext(provenance.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO profitability_alert_provenance (
                    provenance_id, alert_id, tenant_id, source_module, source_step,
                    source_entity_type, source_entity_id, metric_key, metric_value,
                    calculation_timestamp, provenance_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (provenance_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, provenance.provenanceId)
                ps.setString(2, provenance.alertId)
                ps.setString(3, provenance.tenantId)
                ps.setString(4, provenance.sourceModule)
                ps.setString(5, provenance.sourceStep)
                ps.setString(6, provenance.sourceEntityType)
                ps.setString(7, provenance.sourceEntityId)
                ps.setString(8, provenance.metricKey)
                ps.setBigDecimal(9, provenance.metricValue)
                ps.setLong(10, provenance.calculationTimestamp)
                ps.setString(11, provenance.provenanceHash)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listProvenance(tenantId: String, alertId: String): List<ProfitabilityAlertProvenance> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM profitability_alert_provenance WHERE tenant_id = ? AND alert_id = ? ORDER BY calculation_timestamp DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, alertId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAlertProvenance>()
                    while (rs.next()) {
                        list.add(
                            ProfitabilityAlertProvenance(
                                provenanceId = rs.getString("provenance_id"),
                                alertId = rs.getString("alert_id"),
                                tenantId = rs.getString("tenant_id"),
                                sourceModule = rs.getString("source_module"),
                                sourceStep = rs.getString("source_step"),
                                sourceEntityType = rs.getString("source_entity_type"),
                                sourceEntityId = rs.getString("source_entity_id"),
                                metricKey = rs.getString("metric_key"),
                                metricValue = rs.getBigDecimal("metric_value").setScale(4, RoundingMode.HALF_UP),
                                calculationTimestamp = rs.getLong("calculation_timestamp"),
                                provenanceHash = rs.getString("provenance_hash")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    private fun mapAlert(rs: ResultSet): ProfitabilityAlert {
        val ackAt = rs.getLong("acknowledged_at")
        val resAt = rs.getLong("resolved_at")
        val recAction = rs.getString("recommended_action_code")

        return ProfitabilityAlert(
            alertId = rs.getString("alert_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            alertType = ProfitabilityAlertType.valueOf(rs.getString("alert_type")),
            severity = ProfitabilityAlertSeverity.valueOf(rs.getString("severity")),
            status = ProfitabilityAlertStatus.valueOf(rs.getString("status")),
            dimensionType = ProfitabilityAlertDimension.valueOf(rs.getString("dimension_type")),
            dimensionId = rs.getString("dimension_id"),
            dimensionLabel = rs.getString("dimension_label"),
            periodId = rs.getString("period_id"),
            sourceModule = rs.getString("source_module"),
            sourceStep = rs.getString("source_step"),
            sourceEntityType = rs.getString("source_entity_type"),
            sourceEntityId = rs.getString("source_entity_id"),
            triggerMetric = rs.getString("trigger_metric"),
            observedValue = rs.getBigDecimal("observed_value").setScale(4, RoundingMode.HALF_UP),
            thresholdValue = rs.getBigDecimal("threshold_value").setScale(4, RoundingMode.HALF_UP),
            direction = ProfitabilityAlertDirection.valueOf(rs.getString("direction")),
            financialImpact = rs.getBigDecimal("financial_impact").setScale(4, RoundingMode.HALF_UP),
            detectedAt = rs.getLong("detected_at"),
            firstDetectedAt = rs.getLong("first_detected_at"),
            lastDetectedAt = rs.getLong("last_detected_at"),
            occurrenceCount = rs.getInt("occurrence_count"),
            fingerprint = rs.getString("fingerprint"),
            integrityHash = rs.getString("integrity_hash"),
            explanation = rs.getString("explanation"),
            recommendedActionCode = if (!recAction.isNullOrBlank()) ManagementActionCode.valueOf(recAction) else null,
            isRecurring = rs.getBoolean("is_recurring"),
            ruleId = rs.getString("rule_id"),
            acknowledgedAt = if (ackAt != 0L) ackAt else null,
            acknowledgedBy = rs.getString("acknowledged_by"),
            resolvedAt = if (resAt != 0L) resAt else null,
            resolvedBy = rs.getString("resolved_by"),
            resolutionNotes = rs.getString("resolution_notes")
        )
    }

    private fun mapRule(rs: ResultSet): ProfitabilityAlertRule {
        val effTo = rs.getLong("effective_to")
        return ProfitabilityAlertRule(
            ruleId = rs.getString("rule_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            ruleName = rs.getString("rule_name"),
            alertType = ProfitabilityAlertType.valueOf(rs.getString("alert_type")),
            dimensionType = ProfitabilityAlertDimension.valueOf(rs.getString("dimension_type")),
            thresholdMetric = rs.getString("threshold_metric"),
            thresholdValue = rs.getBigDecimal("threshold_value").setScale(4, RoundingMode.HALF_UP),
            comparisonOperator = ComparisonOperator.valueOf(rs.getString("comparison_operator")),
            severity = ProfitabilityAlertSeverity.valueOf(rs.getString("severity")),
            enabled = rs.getBoolean("enabled"),
            description = rs.getString("description") ?: "",
            effectiveFrom = rs.getLong("effective_from"),
            effectiveTo = if (effTo != 0L) effTo else null,
            version = rs.getInt("version"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapAction(rs: ResultSet): ProfitabilityManagementAction {
        val due = rs.getLong("due_at")
        val started = rs.getLong("started_at")
        val completed = rs.getLong("completed_at")
        val verified = rs.getLong("verified_at")
        val realized = rs.getBigDecimal("realized_financial_impact")

        return ProfitabilityManagementAction(
            actionId = rs.getString("action_id"),
            alertId = rs.getString("alert_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            actionCode = ManagementActionCode.valueOf(rs.getString("action_code")),
            actionTitle = rs.getString("action_title"),
            actionDescription = rs.getString("action_description"),
            priorityScore = rs.getBigDecimal("priority_score").setScale(4, RoundingMode.HALF_UP),
            status = ManagementActionStatus.valueOf(rs.getString("status")),
            assignedTo = rs.getString("assigned_to"),
            assignedBy = rs.getString("assigned_by"),
            dueAt = if (due != 0L) due else null,
            startedAt = if (started != 0L) started else null,
            completedAt = if (completed != 0L) completed else null,
            verifiedAt = if (verified != 0L) verified else null,
            verifiedBy = rs.getString("verified_by"),
            expectedFinancialImpact = rs.getBigDecimal("expected_financial_impact").setScale(4, RoundingMode.HALF_UP),
            realizedFinancialImpact = realized?.setScale(4, RoundingMode.HALF_UP),
            outcomeNotes = rs.getString("outcome_notes"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            integrityHash = rs.getString("integrity_hash")
        )
    }
}
