package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramEntityCategory
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramGovernanceSummary
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramOutboxEvent
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe fake data source for Affiliate Programs and Enrollments (Unit Testing).
 */
class FakeAffiliateProgramDataSource : AffiliateProgramDataSource {

    private val programs = ConcurrentHashMap<String, AffiliateProgram>() // key: "$tenantId|$programId"
    private val enrollments = ConcurrentHashMap<String, AffiliateEnrollment>() // key: "$tenantId|$enrollmentId"
    private val auditRecords = mutableListOf<AffiliateProgramAuditRecord>()
    private val outboxEvents = mutableListOf<AffiliateProgramOutboxEvent>()
    private val lock = Any()

    private fun programKey(tenantId: String, programId: String) = "$tenantId|$programId"
    private fun enrollmentKey(tenantId: String, enrollmentId: String) = "$tenantId|$enrollmentId"

    override suspend fun saveProgram(program: AffiliateProgram): AffiliateProgram {
        programs[programKey(program.tenantId, program.programId)] = program
        return program
    }

    override suspend fun findProgramById(tenantId: String, programId: String): AffiliateProgram? {
        return programs[programKey(tenantId, programId)]
    }

    override suspend fun findProgramByCode(tenantId: String, programCode: String): AffiliateProgram? {
        return programs.values.firstOrNull {
            it.tenantId == tenantId && it.programCode.equals(programCode, ignoreCase = true)
        }
    }

    override suspend fun listPrograms(tenantId: String, status: AffiliateProgramStatus?): List<AffiliateProgram> {
        return programs.values
            .filter { it.tenantId == tenantId && (status == null || it.status == status) }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun saveEnrollment(enrollment: AffiliateEnrollment): AffiliateEnrollment {
        enrollments[enrollmentKey(enrollment.tenantId, enrollment.enrollmentId)] = enrollment
        return enrollment
    }

    override suspend fun findEnrollmentById(tenantId: String, enrollmentId: String): AffiliateEnrollment? {
        return enrollments[enrollmentKey(tenantId, enrollmentId)]
    }

    override suspend fun findEnrollmentsByAffiliate(tenantId: String, affiliateId: String): List<AffiliateEnrollment> {
        return enrollments.values
            .filter { it.tenantId == tenantId && it.affiliateId == affiliateId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun findEnrollmentsByProgram(
        tenantId: String,
        programId: String,
        status: AffiliateEnrollmentStatus?
    ): List<AffiliateEnrollment> {
        return enrollments.values
            .filter {
                it.tenantId == tenantId &&
                it.programId == programId &&
                (status == null || it.enrollmentStatus == status)
            }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun listEnrollments(tenantId: String, status: AffiliateEnrollmentStatus?): List<AffiliateEnrollment> {
        return enrollments.values
            .filter { it.tenantId == tenantId && (status == null || it.enrollmentStatus == status) }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun appendAuditRecord(record: AffiliateProgramAuditRecord): AffiliateProgramAuditRecord {
        synchronized(lock) {
            auditRecords.add(record)
        }
        return record
    }

    override suspend fun getLatestAuditRecord(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String
    ): AffiliateProgramAuditRecord? {
        synchronized(lock) {
            return auditRecords
                .filter { it.tenantId == tenantId && it.entityType == entityType && it.entityId == entityId }
                .maxByOrNull { it.timestamp }
        }
    }

    override suspend fun listAuditRecords(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String
    ): List<AffiliateProgramAuditRecord> {
        synchronized(lock) {
            return auditRecords
                .filter { it.tenantId == tenantId && it.entityType == entityType && it.entityId == entityId }
                .sortedBy { it.timestamp }
        }
    }

    override suspend fun saveOutboxEvent(event: AffiliateProgramOutboxEvent): AffiliateProgramOutboxEvent {
        synchronized(lock) {
            outboxEvents.add(event)
        }
        return event
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProgramGovernanceSummary {
        val tenantPrograms = programs.values.filter { it.tenantId == tenantId }
        val tenantEnrollments = enrollments.values.filter { it.tenantId == tenantId }

        return AffiliateProgramGovernanceSummary(
            tenantId = tenantId,
            totalPrograms = tenantPrograms.size.toLong(),
            activePrograms = tenantPrograms.count { it.status == AffiliateProgramStatus.ACTIVE }.toLong(),
            pausedPrograms = tenantPrograms.count { it.status == AffiliateProgramStatus.PAUSED }.toLong(),
            closedPrograms = tenantPrograms.count { it.status == AffiliateProgramStatus.CLOSED }.toLong(),
            archivedPrograms = tenantPrograms.count { it.status == AffiliateProgramStatus.ARCHIVED }.toLong(),
            totalEnrollments = tenantEnrollments.size.toLong(),
            activeEnrollments = tenantEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.ACTIVE }.toLong(),
            pendingEnrollments = tenantEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.PENDING }.toLong(),
            suspendedEnrollments = tenantEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.SUSPENDED }.toLong(),
            terminatedEnrollments = tenantEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.TERMINATED }.toLong(),
            rejectedEnrollments = tenantEnrollments.count { it.enrollmentStatus == AffiliateEnrollmentStatus.REJECTED }.toLong()
        )
    }

    fun listPendingOutboxEvents(tenantId: String): List<AffiliateProgramOutboxEvent> {
        synchronized(lock) {
            return outboxEvents.filter { it.tenantId == tenantId && it.status == "PENDING" }
        }
    }
}
