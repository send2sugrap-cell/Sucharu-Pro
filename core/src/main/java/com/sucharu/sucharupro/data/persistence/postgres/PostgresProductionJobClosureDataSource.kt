package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.jobclosure.ProductionJobClosureDataSource
import com.sucharu.sucharupro.domain.model.jobclosure.*
import com.sucharu.sucharupro.domain.model.jobcosting.VarianceClassification
import java.math.BigDecimal
import java.sql.ResultSet

class PostgresProductionJobClosureDataSource(
    private val transactionManager: TransactionManager
) : ProductionJobClosureDataSource {

    override suspend fun saveClosureRecord(tenantId: String, record: ProductionJobClosureRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_job_closure_records (
                    closure_id, tenant_id, execution_job_id, order_id, closure_status,
                    readiness_audit_json, scorecard_json, provenance_graph_json,
                    post_mortem_summary_json, master_seal_hash, total_good_units_released,
                    grand_total_actual_cost, total_cost_variance, overall_manufacturing_score,
                    performance_grade, closed_at, closed_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (closure_id) DO UPDATE SET
                    closure_status = EXCLUDED.closure_status,
                    readiness_audit_json = EXCLUDED.readiness_audit_json,
                    scorecard_json = EXCLUDED.scorecard_json,
                    provenance_graph_json = EXCLUDED.provenance_graph_json,
                    post_mortem_summary_json = EXCLUDED.post_mortem_summary_json,
                    master_seal_hash = EXCLUDED.master_seal_hash,
                    total_good_units_released = EXCLUDED.total_good_units_released,
                    grand_total_actual_cost = EXCLUDED.grand_total_actual_cost,
                    total_cost_variance = EXCLUDED.total_cost_variance,
                    overall_manufacturing_score = EXCLUDED.overall_manufacturing_score,
                    performance_grade = EXCLUDED.performance_grade,
                    closed_at = EXCLUDED.closed_at,
                    closed_by = EXCLUDED.closed_by
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, record.closureId)
                ps.setString(2, tenantId)
                ps.setString(3, record.executionJobId)
                ps.setString(4, record.orderId)
                ps.setString(5, record.closureStatus.name)
                ps.setString(6, serializeReadinessAudit(record.readinessAudit))
                ps.setString(7, serializeScorecard(record.scorecard))
                ps.setString(8, serializeProvenanceGraph(record.provenanceGraph))
                ps.setString(9, serializePostMortem(record.postMortemSummary))
                ps.setString(10, record.masterCertificate.masterSealHash)
                ps.setBigDecimal(11, record.masterCertificate.totalGoodUnitsReleased)
                ps.setBigDecimal(12, record.masterCertificate.grandTotalActualCost)
                ps.setBigDecimal(13, record.masterCertificate.totalCostVariance)
                ps.setBigDecimal(14, record.masterCertificate.overallManufacturingScore)
                ps.setString(15, record.scorecard.performanceGrade)
                ps.setLong(16, record.closedAt)
                ps.setString(17, record.closedBy)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getClosureRecordByJob(tenantId: String, executionJobId: String): ProductionJobClosureRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_job_closure_records WHERE tenant_id = ? AND execution_job_id = ? ORDER BY closed_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapClosureRecord(rs) else null
                }
            }
        }
    }

    override suspend fun saveScorecard(tenantId: String, scorecard: ManufacturingPerformanceScorecard) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_job_scorecard_records (
                    scorecard_id, tenant_id, execution_job_id, order_id,
                    on_time_in_full_percentage, right_first_time_percentage,
                    cost_adherence_index, machine_efficiency_index, quality_yield_percentage,
                    overall_manufacturing_index, performance_grade, calculated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (scorecard_id) DO UPDATE SET
                    on_time_in_full_percentage = EXCLUDED.on_time_in_full_percentage,
                    right_first_time_percentage = EXCLUDED.right_first_time_percentage,
                    cost_adherence_index = EXCLUDED.cost_adherence_index,
                    machine_efficiency_index = EXCLUDED.machine_efficiency_index,
                    quality_yield_percentage = EXCLUDED.quality_yield_percentage,
                    overall_manufacturing_index = EXCLUDED.overall_manufacturing_index,
                    performance_grade = EXCLUDED.performance_grade,
                    calculated_at = EXCLUDED.calculated_at
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, "SCORE-" + scorecard.executionJobId)
                ps.setString(2, tenantId)
                ps.setString(3, scorecard.executionJobId)
                ps.setString(4, scorecard.orderId)
                ps.setBigDecimal(5, scorecard.onTimeInFullPercentage)
                ps.setBigDecimal(6, scorecard.rightFirstTimePercentage)
                ps.setBigDecimal(7, scorecard.costAdherenceIndex)
                ps.setBigDecimal(8, scorecard.machineEfficiencyIndex)
                ps.setBigDecimal(9, scorecard.qualityYieldPercentage)
                ps.setBigDecimal(10, scorecard.overallManufacturingIndex)
                ps.setString(11, scorecard.performanceGrade)
                ps.setLong(12, scorecard.calculatedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getScorecardByJob(tenantId: String, executionJobId: String): ManufacturingPerformanceScorecard? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_job_scorecard_records WHERE tenant_id = ? AND execution_job_id = ? ORDER BY calculated_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapScorecard(rs) else null
                }
            }
        }
    }

    override suspend fun saveEvent(tenantId: String, event: ProductionJobClosureEvent) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_job_closure_audit_events (
                    event_id, tenant_id, execution_job_id, event_type, actor, payload, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, tenantId)
                ps.setString(3, event.executionJobId)
                ps.setString(4, event.eventType)
                ps.setString(5, event.actor)
                ps.setString(6, event.payload)
                ps.setLong(7, event.timestamp)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobClosureEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_job_closure_audit_events WHERE tenant_id = ? AND execution_job_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionJobClosureEvent>()
                    while (rs.next()) {
                        list.add(
                            ProductionJobClosureEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                eventType = rs.getString("event_type"),
                                actor = rs.getString("actor"),
                                payload = rs.getString("payload"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    private fun mapClosureRecord(rs: ResultSet): ProductionJobClosureRecord {
        val audit = deserializeReadinessAudit(rs.getString("readiness_audit_json"))
        val scorecard = deserializeScorecard(rs.getString("scorecard_json"))
        val prov = deserializeProvenanceGraph(rs.getString("provenance_graph_json"))
        val post = deserializePostMortem(rs.getString("post_mortem_summary_json"))
        val cert = MasterProductionClosureCertificate(
            certificateId = "SEAL-" + rs.getString("execution_job_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            masterSealHash = rs.getString("master_seal_hash"),
            totalGoodUnitsReleased = rs.getBigDecimal("total_good_units_released"),
            grandTotalActualCost = rs.getBigDecimal("grand_total_actual_cost"),
            totalCostVariance = rs.getBigDecimal("total_cost_variance"),
            overallCostClassification = VarianceClassification.NEUTRAL,
            overallManufacturingScore = rs.getBigDecimal("overall_manufacturing_score"),
            sealedAt = rs.getLong("closed_at"),
            sealedBy = rs.getString("closed_by")
        )

        return ProductionJobClosureRecord(
            closureId = rs.getString("closure_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            closureStatus = JobClosureStatus.valueOf(rs.getString("closure_status")),
            readinessAudit = audit,
            scorecard = scorecard,
            provenanceGraph = prov,
            postMortemSummary = post,
            masterCertificate = cert,
            closedAt = rs.getLong("closed_at"),
            closedBy = rs.getString("closed_by")
        )
    }

    private fun mapScorecard(rs: ResultSet): ManufacturingPerformanceScorecard {
        return ManufacturingPerformanceScorecard(
            executionJobId = rs.getString("execution_job_id"),
            tenantId = rs.getString("tenant_id"),
            orderId = rs.getString("order_id"),
            onTimeInFullPercentage = rs.getBigDecimal("on_time_in_full_percentage"),
            rightFirstTimePercentage = rs.getBigDecimal("right_first_time_percentage"),
            costAdherenceIndex = rs.getBigDecimal("cost_adherence_index"),
            machineEfficiencyIndex = rs.getBigDecimal("machine_efficiency_index"),
            qualityYieldPercentage = rs.getBigDecimal("quality_yield_percentage"),
            overallManufacturingIndex = rs.getBigDecimal("overall_manufacturing_index"),
            performanceGrade = rs.getString("performance_grade"),
            calculatedAt = rs.getLong("calculated_at")
        )
    }

    private fun serializeReadinessAudit(audit: JobClosureReadinessAudit): String {
        return "${audit.executionJobId}::${audit.tenantId}::${audit.isQuoteAndCommitmentVerified}::${audit.isProductionPlanningComplete}::${audit.areAllWorkOrdersCompleted}::${audit.isSchedulingDispatched}::${audit.isShopFloorTrackingRecorded}::${audit.isFinalQcReleased}::${audit.isActualJobCostingReconciled}::${audit.isMultiTenantBoundaryValid}::${audit.isReadyForClosure}::${audit.auditedAt}::${audit.auditedBy}::${audit.auditDiscrepancies.joinToString("||")}"
    }

    private fun deserializeReadinessAudit(str: String?): JobClosureReadinessAudit {
        if (str.isNullOrBlank()) return JobClosureReadinessAudit("", "", false, false, false, false, false, false, false, false, false)
        val p = str.split("::")
        val disc = if (p.size > 13 && p[13].isNotBlank()) p[13].split("||") else emptyList()
        return JobClosureReadinessAudit(
            executionJobId = p[0],
            tenantId = p[1],
            isQuoteAndCommitmentVerified = p.getOrNull(2)?.toBoolean() ?: false,
            isProductionPlanningComplete = p.getOrNull(3)?.toBoolean() ?: false,
            areAllWorkOrdersCompleted = p.getOrNull(4)?.toBoolean() ?: false,
            isSchedulingDispatched = p.getOrNull(5)?.toBoolean() ?: false,
            isShopFloorTrackingRecorded = p.getOrNull(6)?.toBoolean() ?: false,
            isFinalQcReleased = p.getOrNull(7)?.toBoolean() ?: false,
            isActualJobCostingReconciled = p.getOrNull(8)?.toBoolean() ?: false,
            isMultiTenantBoundaryValid = p.getOrNull(9)?.toBoolean() ?: false,
            isReadyForClosure = p.getOrNull(10)?.toBoolean() ?: false,
            auditedAt = p.getOrNull(11)?.toLongOrNull() ?: System.currentTimeMillis(),
            auditedBy = p.getOrNull(12) ?: "closure-auditor",
            auditDiscrepancies = disc
        )
    }

    private fun serializeScorecard(scorecard: ManufacturingPerformanceScorecard): String {
        return "${scorecard.executionJobId}::${scorecard.tenantId}::${scorecard.orderId}::${scorecard.onTimeInFullPercentage}::${scorecard.rightFirstTimePercentage}::${scorecard.costAdherenceIndex}::${scorecard.machineEfficiencyIndex}::${scorecard.qualityYieldPercentage}::${scorecard.overallManufacturingIndex}::${scorecard.performanceGrade}::${scorecard.calculatedAt}"
    }

    private fun deserializeScorecard(str: String?): ManufacturingPerformanceScorecard {
        if (str.isNullOrBlank()) return ManufacturingPerformanceScorecard("", "", "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "N/A")
        val p = str.split("::")
        return ManufacturingPerformanceScorecard(
            executionJobId = p[0],
            tenantId = p[1],
            orderId = p[2],
            onTimeInFullPercentage = p.getOrNull(3)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            rightFirstTimePercentage = p.getOrNull(4)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            costAdherenceIndex = p.getOrNull(5)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            machineEfficiencyIndex = p.getOrNull(6)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            qualityYieldPercentage = p.getOrNull(7)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            overallManufacturingIndex = p.getOrNull(8)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            performanceGrade = p.getOrNull(9) ?: "N/A",
            calculatedAt = p.getOrNull(10)?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }

    private fun serializeProvenanceGraph(graph: ProductionJobProvenanceGraph): String {
        val nodesStr = graph.nodes.joinToString(";;") {
            "${it.stepNumber}##${it.stepName}##${it.canonicalEntityName}##${it.canonicalEntityId}##${it.completionStatus.name}##${it.verifiedAt}##${it.verifiedBy}"
        }
        return "${graph.executionJobId}::${graph.tenantId}::${graph.orderId}::${graph.isChainUnbroken}::${graph.masterProvenanceFingerprint}::$nodesStr"
    }

    private fun deserializeProvenanceGraph(str: String?): ProductionJobProvenanceGraph {
        if (str.isNullOrBlank()) return ProductionJobProvenanceGraph("", "", "", emptyList(), false, "")
        val p = str.split("::")
        val nodesList = if (p.size > 5 && p[5].isNotBlank()) {
            p[5].split(";;").mapNotNull {
                val np = it.split("##")
                if (np.size >= 7) {
                    ProductionJobProvenanceNode(
                        stepNumber = np[0].toIntOrNull() ?: 1,
                        stepName = np[1],
                        canonicalEntityName = np[2],
                        canonicalEntityId = np[3],
                        completionStatus = StepCompletionStatus.valueOf(np[4]),
                        verifiedAt = np[5].toLongOrNull() ?: System.currentTimeMillis(),
                        verifiedBy = np[6]
                    )
                } else null
            }
        } else emptyList()

        return ProductionJobProvenanceGraph(
            executionJobId = p[0],
            tenantId = p[1],
            orderId = p[2],
            isChainUnbroken = p.getOrNull(3)?.toBoolean() ?: false,
            masterProvenanceFingerprint = p.getOrNull(4) ?: "",
            nodes = nodesList
        )
    }

    private fun serializePostMortem(post: ProductionPostMortemSummary): String {
        return "${post.executionJobId}::${post.tenantId}::${post.primaryDowntimeDrivers.joinToString("||")}::${post.scrapAndDefectTakeaways.joinToString("||")}::${post.costVarianceTakeaways.joinToString("||")}::${post.operationalRecommendations.joinToString("||")}::${post.generatedAt}"
    }

    private fun deserializePostMortem(str: String?): ProductionPostMortemSummary {
        if (str.isNullOrBlank()) return ProductionPostMortemSummary("", "", emptyList(), emptyList(), emptyList(), emptyList())
        val p = str.split("::")
        return ProductionPostMortemSummary(
            executionJobId = p[0],
            tenantId = p[1],
            primaryDowntimeDrivers = if (p.size > 2 && p[2].isNotBlank()) p[2].split("||") else emptyList(),
            scrapAndDefectTakeaways = if (p.size > 3 && p[3].isNotBlank()) p[3].split("||") else emptyList(),
            costVarianceTakeaways = if (p.size > 4 && p[4].isNotBlank()) p[4].split("||") else emptyList(),
            operationalRecommendations = if (p.size > 5 && p[5].isNotBlank()) p[5].split("||") else emptyList(),
            generatedAt = p.getOrNull(6)?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}
