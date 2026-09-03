package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateProgramDataSource
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateActorType
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditEventType
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramEntityCategory
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramGovernanceSummary
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramOutboxEvent
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus
import java.sql.ResultSet

/**
 * PostgreSQL implementation of AffiliateProgramDataSource with RLS and Multi-Tenant Isolation (Module 20 Step 02).
 */
class PostgresAffiliateProgramDataSource(
    private val transactionManager: TransactionManager
) : AffiliateProgramDataSource {

    override suspend fun saveProgram(program: AffiliateProgram): AffiliateProgram {
        return transactionManager.inTransaction(TenantContext(program.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_programs (
                    program_id, tenant_id, program_code, program_name, description, status,
                    start_date, end_date, eligibility_policy, terms_reference, terms_version,
                    max_participants, created_by, created_at, updated_at, version, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, program_id) DO UPDATE SET
                    program_name = EXCLUDED.program_name,
                    description = EXCLUDED.description,
                    status = EXCLUDED.status,
                    start_date = EXCLUDED.start_date,
                    end_date = EXCLUDED.end_date,
                    eligibility_policy = EXCLUDED.eligibility_policy,
                    terms_reference = EXCLUDED.terms_reference,
                    terms_version = EXCLUDED.terms_version,
                    max_participants = EXCLUDED.max_participants,
                    updated_at = EXCLUDED.updated_at,
                    version = affiliate_programs.version + 1,
                    metadata_json = EXCLUDED.metadata_json
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, program.programId)
                stmt.setString(2, program.tenantId)
                stmt.setString(3, program.programCode)
                stmt.setString(4, program.programName)
                stmt.setString(5, program.description)
                stmt.setString(6, program.status.name)
                stmt.setLong(7, program.startDate)
                stmt.setObject(8, program.endDate)
                stmt.setString(9, program.eligibilityPolicy)
                stmt.setString(10, program.termsReference)
                stmt.setString(11, program.termsVersion)
                stmt.setObject(12, program.maxParticipants)
                stmt.setString(13, program.createdBy)
                stmt.setLong(14, program.createdAt)
                stmt.setLong(15, program.updatedAt)
                stmt.setLong(16, program.version)
                stmt.setString(17, program.metadataJson)
                stmt.executeUpdate()
            }
            program
        }
    }

    override suspend fun findProgramById(tenantId: String, programId: String): AffiliateProgram? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_programs WHERE tenant_id = ? AND program_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, programId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateProgram(rs) else null
                }
            }
        }
    }

    override suspend fun findProgramByCode(tenantId: String, programCode: String): AffiliateProgram? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_programs WHERE tenant_id = ? AND UPPER(program_code) = UPPER(?)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, programCode)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateProgram(rs) else null
                }
            }
        }
    }

    override suspend fun listPrograms(tenantId: String, status: AffiliateProgramStatus?): List<AffiliateProgram> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sb = StringBuilder("SELECT * FROM affiliate_programs WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)

            if (status != null) {
                sb.append(" AND status = ?")
                params.add(status.name)
            }
            sb.append(" ORDER BY created_at DESC")

            conn.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<AffiliateProgram>()
                    while (rs.next()) {
                        results.add(mapAffiliateProgram(rs))
                    }
                    results
                }
            }
        }
    }

    override suspend fun saveEnrollment(enrollment: AffiliateEnrollment): AffiliateEnrollment {
        return transactionManager.inTransaction(TenantContext(enrollment.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_enrollments (
                    enrollment_id, tenant_id, affiliate_id, program_id, enrollment_status,
                    effective_from, effective_to, enrollment_reason, requested_at, approved_by,
                    approved_at, rejected_by, rejected_at, rejection_reason, suspended_by,
                    suspended_at, suspension_reason, terminated_by, terminated_at,
                    termination_reason, created_at, updated_at, version, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, enrollment_id) DO UPDATE SET
                    enrollment_status = EXCLUDED.enrollment_status,
                    effective_from = EXCLUDED.effective_from,
                    effective_to = EXCLUDED.effective_to,
                    enrollment_reason = EXCLUDED.enrollment_reason,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    rejected_by = EXCLUDED.rejected_by,
                    rejected_at = EXCLUDED.rejected_at,
                    rejection_reason = EXCLUDED.rejection_reason,
                    suspended_by = EXCLUDED.suspended_by,
                    suspended_at = EXCLUDED.suspended_at,
                    suspension_reason = EXCLUDED.suspension_reason,
                    terminated_by = EXCLUDED.terminated_by,
                    terminated_at = EXCLUDED.terminated_at,
                    termination_reason = EXCLUDED.termination_reason,
                    updated_at = EXCLUDED.updated_at,
                    version = affiliate_enrollments.version + 1,
                    metadata_json = EXCLUDED.metadata_json
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, enrollment.enrollmentId)
                stmt.setString(2, enrollment.tenantId)
                stmt.setString(3, enrollment.affiliateId)
                stmt.setString(4, enrollment.programId)
                stmt.setString(5, enrollment.enrollmentStatus.name)
                stmt.setObject(6, enrollment.effectiveFrom)
                stmt.setObject(7, enrollment.effectiveTo)
                stmt.setString(8, enrollment.enrollmentReason)
                stmt.setLong(9, enrollment.requestedAt)
                stmt.setString(10, enrollment.approvedBy)
                stmt.setObject(11, enrollment.approvedAt)
                stmt.setString(12, enrollment.rejectedBy)
                stmt.setObject(13, enrollment.rejectedAt)
                stmt.setString(14, enrollment.rejectionReason)
                stmt.setString(15, enrollment.suspendedBy)
                stmt.setObject(16, enrollment.suspendedAt)
                stmt.setString(17, enrollment.suspensionReason)
                stmt.setString(18, enrollment.terminatedBy)
                stmt.setObject(19, enrollment.terminatedAt)
                stmt.setString(20, enrollment.terminationReason)
                stmt.setLong(21, enrollment.createdAt)
                stmt.setLong(22, enrollment.updatedAt)
                stmt.setLong(23, enrollment.version)
                stmt.setString(24, enrollment.metadataJson)
                stmt.executeUpdate()
            }
            enrollment
        }
    }

    override suspend fun findEnrollmentById(tenantId: String, enrollmentId: String): AffiliateEnrollment? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_enrollments WHERE tenant_id = ? AND enrollment_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, enrollmentId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateEnrollment(rs) else null
                }
            }
        }
    }

    override suspend fun findEnrollmentsByAffiliate(tenantId: String, affiliateId: String): List<AffiliateEnrollment> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_enrollments WHERE tenant_id = ? AND affiliate_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<AffiliateEnrollment>()
                    while (rs.next()) {
                        results.add(mapAffiliateEnrollment(rs))
                    }
                    results
                }
            }
        }
    }

    override suspend fun findEnrollmentsByProgram(
        tenantId: String,
        programId: String,
        status: AffiliateEnrollmentStatus?
    ): List<AffiliateEnrollment> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sb = StringBuilder("SELECT * FROM affiliate_enrollments WHERE tenant_id = ? AND program_id = ?")
            val params = mutableListOf<Any>(tenantId, programId)

            if (status != null) {
                sb.append(" AND enrollment_status = ?")
                params.add(status.name)
            }
            sb.append(" ORDER BY created_at DESC")

            conn.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<AffiliateEnrollment>()
                    while (rs.next()) {
                        results.add(mapAffiliateEnrollment(rs))
                    }
                    results
                }
            }
        }
    }

    override suspend fun listEnrollments(tenantId: String, status: AffiliateEnrollmentStatus?): List<AffiliateEnrollment> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sb = StringBuilder("SELECT * FROM affiliate_enrollments WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)

            if (status != null) {
                sb.append(" AND enrollment_status = ?")
                params.add(status.name)
            }
            sb.append(" ORDER BY created_at DESC")

            conn.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<AffiliateEnrollment>()
                    while (rs.next()) {
                        results.add(mapAffiliateEnrollment(rs))
                    }
                    results
                }
            }
        }
    }

    override suspend fun appendAuditRecord(record: AffiliateProgramAuditRecord): AffiliateProgramAuditRecord {
        return transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_program_audit_records (
                    audit_id, tenant_id, entity_type, entity_id, event_type, previous_status,
                    new_status, actor_type, actor_id, actor_role, reason, correlation_id,
                    record_hash, previous_audit_hash, chain_hash, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, audit_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, record.auditId)
                stmt.setString(2, record.tenantId)
                stmt.setString(3, record.entityType.name)
                stmt.setString(4, record.entityId)
                stmt.setString(5, record.eventType.name)
                stmt.setString(6, record.previousStatus)
                stmt.setString(7, record.newStatus)
                stmt.setString(8, record.actorType.name)
                stmt.setString(9, record.actorId)
                stmt.setString(10, record.actorRole)
                stmt.setString(11, record.reason)
                stmt.setString(12, record.correlationId)
                stmt.setString(13, record.recordHash)
                stmt.setString(14, record.previousAuditHash)
                stmt.setString(15, record.chainHash)
                stmt.setLong(16, record.timestamp)
                stmt.executeUpdate()
            }
            record
        }
    }

    override suspend fun getLatestAuditRecord(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String
    ): AffiliateProgramAuditRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_program_audit_records WHERE tenant_id = ? AND entity_type = ? AND entity_id = ? ORDER BY timestamp DESC LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, entityType.name)
                stmt.setString(3, entityId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateProgramAuditRecord(rs) else null
                }
            }
        }
    }

    override suspend fun listAuditRecords(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String
    ): List<AffiliateProgramAuditRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_program_audit_records WHERE tenant_id = ? AND entity_type = ? AND entity_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, entityType.name)
                stmt.setString(3, entityId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateProgramAuditRecord>()
                    while (rs.next()) {
                        list.add(mapAffiliateProgramAuditRecord(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveOutboxEvent(event: AffiliateProgramOutboxEvent): AffiliateProgramOutboxEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_program_outbox_events (
                    outbox_id, tenant_id, aggregate_type, aggregate_id, event_type,
                    payload_json, status, correlation_id, version, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, outbox_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.outboxId)
                stmt.setString(2, event.tenantId)
                stmt.setString(3, event.aggregateType)
                stmt.setString(4, event.aggregateId)
                stmt.setString(5, event.eventType)
                stmt.setString(6, event.payloadJson)
                stmt.setString(7, event.status)
                stmt.setString(8, event.correlationId)
                stmt.setLong(9, event.version)
                stmt.setLong(10, event.createdAt)
                stmt.executeUpdate()
            }
            event
        }
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProgramGovernanceSummary {
        val allPrograms = listPrograms(tenantId)
        val allEnrollments = listEnrollments(tenantId)

        return AffiliateProgramGovernanceSummary(
            tenantId = tenantId,
            totalPrograms = allPrograms.size.toLong(),
            activePrograms = allPrograms.count { it.status == AffiliateProgramStatus.ACTIVE }.toLong(),
            pausedPrograms = allPrograms.count { it.status == AffiliateProgramStatus.PAUSED }.toLong(),
            closedPrograms = allPrograms.count { it.status == AffiliateProgramStatus.CLOSED }.toLong(),
            archivedPrograms = allPrograms.count { it.status == AffiliateProgramStatus.ARCHIVED }.toLong(),
            totalEnrollments = allEnrollments.size.toLong(),
            activeEnrollments = allEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.ACTIVE }.toLong(),
            pendingEnrollments = allEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.PENDING }.toLong(),
            suspendedEnrollments = allEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.SUSPENDED }.toLong(),
            terminatedEnrollments = allEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.TERMINATED }.toLong(),
            rejectedEnrollments = allEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.REJECTED }.toLong()
        )
    }

    private fun mapAffiliateProgram(rs: ResultSet): AffiliateProgram {
        return AffiliateProgram(
            programId = rs.getString("program_id"),
            tenantId = rs.getString("tenant_id"),
            programCode = rs.getString("program_code"),
            programName = rs.getString("program_name"),
            description = rs.getString("description"),
            status = AffiliateProgramStatus.valueOf(rs.getString("status")),
            startDate = rs.getLong("start_date"),
            endDate = rs.getObject("end_date") as? Long,
            eligibilityPolicy = rs.getString("eligibility_policy"),
            termsReference = rs.getString("terms_reference"),
            termsVersion = rs.getString("terms_version"),
            maxParticipants = rs.getObject("max_participants") as? Int,
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    private fun mapAffiliateEnrollment(rs: ResultSet): AffiliateEnrollment {
        return AffiliateEnrollment(
            enrollmentId = rs.getString("enrollment_id"),
            tenantId = rs.getString("tenant_id"),
            affiliateId = rs.getString("affiliate_id"),
            programId = rs.getString("program_id"),
            enrollmentStatus = AffiliateEnrollmentStatus.valueOf(rs.getString("enrollment_status")),
            effectiveFrom = rs.getObject("effective_from") as? Long,
            effectiveTo = rs.getObject("effective_to") as? Long,
            enrollmentReason = rs.getString("enrollment_reason"),
            requestedAt = rs.getLong("requested_at"),
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getObject("approved_at") as? Long,
            rejectedBy = rs.getString("rejected_by"),
            rejectedAt = rs.getObject("rejected_at") as? Long,
            rejectionReason = rs.getString("rejection_reason"),
            suspendedBy = rs.getString("suspended_by"),
            suspendedAt = rs.getObject("suspended_at") as? Long,
            suspensionReason = rs.getString("suspension_reason"),
            terminatedBy = rs.getString("terminated_by"),
            terminatedAt = rs.getObject("terminated_at") as? Long,
            terminationReason = rs.getString("termination_reason"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    private fun mapAffiliateProgramAuditRecord(rs: ResultSet): AffiliateProgramAuditRecord {
        return AffiliateProgramAuditRecord(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            entityType = AffiliateProgramEntityCategory.valueOf(rs.getString("entity_type")),
            entityId = rs.getString("entity_id"),
            eventType = AffiliateProgramAuditEventType.valueOf(rs.getString("event_type")),
            previousStatus = rs.getString("previous_status"),
            newStatus = rs.getString("new_status"),
            actorType = AffiliateActorType.valueOf(rs.getString("actor_type")),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            reason = rs.getString("reason"),
            correlationId = rs.getString("correlation_id"),
            recordHash = rs.getString("record_hash"),
            previousAuditHash = rs.getString("previous_audit_hash"),
            chainHash = rs.getString("chain_hash"),
            timestamp = rs.getLong("timestamp")
        )
    }
}
