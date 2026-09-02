package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPerformanceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

class PostgresVendorPerformanceDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPerformanceDataSource {

    private val scorecardFlows = mutableMapOf<String, MutableStateFlow<List<VendorPerformanceScorecard>>>()
    private val evaluationFlows = mutableMapOf<String, MutableStateFlow<List<VendorEvaluation>>>()
    private val correctiveActionFlows = mutableMapOf<String, MutableStateFlow<List<VendorCorrectiveAction>>>()

    // --- Helpers ---
    private fun toTimestamp(instant: Instant?): Timestamp? = instant?.let { Timestamp.from(it) }
    private fun toInstant(ts: Timestamp?): Instant? = ts?.toInstant()

    private fun mapKpiRow(rs: ResultSet): VendorPerformanceKpi {
        return VendorPerformanceKpi(
            projectId = rs.getString("project_id"),
            kpiId = rs.getString("kpi_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            code = rs.getString("code"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            kpiType = KpiType.valueOf(rs.getString("kpi_type")),
            measurementMethod = KpiMeasurementMethod.valueOf(rs.getString("measurement_method")),
            targetValue = rs.getDouble("target_value"),
            minimumAcceptableValue = rs.getObject("minimum_acceptable_value")?.let { (it as Number).toDouble() },
            maximumAcceptableValue = rs.getObject("maximum_acceptable_value")?.let { (it as Number).toDouble() },
            unit = rs.getString("unit"),
            direction = KpiDirection.valueOf(rs.getString("direction")),
            weight = rs.getDouble("weight"),
            status = KpiStatus.valueOf(rs.getString("status")),
            effectiveFrom = toInstant(rs.getTimestamp("effective_from")) ?: Instant.now(),
            effectiveTo = toInstant(rs.getTimestamp("effective_to")),
            version = rs.getLong("version"),
            createdAt = toInstant(rs.getTimestamp("created_at")) ?: Instant.now(),
            createdBy = rs.getString("created_by"),
            updatedAt = toInstant(rs.getTimestamp("updated_at")) ?: Instant.now(),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapMeasurementRow(rs: ResultSet): VendorPerformanceMeasurement {
        return VendorPerformanceMeasurement(
            projectId = rs.getString("project_id"),
            measurementId = rs.getString("measurement_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            kpiId = rs.getString("kpi_id"),
            kpiCode = rs.getString("kpi_code"),
            periodStart = toInstant(rs.getTimestamp("period_start")) ?: Instant.now(),
            periodEnd = toInstant(rs.getTimestamp("period_end")) ?: Instant.now(),
            actualValue = rs.getDouble("actual_value"),
            numerator = rs.getDouble("numerator"),
            denominator = rs.getDouble("denominator"),
            unit = rs.getString("unit"),
            sampleSize = rs.getInt("sample_size"),
            confidenceState = MeasurementConfidenceState.valueOf(rs.getString("confidence_state")),
            calculationVersion = rs.getString("calculation_version"),
            measuredAt = toInstant(rs.getTimestamp("measured_at")) ?: Instant.now(),
            measuredBy = rs.getString("measured_by")
        )
    }

    private fun mapScorecardItemRow(rs: ResultSet): VendorPerformanceScorecardItem {
        return VendorPerformanceScorecardItem(
            itemId = rs.getString("item_id"),
            scorecardId = rs.getString("scorecard_id"),
            kpiId = rs.getString("kpi_id"),
            kpiCode = rs.getString("kpi_code"),
            kpiName = rs.getString("kpi_name"),
            kpiType = KpiType.valueOf(rs.getString("kpi_type")),
            weight = rs.getDouble("weight"),
            direction = KpiDirection.valueOf(rs.getString("direction")),
            targetValue = rs.getDouble("target_value"),
            actualValue = rs.getDouble("actual_value"),
            normalizedScore = rs.getDouble("normalized_score"),
            weightedScore = rs.getDouble("weighted_score"),
            numerator = rs.getDouble("numerator"),
            denominator = rs.getDouble("denominator"),
            unit = rs.getString("unit"),
            sampleSize = rs.getInt("sample_size"),
            confidenceState = MeasurementConfidenceState.valueOf(rs.getString("confidence_state"))
        )
    }

    private fun mapScorecardRow(rs: ResultSet, items: List<VendorPerformanceScorecardItem> = emptyList()): VendorPerformanceScorecard {
        return VendorPerformanceScorecard(
            projectId = rs.getString("project_id"),
            scorecardId = rs.getString("scorecard_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            periodType = EvaluationPeriodType.valueOf(rs.getString("period_type")),
            periodStart = toInstant(rs.getTimestamp("period_start")) ?: Instant.now(),
            periodEnd = toInstant(rs.getTimestamp("period_end")) ?: Instant.now(),
            overallScore = rs.getDouble("overall_score"),
            rating = PerformanceRating.valueOf(rs.getString("rating")),
            riskLevel = ComplianceRiskLevel.valueOf(rs.getString("risk_level")),
            dataCompleteness = rs.getDouble("data_completeness"),
            sampleSize = rs.getInt("sample_size"),
            calculationVersion = rs.getString("calculation_version"),
            status = ScorecardStatus.valueOf(rs.getString("status")),
            items = items,
            notes = rs.getString("notes"),
            version = rs.getLong("version"),
            generatedAt = toInstant(rs.getTimestamp("generated_at")) ?: Instant.now(),
            generatedBy = rs.getString("generated_by"),
            approvedAt = toInstant(rs.getTimestamp("approved_at")),
            approvedBy = rs.getString("approved_by")
        )
    }

    private fun mapEvaluationCriterionRow(rs: ResultSet): VendorEvaluationCriterion {
        return VendorEvaluationCriterion(
            criterionId = rs.getString("criterion_id"),
            evaluationId = rs.getString("evaluation_id"),
            name = rs.getString("name"),
            category = rs.getString("category"),
            weight = rs.getDouble("weight"),
            score = rs.getDouble("score"),
            comments = rs.getString("comments")
        )
    }

    private fun mapEvaluationRow(rs: ResultSet, criteria: List<VendorEvaluationCriterion> = emptyList()): VendorEvaluation {
        return VendorEvaluation(
            projectId = rs.getString("project_id"),
            evaluationId = rs.getString("evaluation_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            scorecardId = rs.getString("scorecard_id"),
            periodType = EvaluationPeriodType.valueOf(rs.getString("period_type")),
            periodStart = toInstant(rs.getTimestamp("period_start")) ?: Instant.now(),
            periodEnd = toInstant(rs.getTimestamp("period_end")) ?: Instant.now(),
            evaluatorId = rs.getString("evaluator_id"),
            evaluatorName = rs.getString("evaluator_name"),
            status = EvaluationStatus.valueOf(rs.getString("status")),
            decision = rs.getString("decision")?.let { EvaluationDecision.valueOf(it) },
            evaluationScore = rs.getDouble("evaluation_score"),
            rating = PerformanceRating.valueOf(rs.getString("rating")),
            evaluatorComments = rs.getString("evaluator_comments"),
            reviewComments = rs.getString("review_comments"),
            rejectionReason = rs.getString("rejection_reason"),
            criteria = criteria,
            submittedAt = toInstant(rs.getTimestamp("submitted_at")),
            submittedBy = rs.getString("submitted_by"),
            reviewedAt = toInstant(rs.getTimestamp("reviewed_at")),
            reviewedBy = rs.getString("reviewed_by"),
            approvedAt = toInstant(rs.getTimestamp("approved_at")),
            approvedBy = rs.getString("approved_by"),
            finalizedAt = toInstant(rs.getTimestamp("finalized_at")),
            finalizedBy = rs.getString("finalized_by"),
            version = rs.getLong("version"),
            createdAt = toInstant(rs.getTimestamp("created_at")) ?: Instant.now(),
            createdBy = rs.getString("created_by"),
            updatedAt = toInstant(rs.getTimestamp("updated_at")) ?: Instant.now(),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapComplianceRequirementRow(rs: ResultSet): VendorComplianceRequirement {
        return VendorComplianceRequirement(
            projectId = rs.getString("project_id"),
            requirementId = rs.getString("requirement_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            requirementType = ComplianceRequirementType.valueOf(rs.getString("requirement_type")),
            code = rs.getString("code"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            mandatory = rs.getBoolean("mandatory"),
            riskLevel = ComplianceRiskLevel.valueOf(rs.getString("risk_level")),
            validityDays = rs.getObject("validity_days")?.let { (it as Number).toInt() },
            status = ComplianceStatus.valueOf(rs.getString("status")),
            version = rs.getLong("version"),
            createdAt = toInstant(rs.getTimestamp("created_at")) ?: Instant.now(),
            createdBy = rs.getString("created_by"),
            updatedAt = toInstant(rs.getTimestamp("updated_at")) ?: Instant.now(),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapComplianceEvidenceRow(rs: ResultSet): VendorComplianceEvidence {
        return VendorComplianceEvidence(
            projectId = rs.getString("project_id"),
            evidenceId = rs.getString("evidence_id"),
            recordId = rs.getString("record_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            evidenceType = ComplianceEvidenceType.valueOf(rs.getString("evidence_type")),
            fileName = rs.getString("file_name"),
            fileUrl = rs.getString("file_url"),
            checksum = rs.getString("checksum"),
            fileSizeBytes = rs.getLong("file_size_bytes"),
            mimeType = rs.getString("mime_type"),
            uploadedBy = rs.getString("uploaded_by"),
            uploadedAt = toInstant(rs.getTimestamp("uploaded_at")) ?: Instant.now()
        )
    }

    private fun mapComplianceRecordRow(rs: ResultSet, evidence: List<VendorComplianceEvidence> = emptyList()): VendorComplianceRecord {
        return VendorComplianceRecord(
            projectId = rs.getString("project_id"),
            recordId = rs.getString("record_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            requirementId = rs.getString("requirement_id"),
            requirementCode = rs.getString("requirement_code"),
            requirementName = rs.getString("requirement_name"),
            requirementType = ComplianceRequirementType.valueOf(rs.getString("requirement_type")),
            mandatory = rs.getBoolean("mandatory"),
            effectiveDate = toInstant(rs.getTimestamp("effective_date")) ?: Instant.now(),
            expiryDate = toInstant(rs.getTimestamp("expiry_date")),
            status = ComplianceStatus.valueOf(rs.getString("status")),
            riskLevel = ComplianceRiskLevel.valueOf(rs.getString("risk_level")),
            verificationStatus = ComplianceVerificationStatus.valueOf(rs.getString("verification_status")),
            verifiedBy = rs.getString("verified_by"),
            verifiedAt = toInstant(rs.getTimestamp("verified_at")),
            rejectionReason = rs.getString("rejection_reason"),
            notes = rs.getString("notes"),
            evidenceList = evidence,
            version = rs.getLong("version"),
            createdAt = toInstant(rs.getTimestamp("created_at")) ?: Instant.now(),
            createdBy = rs.getString("created_by"),
            updatedAt = toInstant(rs.getTimestamp("updated_at")) ?: Instant.now(),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapCorrectiveActionRow(rs: ResultSet): VendorCorrectiveAction {
        return VendorCorrectiveAction(
            projectId = rs.getString("project_id"),
            actionId = rs.getString("action_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            sourceType = rs.getString("source_type"),
            sourceId = rs.getString("source_id"),
            issueDescription = rs.getString("issue_description"),
            rootCause = rs.getString("root_cause"),
            actionPlan = rs.getString("action_plan"),
            assignedTo = rs.getString("assigned_to"),
            assignedToName = rs.getString("assigned_to_name"),
            priority = CorrectiveActionPriority.valueOf(rs.getString("priority")),
            dueDate = toInstant(rs.getTimestamp("due_date")) ?: Instant.now(),
            status = CorrectiveActionStatus.valueOf(rs.getString("status")),
            startedAt = toInstant(rs.getTimestamp("started_at")),
            completedAt = toInstant(rs.getTimestamp("completed_at")),
            closedAt = toInstant(rs.getTimestamp("closed_at")),
            verificationNotes = rs.getString("verification_notes"),
            verifiedBy = rs.getString("verified_by"),
            verifiedAt = toInstant(rs.getTimestamp("verified_at")),
            version = rs.getLong("version"),
            createdAt = toInstant(rs.getTimestamp("created_at")) ?: Instant.now(),
            createdBy = rs.getString("created_by"),
            updatedAt = toInstant(rs.getTimestamp("updated_at")) ?: Instant.now(),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapRiskIndicatorRow(rs: ResultSet): VendorRiskIndicator {
        return VendorRiskIndicator(
            projectId = rs.getString("project_id"),
            riskId = rs.getString("risk_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            riskType = RiskIndicatorType.valueOf(rs.getString("risk_type")),
            severity = RiskSeverity.valueOf(rs.getString("severity")),
            source = rs.getString("source"),
            sourceId = rs.getString("source_id"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            evidenceReference = rs.getString("evidence_reference"),
            detectedAt = toInstant(rs.getTimestamp("detected_at")) ?: Instant.now(),
            status = RiskStatus.valueOf(rs.getString("status"))
        )
    }

    private fun mapAuditRow(rs: ResultSet): VendorPerformanceAuditEvent {
        return VendorPerformanceAuditEvent(
            projectId = rs.getString("project_id"),
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            eventType = VendorPerformanceAuditEventType.valueOf(rs.getString("event_type")),
            action = rs.getString("action"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            details = rs.getString("details"),
            occurredAt = toInstant(rs.getTimestamp("occurred_at")) ?: Instant.now()
        )
    }

    // --- KPIs ---
    override suspend fun createKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> {
        val tenant = TenantContext(kpi.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_performance_kpis (
                        project_id, kpi_id, tenant_id, code, name, description, kpi_type, measurement_method,
                        target_value, minimum_acceptable_value, maximum_acceptable_value, unit, direction, weight,
                        status, effective_from, effective_to, version, created_at, created_by, updated_at, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, kpi.kpiId)
                    stmt.setString(3, kpi.tenantId)
                    stmt.setString(4, kpi.code)
                    stmt.setString(5, kpi.name)
                    stmt.setString(6, kpi.description)
                    stmt.setString(7, kpi.kpiType.name)
                    stmt.setString(8, kpi.measurementMethod.name)
                    stmt.setDouble(9, kpi.targetValue)
                    if (kpi.minimumAcceptableValue != null) stmt.setDouble(10, kpi.minimumAcceptableValue) else stmt.setNull(10, java.sql.Types.NUMERIC)
                    if (kpi.maximumAcceptableValue != null) stmt.setDouble(11, kpi.maximumAcceptableValue) else stmt.setNull(11, java.sql.Types.NUMERIC)
                    stmt.setString(12, kpi.unit)
                    stmt.setString(13, kpi.direction.name)
                    stmt.setDouble(14, kpi.weight)
                    stmt.setString(15, kpi.status.name)
                    stmt.setTimestamp(16, toTimestamp(kpi.effectiveFrom))
                    stmt.setTimestamp(17, toTimestamp(kpi.effectiveTo))
                    stmt.setLong(18, kpi.version)
                    stmt.setTimestamp(19, toTimestamp(kpi.createdAt))
                    stmt.setString(20, kpi.createdBy)
                    stmt.setTimestamp(21, toTimestamp(kpi.updatedAt))
                    stmt.setString(22, kpi.updatedBy)
                    stmt.executeUpdate()
                }
                kpi
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create KPI")
        }
    }

    override suspend fun updateKpi(kpi: VendorPerformanceKpi): DomainResult<VendorPerformanceKpi> {
        val tenant = TenantContext(kpi.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendor_performance_kpis SET
                        name = ?, description = ?, target_value = ?, minimum_acceptable_value = ?,
                        maximum_acceptable_value = ?, weight = ?, status = ?, effective_to = ?,
                        version = version + 1, updated_at = ?, updated_by = ?
                    WHERE project_id = ? AND kpi_id = ?
                """.trimIndent()
                val rows = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, kpi.name)
                    stmt.setString(2, kpi.description)
                    stmt.setDouble(3, kpi.targetValue)
                    if (kpi.minimumAcceptableValue != null) stmt.setDouble(4, kpi.minimumAcceptableValue) else stmt.setNull(4, java.sql.Types.NUMERIC)
                    if (kpi.maximumAcceptableValue != null) stmt.setDouble(5, kpi.maximumAcceptableValue) else stmt.setNull(5, java.sql.Types.NUMERIC)
                    stmt.setDouble(6, kpi.weight)
                    stmt.setString(7, kpi.status.name)
                    stmt.setTimestamp(8, toTimestamp(kpi.effectiveTo))
                    stmt.setTimestamp(9, toTimestamp(kpi.updatedAt))
                    stmt.setString(10, kpi.updatedBy)
                    stmt.setString(11, tenant.projectId)
                    stmt.setString(12, kpi.kpiId)
                    stmt.executeUpdate()
                }
                if (rows == 0) throw NoSuchElementException("KPI '${kpi.kpiId}' not found")
                kpi
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update KPI")
        }
    }

    override suspend fun findKpiById(projectId: String, kpiId: String): DomainResult<VendorPerformanceKpi> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val kpi = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_performance_kpis WHERE project_id = ? AND kpi_id = ?"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, kpiId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapKpiRow(rs) else null
                }
            }
            if (kpi != null) DomainResult.Success(kpi) else DomainResult.Error(NoSuchElementException("KPI '$kpiId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find KPI by id")
        }
    }

    override suspend fun findKpiByCode(projectId: String, code: String): DomainResult<VendorPerformanceKpi> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val kpi = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_performance_kpis WHERE project_id = ? AND LOWER(code) = LOWER(?)"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, code)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapKpiRow(rs) else null
                }
            }
            if (kpi != null) DomainResult.Success(kpi) else DomainResult.Error(NoSuchElementException("KPI with code '$code' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find KPI by code")
        }
    }

    override suspend fun listKpis(projectId: String, status: KpiStatus?, kpiType: KpiType?): DomainResult<List<VendorPerformanceKpi>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_performance_kpis WHERE project_id = ?")
                if (status != null) sb.append(" AND status = ?")
                if (kpiType != null) sb.append(" AND kpi_type = ?")
                sb.append(" ORDER BY code ASC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    if (status != null) stmt.setString(idx++, status.name)
                    if (kpiType != null) stmt.setString(idx++, kpiType.name)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorPerformanceKpi>()
                    while (rs.next()) {
                        result.add(mapKpiRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list KPIs")
        }
    }

    // --- Measurements ---
    override suspend fun createMeasurement(measurement: VendorPerformanceMeasurement): DomainResult<VendorPerformanceMeasurement> {
        val tenant = TenantContext(measurement.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_performance_measurements (
                        project_id, measurement_id, tenant_id, vendor_id, kpi_id, kpi_code,
                        period_start, period_end, actual_value, numerator, denominator, unit,
                        sample_size, confidence_state, calculation_version, measured_at, measured_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, measurement.measurementId)
                    stmt.setString(3, measurement.tenantId)
                    stmt.setString(4, measurement.vendorId)
                    stmt.setString(5, measurement.kpiId)
                    stmt.setString(6, measurement.kpiCode)
                    stmt.setTimestamp(7, toTimestamp(measurement.periodStart))
                    stmt.setTimestamp(8, toTimestamp(measurement.periodEnd))
                    stmt.setDouble(9, measurement.actualValue)
                    stmt.setDouble(10, measurement.numerator)
                    stmt.setDouble(11, measurement.denominator)
                    stmt.setString(12, measurement.unit)
                    stmt.setInt(13, measurement.sampleSize)
                    stmt.setString(14, measurement.confidenceState.name)
                    stmt.setString(15, measurement.calculationVersion)
                    stmt.setTimestamp(16, toTimestamp(measurement.measuredAt))
                    stmt.setString(17, measurement.measuredBy)
                    stmt.executeUpdate()
                }
                measurement
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create measurement")
        }
    }

    override suspend fun listMeasurements(
        projectId: String,
        vendorId: String,
        kpiId: String?,
        periodStart: Instant?,
        periodEnd: Instant?
    ): DomainResult<List<VendorPerformanceMeasurement>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_performance_measurements WHERE project_id = ? AND vendor_id = ?")
                if (kpiId != null) sb.append(" AND kpi_id = ?")
                if (periodStart != null) sb.append(" AND period_start >= ?")
                if (periodEnd != null) sb.append(" AND period_end <= ?")
                sb.append(" ORDER BY period_start DESC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    stmt.setString(idx++, vendorId)
                    if (kpiId != null) stmt.setString(idx++, kpiId)
                    if (periodStart != null) stmt.setTimestamp(idx++, toTimestamp(periodStart))
                    if (periodEnd != null) stmt.setTimestamp(idx++, toTimestamp(periodEnd))
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorPerformanceMeasurement>()
                    while (rs.next()) {
                        result.add(mapMeasurementRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list measurements")
        }
    }

    // --- Scorecards ---
    override suspend fun createScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard> {
        val tenant = TenantContext(scorecard.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_performance_scorecards (
                        project_id, scorecard_id, tenant_id, vendor_id, period_type, period_start, period_end,
                        overall_score, rating, risk_level, data_completeness, sample_size, calculation_version,
                        status, notes, version, generated_at, generated_by, approved_at, approved_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, scorecard.scorecardId)
                    stmt.setString(3, scorecard.tenantId)
                    stmt.setString(4, scorecard.vendorId)
                    stmt.setString(5, scorecard.periodType.name)
                    stmt.setTimestamp(6, toTimestamp(scorecard.periodStart))
                    stmt.setTimestamp(7, toTimestamp(scorecard.periodEnd))
                    stmt.setDouble(8, scorecard.overallScore)
                    stmt.setString(9, scorecard.rating.name)
                    stmt.setString(10, scorecard.riskLevel.name)
                    stmt.setDouble(11, scorecard.dataCompleteness)
                    stmt.setInt(12, scorecard.sampleSize)
                    stmt.setString(13, scorecard.calculationVersion)
                    stmt.setString(14, scorecard.status.name)
                    stmt.setString(15, scorecard.notes)
                    stmt.setLong(16, scorecard.version)
                    stmt.setTimestamp(17, toTimestamp(scorecard.generatedAt))
                    stmt.setString(18, scorecard.generatedBy)
                    stmt.setTimestamp(19, toTimestamp(scorecard.approvedAt))
                    stmt.setString(20, scorecard.approvedBy)
                    stmt.executeUpdate()
                }

                if (scorecard.items.isNotEmpty()) {
                    val itemSql = """
                        INSERT INTO vendor_performance_scorecard_items (
                            project_id, item_id, scorecard_id, tenant_id, kpi_id, kpi_code, kpi_name, kpi_type,
                            weight, direction, target_value, actual_value, normalized_score, weighted_score,
                            numerator, denominator, unit, sample_size, confidence_state
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    ctx.connection.prepareStatement(itemSql).use { stmt ->
                        for (it in scorecard.items) {
                            stmt.setString(1, tenant.projectId)
                            stmt.setString(2, it.itemId)
                            stmt.setString(3, scorecard.scorecardId)
                            stmt.setString(4, scorecard.tenantId)
                            stmt.setString(5, it.kpiId)
                            stmt.setString(6, it.kpiCode)
                            stmt.setString(7, it.kpiName)
                            stmt.setString(8, it.kpiType.name)
                            stmt.setDouble(9, it.weight)
                            stmt.setString(10, it.direction.name)
                            stmt.setDouble(11, it.targetValue)
                            stmt.setDouble(12, it.actualValue)
                            stmt.setDouble(13, it.normalizedScore)
                            stmt.setDouble(14, it.weightedScore)
                            stmt.setDouble(15, it.numerator)
                            stmt.setDouble(16, it.denominator)
                            stmt.setString(17, it.unit)
                            stmt.setInt(18, it.sampleSize)
                            stmt.setString(19, it.confidenceState.name)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                }
                scorecard
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create scorecard")
        }
    }

    override suspend fun updateScorecard(scorecard: VendorPerformanceScorecard): DomainResult<VendorPerformanceScorecard> {
        val tenant = TenantContext(scorecard.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendor_performance_scorecards SET
                        status = ?, approved_at = ?, approved_by = ?, notes = ?,
                        version = version + 1
                    WHERE project_id = ? AND scorecard_id = ?
                """.trimIndent()
                val rows = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, scorecard.status.name)
                    stmt.setTimestamp(2, toTimestamp(scorecard.approvedAt))
                    stmt.setString(3, scorecard.approvedBy)
                    stmt.setString(4, scorecard.notes)
                    stmt.setString(5, tenant.projectId)
                    stmt.setString(6, scorecard.scorecardId)
                    stmt.executeUpdate()
                }
                if (rows == 0) throw NoSuchElementException("Scorecard '${scorecard.scorecardId}' not found")
                scorecard
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update scorecard")
        }
    }

    override suspend fun findScorecardById(projectId: String, scorecardId: String): DomainResult<VendorPerformanceScorecard> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val sc = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_performance_scorecards WHERE project_id = ? AND scorecard_id = ?"
                val raw = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, scorecardId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapScorecardRow(rs) else null
                }
                if (raw != null) {
                    val itemSql = "SELECT * FROM vendor_performance_scorecard_items WHERE project_id = ? AND scorecard_id = ?"
                    val items = ctx.connection.prepareStatement(itemSql).use { stmt ->
                        stmt.setString(1, tenant.projectId)
                        stmt.setString(2, scorecardId)
                        val rs = stmt.executeQuery()
                        val list = mutableListOf<VendorPerformanceScorecardItem>()
                        while (rs.next()) {
                            list.add(mapScorecardItemRow(rs))
                        }
                        list
                    }
                    raw.copy(items = items)
                } else null
            }
            if (sc != null) DomainResult.Success(sc) else DomainResult.Error(NoSuchElementException("Scorecard '$scorecardId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find scorecard by id")
        }
    }

    override suspend fun listScorecards(projectId: String, vendorId: String, status: ScorecardStatus?): DomainResult<List<VendorPerformanceScorecard>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_performance_scorecards WHERE project_id = ? AND vendor_id = ?")
                if (status != null) sb.append(" AND status = ?")
                sb.append(" ORDER BY period_end DESC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    stmt.setString(idx++, vendorId)
                    if (status != null) stmt.setString(idx++, status.name)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorPerformanceScorecard>()
                    while (rs.next()) {
                        result.add(mapScorecardRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list scorecards")
        }
    }

    override fun observeScorecards(projectId: String, vendorId: String?): Flow<List<VendorPerformanceScorecard>> {
        val key = "$projectId:$vendorId"
        return scorecardFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    // --- Evaluations ---
    override suspend fun createEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation> {
        val tenant = TenantContext(evaluation.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_evaluations (
                        project_id, evaluation_id, tenant_id, vendor_id, scorecard_id, period_type, period_start, period_end,
                        evaluator_id, evaluator_name, status, decision, evaluation_score, rating, evaluator_comments,
                        review_comments, rejection_reason, submitted_at, submitted_by, reviewed_at, reviewed_by,
                        approved_at, approved_by, finalized_at, finalized_by, version, created_at, created_by, updated_at, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, evaluation.evaluationId)
                    stmt.setString(3, evaluation.tenantId)
                    stmt.setString(4, evaluation.vendorId)
                    stmt.setString(5, evaluation.scorecardId)
                    stmt.setString(6, evaluation.periodType.name)
                    stmt.setTimestamp(7, toTimestamp(evaluation.periodStart))
                    stmt.setTimestamp(8, toTimestamp(evaluation.periodEnd))
                    stmt.setString(9, evaluation.evaluatorId)
                    stmt.setString(10, evaluation.evaluatorName)
                    stmt.setString(11, evaluation.status.name)
                    stmt.setString(12, evaluation.decision?.name)
                    stmt.setDouble(13, evaluation.evaluationScore)
                    stmt.setString(14, evaluation.rating.name)
                    stmt.setString(15, evaluation.evaluatorComments)
                    stmt.setString(16, evaluation.reviewComments)
                    stmt.setString(17, evaluation.rejectionReason)
                    stmt.setTimestamp(18, toTimestamp(evaluation.submittedAt))
                    stmt.setString(19, evaluation.submittedBy)
                    stmt.setTimestamp(20, toTimestamp(evaluation.reviewedAt))
                    stmt.setString(21, evaluation.reviewedBy)
                    stmt.setTimestamp(22, toTimestamp(evaluation.approvedAt))
                    stmt.setString(23, evaluation.approvedBy)
                    stmt.setTimestamp(24, toTimestamp(evaluation.finalizedAt))
                    stmt.setString(25, evaluation.finalizedBy)
                    stmt.setLong(26, evaluation.version)
                    stmt.setTimestamp(27, toTimestamp(evaluation.createdAt))
                    stmt.setString(28, evaluation.createdBy)
                    stmt.setTimestamp(29, toTimestamp(evaluation.updatedAt))
                    stmt.setString(30, evaluation.updatedBy)
                    stmt.executeUpdate()
                }

                if (evaluation.criteria.isNotEmpty()) {
                    val critSql = """
                        INSERT INTO vendor_evaluation_criteria (
                            project_id, criterion_id, evaluation_id, tenant_id, name, category, weight, score, comments
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    ctx.connection.prepareStatement(critSql).use { stmt ->
                        for (c in evaluation.criteria) {
                            stmt.setString(1, tenant.projectId)
                            stmt.setString(2, c.criterionId)
                            stmt.setString(3, evaluation.evaluationId)
                            stmt.setString(4, evaluation.tenantId)
                            stmt.setString(5, c.name)
                            stmt.setString(6, c.category)
                            stmt.setDouble(7, c.weight)
                            stmt.setDouble(8, c.score)
                            stmt.setString(9, c.comments)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                }
                evaluation
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create evaluation")
        }
    }

    override suspend fun updateEvaluation(evaluation: VendorEvaluation): DomainResult<VendorEvaluation> {
        val tenant = TenantContext(evaluation.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendor_evaluations SET
                        status = ?, decision = ?, review_comments = ?, rejection_reason = ?,
                        submitted_at = ?, submitted_by = ?, reviewed_at = ?, reviewed_by = ?,
                        approved_at = ?, approved_by = ?, finalized_at = ?, finalized_by = ?,
                        version = version + 1, updated_at = ?, updated_by = ?
                    WHERE project_id = ? AND evaluation_id = ?
                """.trimIndent()
                val rows = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, evaluation.status.name)
                    stmt.setString(2, evaluation.decision?.name)
                    stmt.setString(3, evaluation.reviewComments)
                    stmt.setString(4, evaluation.rejectionReason)
                    stmt.setTimestamp(5, toTimestamp(evaluation.submittedAt))
                    stmt.setString(6, evaluation.submittedBy)
                    stmt.setTimestamp(7, toTimestamp(evaluation.reviewedAt))
                    stmt.setString(8, evaluation.reviewedBy)
                    stmt.setTimestamp(9, toTimestamp(evaluation.approvedAt))
                    stmt.setString(10, evaluation.approvedBy)
                    stmt.setTimestamp(11, toTimestamp(evaluation.finalizedAt))
                    stmt.setString(12, evaluation.finalizedBy)
                    stmt.setTimestamp(13, toTimestamp(evaluation.updatedAt))
                    stmt.setString(14, evaluation.updatedBy)
                    stmt.setString(15, tenant.projectId)
                    stmt.setString(16, evaluation.evaluationId)
                    stmt.executeUpdate()
                }
                if (rows == 0) throw NoSuchElementException("Evaluation '${evaluation.evaluationId}' not found")
                evaluation
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update evaluation")
        }
    }

    override suspend fun findEvaluationById(projectId: String, evaluationId: String): DomainResult<VendorEvaluation> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val eval = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_evaluations WHERE project_id = ? AND evaluation_id = ?"
                val raw = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, evaluationId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapEvaluationRow(rs) else null
                }
                if (raw != null) {
                    val critSql = "SELECT * FROM vendor_evaluation_criteria WHERE project_id = ? AND evaluation_id = ?"
                    val criteria = ctx.connection.prepareStatement(critSql).use { stmt ->
                        stmt.setString(1, tenant.projectId)
                        stmt.setString(2, evaluationId)
                        val rs = stmt.executeQuery()
                        val list = mutableListOf<VendorEvaluationCriterion>()
                        while (rs.next()) {
                            list.add(mapEvaluationCriterionRow(rs))
                        }
                        list
                    }
                    raw.copy(criteria = criteria)
                } else null
            }
            if (eval != null) DomainResult.Success(eval) else DomainResult.Error(NoSuchElementException("Evaluation '$evaluationId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find evaluation by id")
        }
    }

    override suspend fun listEvaluations(projectId: String, vendorId: String?, status: EvaluationStatus?): DomainResult<List<VendorEvaluation>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_evaluations WHERE project_id = ?")
                if (vendorId != null) sb.append(" AND vendor_id = ?")
                if (status != null) sb.append(" AND status = ?")
                sb.append(" ORDER BY period_end DESC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    if (vendorId != null) stmt.setString(idx++, vendorId)
                    if (status != null) stmt.setString(idx++, status.name)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorEvaluation>()
                    while (rs.next()) {
                        result.add(mapEvaluationRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list evaluations")
        }
    }

    override fun observeEvaluations(projectId: String, vendorId: String?): Flow<List<VendorEvaluation>> {
        val key = "$projectId:$vendorId"
        return evaluationFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    // --- Compliance Requirements ---
    override suspend fun createComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> {
        val tenant = TenantContext(requirement.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_compliance_requirements (
                        project_id, requirement_id, tenant_id, requirement_type, code, name, description,
                        mandatory, risk_level, validity_days, status, version, created_at, created_by, updated_at, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, requirement.requirementId)
                    stmt.setString(3, requirement.tenantId)
                    stmt.setString(4, requirement.requirementType.name)
                    stmt.setString(5, requirement.code)
                    stmt.setString(6, requirement.name)
                    stmt.setString(7, requirement.description)
                    stmt.setBoolean(8, requirement.mandatory)
                    stmt.setString(9, requirement.riskLevel.name)
                    if (requirement.validityDays != null) stmt.setInt(10, requirement.validityDays) else stmt.setNull(10, java.sql.Types.INTEGER)
                    stmt.setString(11, requirement.status.name)
                    stmt.setLong(12, requirement.version)
                    stmt.setTimestamp(13, toTimestamp(requirement.createdAt))
                    stmt.setString(14, requirement.createdBy)
                    stmt.setTimestamp(15, toTimestamp(requirement.updatedAt))
                    stmt.setString(16, requirement.updatedBy)
                    stmt.executeUpdate()
                }
                requirement
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create compliance requirement")
        }
    }

    override suspend fun updateComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<VendorComplianceRequirement> {
        val tenant = TenantContext(requirement.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendor_compliance_requirements SET
                        name = ?, description = ?, mandatory = ?, risk_level = ?, validity_days = ?, status = ?,
                        version = version + 1, updated_at = ?, updated_by = ?
                    WHERE project_id = ? AND requirement_id = ?
                """.trimIndent()
                val rows = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, requirement.name)
                    stmt.setString(2, requirement.description)
                    stmt.setBoolean(3, requirement.mandatory)
                    stmt.setString(4, requirement.riskLevel.name)
                    if (requirement.validityDays != null) stmt.setInt(5, requirement.validityDays) else stmt.setNull(5, java.sql.Types.INTEGER)
                    stmt.setString(6, requirement.status.name)
                    stmt.setTimestamp(7, toTimestamp(requirement.updatedAt))
                    stmt.setString(8, requirement.updatedBy)
                    stmt.setString(9, tenant.projectId)
                    stmt.setString(10, requirement.requirementId)
                    stmt.executeUpdate()
                }
                if (rows == 0) throw NoSuchElementException("Compliance requirement '${requirement.requirementId}' not found")
                requirement
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update compliance requirement")
        }
    }

    override suspend fun findComplianceRequirementById(projectId: String, requirementId: String): DomainResult<VendorComplianceRequirement> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val req = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_compliance_requirements WHERE project_id = ? AND requirement_id = ?"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, requirementId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapComplianceRequirementRow(rs) else null
                }
            }
            if (req != null) DomainResult.Success(req) else DomainResult.Error(NoSuchElementException("Compliance requirement '$requirementId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find compliance requirement by id")
        }
    }

    override suspend fun findComplianceRequirementByCode(projectId: String, code: String): DomainResult<VendorComplianceRequirement> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val req = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_compliance_requirements WHERE project_id = ? AND LOWER(code) = LOWER(?)"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, code)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapComplianceRequirementRow(rs) else null
                }
            }
            if (req != null) DomainResult.Success(req) else DomainResult.Error(NoSuchElementException("Compliance requirement with code '$code' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find compliance requirement by code")
        }
    }

    override suspend fun listComplianceRequirements(projectId: String, status: ComplianceStatus?): DomainResult<List<VendorComplianceRequirement>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_compliance_requirements WHERE project_id = ?")
                if (status != null) sb.append(" AND status = ?")
                sb.append(" ORDER BY code ASC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    if (status != null) stmt.setString(idx++, status.name)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorComplianceRequirement>()
                    while (rs.next()) {
                        result.add(mapComplianceRequirementRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list compliance requirements")
        }
    }

    // --- Compliance Records ---
    override suspend fun createComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord> {
        val tenant = TenantContext(record.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_compliance_records (
                        project_id, record_id, tenant_id, vendor_id, requirement_id, requirement_code, requirement_name,
                        requirement_type, mandatory, effective_date, expiry_date, status, risk_level, verification_status,
                        verified_by, verified_at, rejection_reason, notes, version, created_at, created_by, updated_at, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, record.recordId)
                    stmt.setString(3, record.tenantId)
                    stmt.setString(4, record.vendorId)
                    stmt.setString(5, record.requirementId)
                    stmt.setString(6, record.requirementCode)
                    stmt.setString(7, record.requirementName)
                    stmt.setString(8, record.requirementType.name)
                    stmt.setBoolean(9, record.mandatory)
                    stmt.setTimestamp(10, toTimestamp(record.effectiveDate))
                    stmt.setTimestamp(11, toTimestamp(record.expiryDate))
                    stmt.setString(12, record.status.name)
                    stmt.setString(13, record.riskLevel.name)
                    stmt.setString(14, record.verificationStatus.name)
                    stmt.setString(15, record.verifiedBy)
                    stmt.setTimestamp(16, toTimestamp(record.verifiedAt))
                    stmt.setString(17, record.rejectionReason)
                    stmt.setString(18, record.notes)
                    stmt.setLong(19, record.version)
                    stmt.setTimestamp(20, toTimestamp(record.createdAt))
                    stmt.setString(21, record.createdBy)
                    stmt.setTimestamp(22, toTimestamp(record.updatedAt))
                    stmt.setString(23, record.updatedBy)
                    stmt.executeUpdate()
                }

                if (record.evidenceList.isNotEmpty()) {
                    val evSql = """
                        INSERT INTO vendor_compliance_evidence (
                            project_id, evidence_id, record_id, tenant_id, evidence_type, file_name, file_url,
                            checksum, file_size_bytes, mime_type, uploaded_by, uploaded_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    ctx.connection.prepareStatement(evSql).use { stmt ->
                        for (ev in record.evidenceList) {
                            stmt.setString(1, tenant.projectId)
                            stmt.setString(2, ev.evidenceId)
                            stmt.setString(3, record.recordId)
                            stmt.setString(4, record.tenantId)
                            stmt.setString(5, ev.evidenceType.name)
                            stmt.setString(6, ev.fileName)
                            stmt.setString(7, ev.fileUrl)
                            stmt.setString(8, ev.checksum)
                            stmt.setLong(9, ev.fileSizeBytes)
                            stmt.setString(10, ev.mimeType)
                            stmt.setString(11, ev.uploadedBy)
                            stmt.setTimestamp(12, toTimestamp(ev.uploadedAt))
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                }
                record
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create compliance record")
        }
    }

    override suspend fun updateComplianceRecord(record: VendorComplianceRecord): DomainResult<VendorComplianceRecord> {
        val tenant = TenantContext(record.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendor_compliance_records SET
                        status = ?, risk_level = ?, verification_status = ?, verified_by = ?, verified_at = ?,
                        rejection_reason = ?, notes = ?, version = version + 1, updated_at = ?, updated_by = ?
                    WHERE project_id = ? AND record_id = ?
                """.trimIndent()
                val rows = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, record.status.name)
                    stmt.setString(2, record.riskLevel.name)
                    stmt.setString(3, record.verificationStatus.name)
                    stmt.setString(4, record.verifiedBy)
                    stmt.setTimestamp(5, toTimestamp(record.verifiedAt))
                    stmt.setString(6, record.rejectionReason)
                    stmt.setString(7, record.notes)
                    stmt.setTimestamp(8, toTimestamp(record.updatedAt))
                    stmt.setString(9, record.updatedBy)
                    stmt.setString(10, tenant.projectId)
                    stmt.setString(11, record.recordId)
                    stmt.executeUpdate()
                }
                if (rows == 0) throw NoSuchElementException("Compliance record '${record.recordId}' not found")
                record
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update compliance record")
        }
    }

    override suspend fun findComplianceRecordById(projectId: String, recordId: String): DomainResult<VendorComplianceRecord> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val rec = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_compliance_records WHERE project_id = ? AND record_id = ?"
                val raw = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, recordId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapComplianceRecordRow(rs) else null
                }
                if (raw != null) {
                    val evSql = "SELECT * FROM vendor_compliance_evidence WHERE project_id = ? AND record_id = ?"
                    val evidence = ctx.connection.prepareStatement(evSql).use { stmt ->
                        stmt.setString(1, tenant.projectId)
                        stmt.setString(2, recordId)
                        val rs = stmt.executeQuery()
                        val list = mutableListOf<VendorComplianceEvidence>()
                        while (rs.next()) {
                            list.add(mapComplianceEvidenceRow(rs))
                        }
                        list
                    }
                    raw.copy(evidenceList = evidence)
                } else null
            }
            if (rec != null) DomainResult.Success(rec) else DomainResult.Error(NoSuchElementException("Compliance record '$recordId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find compliance record by id")
        }
    }

    override suspend fun listComplianceRecords(projectId: String, vendorId: String?, status: ComplianceStatus?): DomainResult<List<VendorComplianceRecord>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_compliance_records WHERE project_id = ?")
                if (vendorId != null) sb.append(" AND vendor_id = ?")
                if (status != null) sb.append(" AND status = ?")
                sb.append(" ORDER BY created_at DESC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    if (vendorId != null) stmt.setString(idx++, vendorId)
                    if (status != null) stmt.setString(idx++, status.name)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorComplianceRecord>()
                    while (rs.next()) {
                        result.add(mapComplianceRecordRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list compliance records")
        }
    }

    override suspend fun addComplianceEvidence(evidence: VendorComplianceEvidence): DomainResult<VendorComplianceEvidence> {
        val tenant = TenantContext(evidence.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_compliance_evidence (
                        project_id, evidence_id, record_id, tenant_id, evidence_type, file_name, file_url,
                        checksum, file_size_bytes, mime_type, uploaded_by, uploaded_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, evidence.evidenceId)
                    stmt.setString(3, evidence.recordId)
                    stmt.setString(4, evidence.tenantId)
                    stmt.setString(5, evidence.evidenceType.name)
                    stmt.setString(6, evidence.fileName)
                    stmt.setString(7, evidence.fileUrl)
                    stmt.setString(8, evidence.checksum)
                    stmt.setLong(9, evidence.fileSizeBytes)
                    stmt.setString(10, evidence.mimeType)
                    stmt.setString(11, evidence.uploadedBy)
                    stmt.setTimestamp(12, toTimestamp(evidence.uploadedAt))
                    stmt.executeUpdate()
                }
                evidence
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "add compliance evidence")
        }
    }

    override suspend fun listComplianceEvidence(projectId: String, recordId: String): DomainResult<List<VendorComplianceEvidence>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_compliance_evidence WHERE project_id = ? AND record_id = ?"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, recordId)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorComplianceEvidence>()
                    while (rs.next()) {
                        result.add(mapComplianceEvidenceRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list compliance evidence")
        }
    }

    // --- Corrective Actions ---
    override suspend fun createCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction> {
        val tenant = TenantContext(action.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_corrective_actions (
                        project_id, action_id, tenant_id, vendor_id, source_type, source_id, issue_description,
                        root_cause, action_plan, assigned_to, assigned_to_name, priority, due_date, status,
                        started_at, completed_at, closed_at, verification_notes, verified_by, verified_at,
                        version, created_at, created_by, updated_at, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, action.actionId)
                    stmt.setString(3, action.tenantId)
                    stmt.setString(4, action.vendorId)
                    stmt.setString(5, action.sourceType)
                    stmt.setString(6, action.sourceId)
                    stmt.setString(7, action.issueDescription)
                    stmt.setString(8, action.rootCause)
                    stmt.setString(9, action.actionPlan)
                    stmt.setString(10, action.assignedTo)
                    stmt.setString(11, action.assignedToName)
                    stmt.setString(12, action.priority.name)
                    stmt.setTimestamp(13, toTimestamp(action.dueDate))
                    stmt.setString(14, action.status.name)
                    stmt.setTimestamp(15, toTimestamp(action.startedAt))
                    stmt.setTimestamp(16, toTimestamp(action.completedAt))
                    stmt.setTimestamp(17, toTimestamp(action.closedAt))
                    stmt.setString(18, action.verificationNotes)
                    stmt.setString(19, action.verifiedBy)
                    stmt.setTimestamp(20, toTimestamp(action.verifiedAt))
                    stmt.setLong(21, action.version)
                    stmt.setTimestamp(22, toTimestamp(action.createdAt))
                    stmt.setString(23, action.createdBy)
                    stmt.setTimestamp(24, toTimestamp(action.updatedAt))
                    stmt.setString(25, action.updatedBy)
                    stmt.executeUpdate()
                }
                action
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create corrective action")
        }
    }

    override suspend fun updateCorrectiveAction(action: VendorCorrectiveAction): DomainResult<VendorCorrectiveAction> {
        val tenant = TenantContext(action.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendor_corrective_actions SET
                        status = ?, started_at = ?, completed_at = ?, closed_at = ?,
                        verification_notes = ?, verified_by = ?, verified_at = ?,
                        version = version + 1, updated_at = ?, updated_by = ?
                    WHERE project_id = ? AND action_id = ?
                """.trimIndent()
                val rows = ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, action.status.name)
                    stmt.setTimestamp(2, toTimestamp(action.startedAt))
                    stmt.setTimestamp(3, toTimestamp(action.completedAt))
                    stmt.setTimestamp(4, toTimestamp(action.closedAt))
                    stmt.setString(5, action.verificationNotes)
                    stmt.setString(6, action.verifiedBy)
                    stmt.setTimestamp(7, toTimestamp(action.verifiedAt))
                    stmt.setTimestamp(8, toTimestamp(action.updatedAt))
                    stmt.setString(9, action.updatedBy)
                    stmt.setString(10, tenant.projectId)
                    stmt.setString(11, action.actionId)
                    stmt.executeUpdate()
                }
                if (rows == 0) throw NoSuchElementException("Corrective action '${action.actionId}' not found")
                action
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update corrective action")
        }
    }

    override suspend fun findCorrectiveActionById(projectId: String, actionId: String): DomainResult<VendorCorrectiveAction> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val act = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_corrective_actions WHERE project_id = ? AND action_id = ?"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, actionId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapCorrectiveActionRow(rs) else null
                }
            }
            if (act != null) DomainResult.Success(act) else DomainResult.Error(NoSuchElementException("Corrective action '$actionId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find corrective action by id")
        }
    }

    override suspend fun listCorrectiveActions(projectId: String, vendorId: String?, status: CorrectiveActionStatus?): DomainResult<List<VendorCorrectiveAction>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_corrective_actions WHERE project_id = ?")
                if (vendorId != null) sb.append(" AND vendor_id = ?")
                if (status != null) sb.append(" AND status = ?")
                sb.append(" ORDER BY due_date ASC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    if (vendorId != null) stmt.setString(idx++, vendorId)
                    if (status != null) stmt.setString(idx++, status.name)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorCorrectiveAction>()
                    while (rs.next()) {
                        result.add(mapCorrectiveActionRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list corrective actions")
        }
    }

    override fun observeCorrectiveActions(projectId: String, vendorId: String?): Flow<List<VendorCorrectiveAction>> {
        val key = "$projectId:$vendorId"
        return correctiveActionFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    // --- Risk Indicators & Audit Trail ---
    override suspend fun createRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator> {
        val tenant = TenantContext(risk.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_performance_risk_indicators (
                        project_id, risk_id, tenant_id, vendor_id, risk_type, severity,
                        source, source_id, title, description, evidence_reference, detected_at, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, risk.riskId)
                    stmt.setString(3, risk.tenantId)
                    stmt.setString(4, risk.vendorId)
                    stmt.setString(5, risk.riskType.name)
                    stmt.setString(6, risk.severity.name)
                    stmt.setString(7, risk.source)
                    stmt.setString(8, risk.sourceId)
                    stmt.setString(9, risk.title)
                    stmt.setString(10, risk.description)
                    stmt.setString(11, risk.evidenceReference)
                    stmt.setTimestamp(12, toTimestamp(risk.detectedAt))
                    stmt.setString(13, risk.status.name)
                    stmt.executeUpdate()
                }
                risk
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create risk indicator")
        }
    }

    override suspend fun updateRiskIndicator(risk: VendorRiskIndicator): DomainResult<VendorRiskIndicator> {
        val tenant = TenantContext(risk.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = "UPDATE vendor_performance_risk_indicators SET status = ? WHERE project_id = ? AND risk_id = ?"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, risk.status.name)
                    stmt.setString(2, tenant.projectId)
                    stmt.setString(3, risk.riskId)
                    stmt.executeUpdate()
                }
                risk
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update risk indicator")
        }
    }

    override suspend fun listRiskIndicators(projectId: String, vendorId: String?, status: RiskStatus?): DomainResult<List<VendorRiskIndicator>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_performance_risk_indicators WHERE project_id = ?")
                if (vendorId != null) sb.append(" AND vendor_id = ?")
                if (status != null) sb.append(" AND status = ?")
                sb.append(" ORDER BY detected_at DESC")
                ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenant.projectId)
                    if (vendorId != null) stmt.setString(idx++, vendorId)
                    if (status != null) stmt.setString(idx++, status.name)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorRiskIndicator>()
                    while (rs.next()) {
                        result.add(mapRiskIndicatorRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list risk indicators")
        }
    }

    override suspend fun appendAuditEvent(event: VendorPerformanceAuditEvent): DomainResult<VendorPerformanceAuditEvent> {
        val tenant = TenantContext(event.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_performance_audit_events (
                        project_id, audit_id, tenant_id, entity_type, entity_id, event_type,
                        action, actor_id, actor_role, details, occurred_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, event.auditId)
                    stmt.setString(3, event.tenantId)
                    stmt.setString(4, event.entityType)
                    stmt.setString(5, event.entityId)
                    stmt.setString(6, event.eventType.name)
                    stmt.setString(7, event.action)
                    stmt.setString(8, event.actorId)
                    stmt.setString(9, event.actorRole)
                    stmt.setString(10, event.details)
                    stmt.setTimestamp(11, toTimestamp(event.occurredAt))
                    stmt.executeUpdate()
                }
                event
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "append audit event")
        }
    }

    override suspend fun listAuditEvents(projectId: String, entityId: String): DomainResult<List<VendorPerformanceAuditEvent>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_performance_audit_events WHERE project_id = ? AND entity_id = ? ORDER BY occurred_at ASC"
                ctx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenant.projectId)
                    stmt.setString(2, entityId)
                    val rs = stmt.executeQuery()
                    val result = mutableListOf<VendorPerformanceAuditEvent>()
                    while (rs.next()) {
                        result.add(mapAuditRow(rs))
                    }
                    result
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list audit events")
        }
    }
}
