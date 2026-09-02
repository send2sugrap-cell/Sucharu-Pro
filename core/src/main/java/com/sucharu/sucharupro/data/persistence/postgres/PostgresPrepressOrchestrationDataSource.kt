package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.imposition.PrepressOrchestrationDataSource
import com.sucharu.sucharupro.domain.model.imposition.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.util.UUID

/**
 * PostgreSQL Data Source implementation for Prepress Orchestration Master Plans.
 * Module 18 Step 06.
 */
class PostgresPrepressOrchestrationDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "default"
) : PrepressOrchestrationDataSource {

    override suspend fun savePlan(tenantId: String, plan: PrepressOrchestrationPlan): PrepressOrchestrationPlan {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(plan.tenantId == tenantId) { "Tenant ID mismatch." }

        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val sql = """
                INSERT INTO prepress_orchestration_plans (
                    plan_id, tenant_id, plan_name, version, status,
                    job_id, order_id, order_item_id, product_name,
                    step01_imposition_id, step01_integrity_hash,
                    step02_gang_run_batch_id, step02_integrity_hash,
                    step03_nesting_id, step03_integrity_hash,
                    step04_signature_id, step04_integrity_hash,
                    step05_ctp_output_id, step05_integrity_hash,
                    required_quantity, total_produced_quantity, required_sheets,
                    sheet_utilization_percentage, waste_percentage,
                    total_signatures_count, total_plates_count,
                    press_sheet_width_mm, press_sheet_height_mm,
                    plate_width_mm, plate_height_mm,
                    readiness_score, is_fully_reconciled,
                    blocking_errors_count, warnings_count, reconciliation_summary,
                    master_integrity_hash, approval_status, approved_by, approved_at,
                    ai_handoff_status, downstream_handoff_status, notes,
                    created_at, created_by
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?
                )
                ON CONFLICT (plan_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    version = EXCLUDED.version,
                    total_produced_quantity = EXCLUDED.total_produced_quantity,
                    required_sheets = EXCLUDED.required_sheets,
                    sheet_utilization_percentage = EXCLUDED.sheet_utilization_percentage,
                    waste_percentage = EXCLUDED.waste_percentage,
                    total_signatures_count = EXCLUDED.total_signatures_count,
                    total_plates_count = EXCLUDED.total_plates_count,
                    readiness_score = EXCLUDED.readiness_score,
                    is_fully_reconciled = EXCLUDED.is_fully_reconciled,
                    blocking_errors_count = EXCLUDED.blocking_errors_count,
                    warnings_count = EXCLUDED.warnings_count,
                    reconciliation_summary = EXCLUDED.reconciliation_summary,
                    master_integrity_hash = EXCLUDED.master_integrity_hash,
                    approval_status = EXCLUDED.approval_status,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    notes = EXCLUDED.notes
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                var idx = 1
                stmt.setString(idx++, plan.planId)
                stmt.setString(idx++, plan.tenantId)
                stmt.setString(idx++, plan.planName)
                stmt.setInt(idx++, plan.version)
                stmt.setString(idx++, plan.status.name)

                stmt.setString(idx++, plan.jobId)
                stmt.setString(idx++, plan.orderId)
                stmt.setString(idx++, plan.orderItemId)
                stmt.setString(idx++, plan.productName)

                stmt.setString(idx++, plan.step01ImpositionId)
                stmt.setString(idx++, plan.step01IntegrityHash)
                stmt.setString(idx++, plan.step02GangRunBatchId)
                stmt.setString(idx++, plan.step02IntegrityHash)
                stmt.setString(idx++, plan.step03NestingId)
                stmt.setString(idx++, plan.step03IntegrityHash)
                stmt.setString(idx++, plan.step04SignatureId)
                stmt.setString(idx++, plan.step04IntegrityHash)
                stmt.setString(idx++, plan.step05CtpOutputId)
                stmt.setString(idx++, plan.step05IntegrityHash)

                stmt.setLong(idx++, plan.requiredQuantity)
                stmt.setLong(idx++, plan.totalProducedQuantity)
                stmt.setLong(idx++, plan.requiredSheets)
                stmt.setBigDecimal(idx++, plan.sheetUtilizationPercentage)
                stmt.setBigDecimal(idx++, plan.wastePercentage)
                stmt.setInt(idx++, plan.totalSignaturesCount)
                stmt.setInt(idx++, plan.totalPlatesCount)

                stmt.setBigDecimal(idx++, plan.pressSheetWidthMm)
                stmt.setBigDecimal(idx++, plan.pressSheetHeightMm)
                stmt.setBigDecimal(idx++, plan.plateWidthMm)
                stmt.setBigDecimal(idx++, plan.plateHeightMm)

                stmt.setBigDecimal(idx++, plan.readinessScore.overallScore)
                stmt.setBoolean(idx++, plan.reconciliationResult.isReconciled)
                stmt.setInt(idx++, plan.reconciliationResult.blockingErrorsCount)
                stmt.setInt(idx++, plan.reconciliationResult.warningsCount)
                stmt.setString(idx++, plan.reconciliationResult.summary)

                stmt.setString(idx++, plan.masterIntegrityHash)
                stmt.setString(idx++, plan.approvalStatus)
                stmt.setString(idx++, plan.approvedBy)
                if (plan.approvedAt != null) stmt.setLong(idx++, plan.approvedAt) else stmt.setNull(idx++, java.sql.Types.BIGINT)

                stmt.setString(idx++, plan.aiHandoffStatus)
                stmt.setString(idx++, plan.downstreamHandoffStatus)
                stmt.setString(idx++, plan.notes)
                stmt.setLong(idx++, plan.createdAt)
                stmt.setString(idx++, plan.createdBy)

                stmt.executeUpdate()
            }

            // Save Discrepancies
            if (plan.reconciliationResult.discrepancies.isNotEmpty()) {
                val discSql = """
                    INSERT INTO prepress_reconciliation_discrepancies (
                        discrepancy_id, plan_id, tenant_id, field_name,
                        source_step, target_step, expected_value, actual_value,
                        severity, message
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (discrepancy_id) DO NOTHING
                """.trimIndent()
                conn.prepareStatement(discSql).use { stmt ->
                    for (d in plan.reconciliationResult.discrepancies) {
                        stmt.setString(1, d.discrepancyId)
                        stmt.setString(2, plan.planId)
                        stmt.setString(3, plan.tenantId)
                        stmt.setString(4, d.field)
                        stmt.setString(5, d.sourceStep)
                        stmt.setString(6, d.targetStep)
                        stmt.setString(7, d.expectedValue)
                        stmt.setString(8, d.actualValue)
                        stmt.setString(9, d.severity.name)
                        stmt.setString(10, d.message)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }

            // Save Recommendations
            if (plan.recommendations.isNotEmpty()) {
                val recSql = """
                    INSERT INTO prepress_optimization_recommendations (
                        recommendation_id, plan_id, tenant_id, recommendation_type,
                        title, description, affected_step,
                        estimated_waste_reduction_percentage, estimated_plate_savings_count,
                        rationale, confidence_score, requires_approval, is_applied
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (recommendation_id) DO NOTHING
                """.trimIndent()
                conn.prepareStatement(recSql).use { stmt ->
                    for (r in plan.recommendations) {
                        stmt.setString(1, r.recommendationId)
                        stmt.setString(2, plan.planId)
                        stmt.setString(3, plan.tenantId)
                        stmt.setString(4, r.recommendationType)
                        stmt.setString(5, r.title)
                        stmt.setString(6, r.description)
                        stmt.setString(7, r.affectedStep)
                        stmt.setBigDecimal(8, r.estimatedWasteReductionPercentage)
                        stmt.setInt(9, r.estimatedPlateSavingsCount)
                        stmt.setString(10, r.rationale)
                        stmt.setBigDecimal(11, r.confidenceScore)
                        stmt.setBoolean(12, r.requiresApproval)
                        stmt.setBoolean(13, r.isApplied)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }

            // Audit record
            val auditSql = """
                INSERT INTO prepress_orchestration_audits (
                    audit_id, plan_id, tenant_id, action, previous_status, new_status, actor, reason, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(auditSql).use { stmt ->
                stmt.setString(1, UUID.randomUUID().toString())
                stmt.setString(2, plan.planId)
                stmt.setString(3, tenantId)
                stmt.setString(4, "PLAN_CREATED_OR_UPDATED")
                stmt.setString(5, null)
                stmt.setString(6, plan.status.name)
                stmt.setString(7, plan.createdBy)
                stmt.setString(8, "Master prepress orchestration plan version ${plan.version}")
                stmt.setLong(9, System.currentTimeMillis())
                stmt.executeUpdate()
            }

            plan
        }
    }

    override suspend fun findById(tenantId: String, planId: String): PrepressOrchestrationPlan? {
        if (tenantId.isBlank() || planId.isBlank()) return null
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM prepress_orchestration_plans WHERE tenant_id = ? AND plan_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, planId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToPlan(rs, conn) else null
                }
            }
        }
    }

    override suspend fun findByJobId(tenantId: String, jobId: String): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank() || jobId.isBlank()) return emptyList()
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM prepress_orchestration_plans WHERE tenant_id = ? AND job_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, jobId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<PrepressOrchestrationPlan>()
                    while (rs.next()) {
                        list.add(mapRowToPlan(rs, conn))
                    }
                    list
                }
            }
        }
    }

    override suspend fun findByOrderId(tenantId: String, orderId: String): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank() || orderId.isBlank()) return emptyList()
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM prepress_orchestration_plans WHERE tenant_id = ? AND order_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, orderId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<PrepressOrchestrationPlan>()
                    while (rs.next()) {
                        list.add(mapRowToPlan(rs, conn))
                    }
                    list
                }
            }
        }
    }

    override suspend fun listAll(tenantId: String, limit: Int): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank()) return emptyList()
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM prepress_orchestration_plans WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setInt(2, limit)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<PrepressOrchestrationPlan>()
                    while (rs.next()) {
                        list.add(mapRowToPlan(rs, conn))
                    }
                    list
                }
            }
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        planId: String,
        newStatus: PrepressPlanStatus,
        actor: String,
        notes: String?
    ): Boolean {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(planId.isNotBlank()) { "Plan ID must not be blank." }

        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val currentStatusSql = "SELECT status FROM prepress_orchestration_plans WHERE tenant_id = ? AND plan_id = ?"
            val currentStatus = conn.prepareStatement(currentStatusSql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, planId)
                stmt.executeQuery().use { rs -> if (rs.next()) rs.getString("status") else null }
            } ?: return@inTransaction false

            val updateSql = """
                UPDATE prepress_orchestration_plans
                SET status = ?,
                    approval_status = CASE WHEN ? = 'APPROVED' THEN 'APPROVED' ELSE approval_status END,
                    approved_by = CASE WHEN ? = 'APPROVED' THEN ? ELSE approved_by END,
                    approved_at = CASE WHEN ? = 'APPROVED' THEN ? ELSE approved_at END,
                    notes = COALESCE(?, notes)
                WHERE tenant_id = ? AND plan_id = ?
            """.trimIndent()

            val now = System.currentTimeMillis()
            conn.prepareStatement(updateSql).use { stmt ->
                stmt.setString(1, newStatus.name)
                stmt.setString(2, newStatus.name)
                stmt.setString(3, newStatus.name)
                stmt.setString(4, actor)
                stmt.setString(5, newStatus.name)
                stmt.setLong(6, now)
                stmt.setString(7, notes)
                stmt.setString(8, tenantId)
                stmt.setString(9, planId)
                stmt.executeUpdate()
            }

            val auditSql = """
                INSERT INTO prepress_orchestration_audits (
                    audit_id, plan_id, tenant_id, action, previous_status, new_status, actor, reason, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(auditSql).use { stmt ->
                stmt.setString(1, UUID.randomUUID().toString())
                stmt.setString(2, planId)
                stmt.setString(3, tenantId)
                stmt.setString(4, "STATUS_UPDATE")
                stmt.setString(5, currentStatus)
                stmt.setString(6, newStatus.name)
                stmt.setString(7, actor)
                stmt.setString(8, notes)
                stmt.setLong(9, now)
                stmt.executeUpdate()
            }
            true
        }
    }

    override suspend fun recordAudit(
        tenantId: String,
        planId: String,
        action: String,
        previousStatus: String?,
        newStatus: String,
        actor: String,
        reason: String?
    ): Boolean {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO prepress_orchestration_audits (
                    audit_id, plan_id, tenant_id, action, previous_status, new_status, actor, reason, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, UUID.randomUUID().toString())
                stmt.setString(2, planId)
                stmt.setString(3, tenantId)
                stmt.setString(4, action)
                stmt.setString(5, previousStatus)
                stmt.setString(6, newStatus)
                stmt.setString(7, actor)
                stmt.setString(8, reason)
                stmt.setLong(9, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            true
        }
    }

    private fun mapRowToPlan(rs: ResultSet, conn: java.sql.Connection): PrepressOrchestrationPlan {
        val planId = rs.getString("plan_id")
        val tenantId = rs.getString("tenant_id")

        val discrepancies = mutableListOf<ReconciliationDiscrepancy>()
        val discSql = "SELECT * FROM prepress_reconciliation_discrepancies WHERE plan_id = ? AND tenant_id = ?"
        conn.prepareStatement(discSql).use { stmt ->
            stmt.setString(1, planId)
            stmt.setString(2, tenantId)
            stmt.executeQuery().use { discRs ->
                while (discRs.next()) {
                    discrepancies.add(
                        ReconciliationDiscrepancy(
                            discrepancyId = discRs.getString("discrepancy_id"),
                            field = discRs.getString("field_name"),
                            sourceStep = discRs.getString("source_step"),
                            targetStep = discRs.getString("target_step"),
                            expectedValue = discRs.getString("expected_value"),
                            actualValue = discRs.getString("actual_value"),
                            severity = ReconciliationSeverity.valueOf(discRs.getString("severity")),
                            message = discRs.getString("message")
                        )
                    )
                }
            }
        }

        val recommendations = mutableListOf<PrepressOptimizationRecommendation>()
        val recSql = "SELECT * FROM prepress_optimization_recommendations WHERE plan_id = ? AND tenant_id = ?"
        conn.prepareStatement(recSql).use { stmt ->
            stmt.setString(1, planId)
            stmt.setString(2, tenantId)
            stmt.executeQuery().use { recRs ->
                while (recRs.next()) {
                    recommendations.add(
                        PrepressOptimizationRecommendation(
                            recommendationId = recRs.getString("recommendation_id"),
                            recommendationType = recRs.getString("recommendation_type"),
                            title = recRs.getString("title"),
                            description = recRs.getString("description"),
                            affectedStep = recRs.getString("affected_step"),
                            estimatedWasteReductionPercentage = recRs.getBigDecimal("estimated_waste_reduction_percentage"),
                            estimatedPlateSavingsCount = recRs.getInt("estimated_plate_savings_count"),
                            rationale = recRs.getString("rationale"),
                            confidenceScore = recRs.getBigDecimal("confidence_score"),
                            requiresApproval = recRs.getBoolean("requires_approval"),
                            isApplied = recRs.getBoolean("is_applied")
                        )
                    )
                }
            }
        }

        val isReconciled = rs.getBoolean("is_fully_reconciled")
        val blockingCount = rs.getInt("blocking_errors_count")
        val warningCount = rs.getInt("warnings_count")
        val reconSummary = rs.getString("reconciliation_summary") ?: ""

        val reconciliation = PrepressReconciliationResult(
            isReconciled = isReconciled,
            blockingErrorsCount = blockingCount,
            warningsCount = warningCount,
            discrepancies = discrepancies,
            reconciledProducedQuantity = rs.getLong("total_produced_quantity"),
            reconciledRequiredSheets = rs.getLong("required_sheets"),
            reconciledTotalPages = 16,
            reconciledSignaturesCount = rs.getInt("total_signatures_count"),
            reconciledPlatesCount = rs.getInt("total_plates_count"),
            reconciledWastePercentage = rs.getBigDecimal("waste_percentage"),
            reconciledUtilizationPercentage = rs.getBigDecimal("sheet_utilization_percentage"),
            summary = reconSummary
        )

        val readinessScore = PrepressReadinessScore(
            overallScore = rs.getBigDecimal("readiness_score"),
            geometricValidityScore = BigDecimal("20.0000"),
            nestingEfficiencyScore = rs.getBigDecimal("sheet_utilization_percentage").multiply(BigDecimal("0.20")),
            gangRunEfficiencyScore = BigDecimal("15.0000"),
            sheetUtilizationScore = rs.getBigDecimal("sheet_utilization_percentage").multiply(BigDecimal("0.20")),
            signatureValidityScore = BigDecimal("15.0000"),
            ctpReadinessScore = BigDecimal("15.0000"),
            integrityVerificationScore = BigDecimal("15.0000"),
            penaltyPoints = BigDecimal.ZERO,
            summary = "Authoritative persisted readiness score"
        )

        return PrepressOrchestrationPlan(
            planId = planId,
            tenantId = tenantId,
            planName = rs.getString("plan_name"),
            version = rs.getInt("version"),
            status = PrepressPlanStatus.valueOf(rs.getString("status")),
            jobId = rs.getString("job_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            productName = rs.getString("product_name"),
            step01ImpositionId = rs.getString("step01_imposition_id"),
            step01IntegrityHash = rs.getString("step01_integrity_hash"),
            step02GangRunBatchId = rs.getString("step02_gang_run_batch_id"),
            step02IntegrityHash = rs.getString("step02_integrity_hash"),
            step03NestingId = rs.getString("step03_nesting_id"),
            step03IntegrityHash = rs.getString("step03_integrity_hash"),
            step04SignatureId = rs.getString("step04_signature_id"),
            step04IntegrityHash = rs.getString("step04_integrity_hash"),
            step05CtpOutputId = rs.getString("step05_ctp_output_id"),
            step05IntegrityHash = rs.getString("step05_integrity_hash"),
            requiredQuantity = rs.getLong("required_quantity"),
            totalProducedQuantity = rs.getLong("total_produced_quantity"),
            requiredSheets = rs.getLong("required_sheets"),
            sheetUtilizationPercentage = rs.getBigDecimal("sheet_utilization_percentage"),
            wastePercentage = rs.getBigDecimal("waste_percentage"),
            totalSignaturesCount = rs.getInt("total_signatures_count"),
            totalPlatesCount = rs.getInt("total_plates_count"),
            pressSheetWidthMm = rs.getBigDecimal("press_sheet_width_mm"),
            pressSheetHeightMm = rs.getBigDecimal("press_sheet_height_mm"),
            plateWidthMm = rs.getBigDecimal("plate_width_mm"),
            plateHeightMm = rs.getBigDecimal("plate_height_mm"),
            reconciliationResult = reconciliation,
            readinessScore = readinessScore,
            recommendations = recommendations,
            masterIntegrityHash = rs.getString("master_integrity_hash"),
            approvalStatus = rs.getString("approval_status"),
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getLong("approved_at").takeIf { !rs.wasNull() },
            aiHandoffStatus = rs.getString("ai_handoff_status"),
            downstreamHandoffStatus = rs.getString("downstream_handoff_status"),
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by")
        )
    }
}
